package org.poc.andreyz.djl.mlp.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.poc.andreyz.djl.mlp.model.TrainingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@Slf4j
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers: 172.19.254.126:9092}")
    private String kafkaBrokerUrls;

    @Value(("${spring.kafka.consumer.group-id: andreyz}"))
    private String groupId;

    @Value(("${spring.kafka.consumer.max-poll-records: 1000}"))
    private String batchSize;

    public ConsumerFactory<String, TrainingMessage> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerUrls);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(TrainingMessage.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TrainingMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TrainingMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        //factory.setConcurrency(1);
        return factory;
    }

}
