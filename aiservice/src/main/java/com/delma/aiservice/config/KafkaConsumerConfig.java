package com.delma.aiservice.config;

import com.delma.aiservice.kafka.ConsultationNotesEvent;
import com.delma.aiservice.kafka.DocumentUploadedEvent;
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
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // WHY separate baseProps method?
    // Both documentConsumerFactory and consultationConsumerFactory
    // need the same base config (bootstrap servers).
    // Extract to avoid duplication — DRY principle.
    // groupId and offsetReset differ per consumer so they're parameters.
    private Map<String, Object> baseProps(String groupId, String offsetReset) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
        return props;
    }

    // WHY ProducerFactory here too?
    // Same reason as notificationservice:
    // DeadLetterPublishingRecoverer needs to write to:
    //   document-uploaded.DLT
    //   consultation-notes-ready.DLT
    // Writing = producing = needs ProducerFactory + KafkaTemplate
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

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // WHY ONE shared errorHandler for both consumers?
    // Both consumers (document + consultation) need the same retry logic:
    // 3 retries, exponential backoff, then DLT.
    // No reason to create two identical beans.
    // One errorHandler bean — both factories use it via
    // factory.setCommonErrorHandler(errorHandler(kafkaTemplate()))
    @Bean
    public DefaultErrorHandler errorHandler(
            KafkaTemplate<String, Object> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate);

        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000L);
        backOff.setMaxElapsedTime(30000L);

        return new DefaultErrorHandler(recoverer, backOff);
    }

    // ── DocumentUploadedEvent ─────────────────────────────────────

    // WHY "earliest" offset reset for documents?
    // Documents need to be indexed in the RAG pipeline.
    // If aiservice was down for 2 hours, we NEED to index
    // all documents uploaded during that time.
    // "earliest" = process everything missed while down.
    @Bean
    public ConsumerFactory<String, DocumentUploadedEvent>
    documentConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseProps("aiservice-rag-group", "earliest"),
                new StringDeserializer(),
                new JacksonJsonDeserializer<>(DocumentUploadedEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DocumentUploadedEvent>
    documentKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory
                <String, DocumentUploadedEvent>();
        factory.setConsumerFactory(documentConsumerFactory());
        // Wire in our DefaultErrorHandler
        // document-uploaded failures → retry → document-uploaded.DLT
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate()));
        return factory;
    }

    // ── ConsultationNotesEvent ────────────────────────────────────

    // WHY "latest" offset reset for consultation notes?
    // Consultation AI reports use Groq API (costs money per call).
    // If aiservice restarts, we DON'T want to reprocess old consultation
    // notes — that would double-charge Groq API and create duplicate reports.
    // "latest" = only new messages after restart.
    //
    // This is different from documents (earliest) because:
    // Documents: idempotent (re-indexing same doc = same result, no cost)
    // Consultation: NOT idempotent (re-processing = duplicate Groq call = cost)
    @Bean
    public ConsumerFactory<String, ConsultationNotesEvent>
    consultationConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseProps("aiservice-consultation-group", "latest"),
                new StringDeserializer(),
                new JacksonJsonDeserializer<>(ConsultationNotesEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ConsultationNotesEvent>
    consultationKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory
                <String, ConsultationNotesEvent>();
        factory.setConsumerFactory(consultationConsumerFactory());
        // consultation-notes-ready failures → retry → consultation-notes-ready.DLT
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate()));
        return factory;
    }
}