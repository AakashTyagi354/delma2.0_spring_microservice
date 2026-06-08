package com.delma.notificationservice.config;


import com.delma.notificationservice.kafka.NotificationEvent;
import com.fasterxml.jackson.databind.JsonSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        // AUTO_OFFSET_RESET — what to do when this consumer starts
        // for the very first time (no committed offset exists)?
        //
        // "earliest" = start from the OLDEST unread message
        //              meaning: if notificationservice was down for 1 hour,
        //              it will process all notifications from that hour
        //              when it comes back up
        //
        // "latest"  = start from NEW messages only
        //              meaning: ignore anything that happened while down
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // DefaultKafkaConsumerFactory constructor takes 3 arguments:
        // 1. props map — connection + behavior configuration
        // 2. key deserializer — how to convert key bytes → String
        // 3. value deserializer — how to convert value bytes → NotificationEvent
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }
    //     notificationservice consumes from notification-topic
    //     but also produces to notification-topic.DLT
    //     It's primarily a consumer, but needs producer capability
    //     just for the DLT use case.
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),           // key serializer
                new JacksonJsonSerializer<>()     // value serializer — Spring Kafka 4.0
        );
    }
    // KafkaTemplate is Spring's high-level API for sending messages
    // It wraps the raw Kafka producer with:
    //   - Exception translation (Kafka exceptions → Spring exceptions)
    //   - Transaction support
    //   - Metrics integration
    //
    // DeadLetterPublishingRecoverer requires a KafkaTemplate
    // to publish failed messages to the DLT topic
    // Without this bean, the recoverer has no way to write to Kafka
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        // KafkaTemplate wraps ProducerFactory
        // Every send() call uses the producer config we defined above
        return new KafkaTemplate<>(producerFactory());
    }
    // exception in consumer → Kafka retries forever (poison pill)
    // exception → retry N times with backoff → DLT → move on
    // It replaces the old try-catch-swallow pattern with a proper strategy
    @Bean
    public DefaultErrorHandler errorHandler(
            KafkaTemplate<String, Object> kafkaTemplate) {

        // This is what actually MOVES failed messages to the DLT topic
        // When all retries are exhausted, DefaultErrorHandler calls
        // the recoverer's recover() method
        // DeadLetterPublishingRecoverer then:
        // 1. Takes the original ConsumerRecord (the failed message)
        // 2. Adds headers with failure metadata:
        //    - kafka_dlt-exception-fqcn → "java.lang.NullPointerException"
        //    - kafka_dlt-exception-message → "userId must not be null"
        //    - kafka_dlt-original-topic → "notification-topic"
        //    - kafka_dlt-original-partition → 0
        //    - kafka_dlt-original-offset → 42
        // 3. Publishes to "notification-topic.DLT" using kafkaTemplate
        // 4. Returns normally → DefaultErrorHandler commits original offset
        // Kafka creates topics on first write by default
        // DeadLetterPublishingRecoverer just appends ".DLT" to topic name
        // No manual topic creation needed
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate);

        // Controls HOW LONG to wait between retries
        ExponentialBackOff backOff = new ExponentialBackOff();
        // Wait 1 second before first retry
        backOff.setInitialInterval(1000L);
        // Double the wait each time: 1s → 2s → 4s → 8s...
        backOff.setMultiplier(2.0);
        // Never wait more than 10 seconds between retries
        backOff.setMaxInterval(10000L);
        // Give up entirely after 30 seconds total elapsed time
        backOff.setMaxElapsedTime(30000L);
        // Combine recoverer + backoff into DefaultErrorHandler
        // DefaultErrorHandler is the glue:
        // 1. Catches exceptions from @KafkaListener methods
        // 2. Waits according to backOff schedule
        // 3. Retries the message
        // 4. After maxElapsedTime → calls recoverer.recover()
        //    → recoverer publishes to DLT
        //    → returns normally
        // 5. DefaultErrorHandler commits the original offset
        //    → partition is unblocked
        //    → next message processed
        return new DefaultErrorHandler(recoverer, backOff);
    }
    // @KafkaListener annotated methods don't run themselves
    // Spring creates a "listener container" for each @KafkaListener
    // The container:
    //   1. Polls Kafka for new messages in a background thread
    //   2. Deserializes the message using ConsumerFactory
    //   3. Calls your @KafkaListener method with the deserialized object
    //   4. If exception: delegates to CommonErrorHandler
    //   5. Commits offset on success

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Without this: DefaultErrorHandler bean exists but is NEVER used
        // Spring would use its own default error handler (infinite retry)
        //
        // With this: every exception in @KafkaListener goes through our handler:
        // exception → wait 1s → retry → wait 2s → retry → wait 4s → DLT
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate()));
        return factory;
    }


}
