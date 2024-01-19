package org.poc.andreyz.djl.mlp.config;

import lombok.extern.slf4j.Slf4j;
import org.poc.andreyz.djl.mlp.model.TrainingMessage;
import org.poc.andreyz.djl.mlp.service.MLTrainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;


@Component
@EnableAsync
@Slf4j
public class MLTrainerConfig {
    @Value("${runTrainer: Yes}")
    private String runTrainer;

    @Autowired
    KafkaTemplate<String, TrainingMessage> kafkaTemplate;
    @Autowired
    ProducerFactory<String, TrainingMessage> producerFactory;
    @Autowired
    private ConcurrentKafkaListenerContainerFactory<String, TrainingMessage> kafkaListenerContainerFactory;
    @Autowired
    private KafkaListenerEndpointRegistry listenerController;
    @Autowired
    MLTrainerService trainerService;


    @Bean(name = "customAsyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;
    }

    @PostConstruct
    public void init() {
        if(runTrainer.equals("Yes")) {
            log.info("MLTrainerConfig.init TRAINING STARTED.");
            trainerService.runDataProducer();
            trainerService.runTrainer();
        } else {
            log.info("MLTrainerConfig.init TRAINING SKIPPED.");
        }
    }

}
