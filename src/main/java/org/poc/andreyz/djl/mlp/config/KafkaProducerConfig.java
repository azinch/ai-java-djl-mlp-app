package org.poc.andreyz.djl.mlp.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.poc.andreyz.djl.mlp.model.TrainingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;


@Configuration
@Slf4j
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers: 172.19.254.126:9092}")
    private String kafkaBrokerUrls;
    @Value("${spring.kafka.producer.acks: all}")
    private String acks;
    @Value("${spring.kafka.producer.batch-size: 16384}")
    private String producerBatchSize;
    @Value("${producerLingerMs: 10}")
    private String producerLingerMs;


    @Bean
    public ProducerFactory<String, TrainingMessage> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerUrls);
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, producerLingerMs);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, producerBatchSize);
        log.info("KafkaConfig.producerFactory created factory with ProducerConfig: {}", configProps);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, TrainingMessage> kafkaTemplate() {
        KafkaTemplate<String, TrainingMessage> kafkaTemplate = new KafkaTemplate<>(producerFactory());
        log.info("KafkaConfig.kafkaTemplate created template:  {}", kafkaTemplate);
        return kafkaTemplate;
    }

}
