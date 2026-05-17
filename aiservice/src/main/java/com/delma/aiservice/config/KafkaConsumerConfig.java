package com.delma.aiservice.config;

import com.delma.aiservice.kafka.ConsultationNotesEvent;
import com.delma.aiservice.kafka.DocumentUploadedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Shared base config ────────────────────────────────────────────────
    // offsetReset:
    //   "earliest" → replay all messages from beginning (RAG indexing)
    //   "latest"   → only new messages (consultation notes — avoid duplicates)
    private Map<String, Object> baseProps(String groupId, String offsetReset) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }

    // ── DocumentUploadedEvent factory ─────────────────────────────────────
    // Uses "earliest" — if aiservice restarts we want to re-index
    // any documents that were uploaded while it was down
    @Bean
    public ConsumerFactory<String, DocumentUploadedEvent>
            documentConsumerFactory() {
        Map<String, Object> props = baseProps("aiservice-rag-group", "earliest");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                DocumentUploadedEvent.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DocumentUploadedEvent>
            documentKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory
                <String, DocumentUploadedEvent>();
        factory.setConsumerFactory(documentConsumerFactory());
        return factory;
    }

    // ── ConsultationNotesEvent factory ────────────────────────────────────
    // Uses "latest" — prevents reprocessing old events on restart
    // AI report is already saved in DB — reprocessing = duplicate Groq calls
    @Bean
    public ConsumerFactory<String, ConsultationNotesEvent>
            consultationConsumerFactory() {
        Map<String, Object> props = baseProps(
                "aiservice-consultation-group", "latest");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                ConsultationNotesEvent.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ConsultationNotesEvent>
            consultationKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory
                <String, ConsultationNotesEvent>();
        factory.setConsumerFactory(consultationConsumerFactory());
        return factory;
    }
}
