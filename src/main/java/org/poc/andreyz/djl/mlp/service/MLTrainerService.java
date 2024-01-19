package org.poc.andreyz.djl.mlp.service;

import ai.djl.Model;
import ai.djl.basicmodelzoo.basic.Mlp;
import ai.djl.engine.Engine;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingResult;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.listener.SaveModelTrainingListener;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.util.ProgressBar;
import lombok.extern.slf4j.Slf4j;
import org.poc.andreyz.djl.mlp.data.MLModelEntity;
import org.poc.andreyz.djl.mlp.data.MLModelRepository;
import org.poc.andreyz.djl.mlp.model.TrainingMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class MLTrainerService
{
    @Value("${spring.kafka.topics: mlp-demo}")
    private String topic;
    @Value(("${spring.kafka.consumer.max-poll-records: 100}"))
    private String kafkaBatchSize;
    @Value(("${featureCount: 8}"))
    private String featureCount;
    @Value(("${trainBatchSize: 10}"))
    private String trainBatchSize;
    @Value(("${trainNumEpoch: 3}"))
    private String trainNumEpoch;
    @Value(("${timeDelaySec: 10}"))
    private String timeDelaySec;
    @Value("${modelName: mlp}")
    private String modelName;
    @Value("${modelDir: model}")
    private String modelDir;

    @Autowired
    KafkaTemplate<String, TrainingMessage> kafkaTemplate;
    @Autowired
    ProducerFactory<String, TrainingMessage> producerFactory;
    @Autowired
    private ConcurrentKafkaListenerContainerFactory<String, TrainingMessage> kafkaListenerContainerFactory;
    @Autowired
    private KafkaListenerEndpointRegistry listenerController;
    @Autowired
    private MLModelRepository modelRepository;

    final private String LISTENER_ID = "my-listener";
    private ArrayList<TrainingMessage> messageList;
    private CountDownLatch threadLatch;

    private int kfkBatch;
    private int ftrCnt;

    float minIncome, maxIncome;
    float minPrice, maxPrice;
    float minUsage, maxUsage;
    float minLoyalty, maxLoyalty;
    float minAge, maxAge;
    String [] sexArr;
    String [] familyArr;
    String [] regionArr;
    Map<String, Float> regionMap;

    private float [] minFtrArr;
    private float [] maxFtrArr;


    @PostConstruct
    private void init() {
        log.info("MLTrainerService.init");

        kfkBatch = Integer.parseInt(kafkaBatchSize);
        ftrCnt = Integer.parseInt(featureCount);

        minIncome = 30000f; maxIncome = 100000f;
        minPrice = 400f; maxPrice = 2000f;
        minUsage = 600f; maxUsage = 3600f;
        minLoyalty = 1f; maxLoyalty = 10f;
        minAge = 20f; maxAge = 70f;
        sexArr = new String[] {"man", "woman"};
        familyArr = new String[] {"married", "single"};

        regionArr = new String[] {"north", "west", "south", "east"};
        regionMap = new HashMap<>();
        regionMap.put("north", 0.7f);
        regionMap.put("west", 0.6f);
        regionMap.put("south", 0.4f);
        regionMap.put("east", 0.3f);
        // min/max values of the features
        minFtrArr = new float[ftrCnt];
        maxFtrArr = new float[ftrCnt];

        log.debug("MLTrainerService.init, kfkBatch: {}, ftrCnt: {}", kfkBatch, ftrCnt);
    }

    @Async("customAsyncExecutor")
    public void runDataProducer() {
        log.info("runDataProducer started");
        Boolean runFlag = true;
        Random random = new Random();

        while(runFlag) {
            /** try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } **/

            float income = minIncome + random.nextFloat()*(maxIncome-minIncome);
            float incomeFactor = 10*(income-minIncome)/(maxIncome-minIncome);

            float price = minPrice + random.nextFloat()*(maxPrice-minPrice);
            float priceFactor = (maxPrice-price)/(maxPrice-minPrice);

            float usage = minUsage + random.nextFloat()*(maxUsage-minUsage);
            float usageFactor = 7*(maxUsage-usage)/(maxUsage-minUsage);

            float loyalty = minLoyalty + random.nextFloat()*(maxLoyalty-minLoyalty);
            float loyaltyFactor = 7*(loyalty-minLoyalty)/(maxLoyalty-minLoyalty);

            float age = minAge + random.nextFloat()*(maxAge-minAge);
            float ageFactor = 8*(age-minAge)/(maxAge-minAge);

            String sex = sexArr[random.nextInt(2)];
            float sexFactor = 0.4f;
            if(sex.equals("woman")) {
                sexFactor = 0.6f;
            }

            String family = familyArr[random.nextInt(2)];
            float familyFactor = 0f;
            if(family.equals("single")) {
                familyFactor = 2.5f;
            }

            String region = regionArr[random.nextInt(4)];
            float regionFactor = regionMap.get(region);

            float calc =
                    (incomeFactor + priceFactor + usageFactor + loyaltyFactor + ageFactor + sexFactor + familyFactor + regionFactor)
                            / (10 + 1 + 7 + 7 + 8 + 1 + 2.5f + 1);

            int label = 0;
            if(calc >= 0.5) {
                label = 1;
            }

            TrainingMessage message = TrainingMessage.builder()
                    .income(income)
                    .price(price)
                    .usage(usage)
                    .loyalty(loyalty)
                    .age(age)
                    .sex(sex)
                    .family(family)
                    .region(region)
                    .timelyPayLabel(label)
                    .build();

            kafkaTemplate.send(topic, message);
            //log.debug("runDataProducer message: {}", message);
        }
    }

    @Async("customAsyncExecutor")
    public void runTrainer()
    {
        log.info("runTrainer started and sleeping for {} sec", timeDelaySec);
        // Construct Neural network
        Block block = new Mlp(ftrCnt, 2, new int[] {12, 12, 12});
        try (Model model = Model.newInstance(modelName))
        {
            Thread.sleep(Integer.parseInt(timeDelaySec)*1000L);
            model.setBlock(block);
            // Setup training configuration
            DefaultTrainingConfig config = setupTrainingConfig();
            Trainer trainer = model.newTrainer(config);
            trainer.setMetrics(new Metrics());
            Shape inputShape = new Shape(1, ftrCnt);
            //Initialize trainer with proper input shape
            trainer.initialize(inputShape);

            boolean runFlag = true;
            while(runFlag)
            {
                try {
                    // Get training dataset
                    ArrayDataset trainingSet = getDataset();
                    EasyTrain.fit(trainer, Integer.parseInt(trainNumEpoch), trainingSet, null);
                    model.save(Paths.get(modelDir), modelName);
                    uploadModelIntoDb(model.getModelPath());
                } catch (Exception exception) {
                    log.error("runTrainer Got Exception in the loop: {}", exception.getMessage());
                }
            }

        } catch (Exception exception) {
            log.error("runTrainer Got Exception: {}", exception.getMessage());
        }
    }

    private ArrayDataset getDataset()
    {
        log.debug("getDataset started");

        ArrayDataset dataset = null;
        float [][] ftrArr = new float[kfkBatch][ftrCnt];
        int [] labelArr = new int[kfkBatch];

        try
        {
            threadLatch = new CountDownLatch(1);
            startListener();
            threadLatch.await(10, TimeUnit.SECONDS);
            stopListener();

            int row = 0;
            for (TrainingMessage message: messageList) {
                //log.debug("runTrainer.getDataset message: {}", message);
                float sex = 0.4f;
                if(message.getSex().equals("woman")) {
                    sex = 0.6f;
                }
                float family = 0f;
                if(message.getFamily().equals("single")) {
                    family = 1f;
                }
                float region = regionMap.get(message.getRegion());

                ftrArr[row] = new float [] {
                        message.getIncome(),
                        message.getPrice(),
                        message.getUsage(),
                        message.getLoyalty(),
                        message.getAge(),
                        sex,
                        family,
                        region
                };

                labelArr[row] = message.getTimelyPayLabel();
                row++;
            }

            NDManager manager = NDManager.newBaseManager();

            NDArray data = manager.create(ftrArr);
            //log.debug("data = shape: {}, type: {}\n{}\n", data.getShape(), data.getDataType(), data.toFloatArray());

            NDArray minData = data.min(new int[]{0});
            NDArray maxData = data.max(new int[]{0});
            minFtrArr = minData.toFloatArray();
            maxFtrArr = maxData.toFloatArray();
            /** log.debug("minData = shape: {}, type: {}\n{}\nmaxData = shape: {}, type: {}\n{}\n",
                    minData.getShape(), minData.getDataType(), minData.toFloatArray(),
                    maxData.getShape(), maxData.getDataType(), maxData.toFloatArray()); **/

            NDArray normData = data.sub(minData).div(maxData.sub(minData));
            NDArray labels = manager.create(labelArr);
            /** log.debug("normData = shape: {}, type: {}\n{}\nlabels = shape: {}, type: {}\n{}\n",
                    normData.getShape(), normData.getDataType(), normData.toFloatArray(),
                    labels.getShape(), labels.getDataType(), labels.toIntArray()); **/

            dataset = new ArrayDataset.Builder()
                    .setData(normData)
                    .optLabels(labels)
                    .setSampling(Integer.parseInt(trainBatchSize), false)
                    .build();
            dataset.prepare(new ProgressBar());

        } catch (Exception exception) {
            log.error("getDataset Got Exception: {}", exception.getMessage());
        }

        log.debug("getDataset ended");
        return dataset;
    }

    private DefaultTrainingConfig setupTrainingConfig() {
        log.debug("setupTrainingConfig started");
        SaveModelTrainingListener listener = new SaveModelTrainingListener(modelDir);
        listener.setSaveModelCallback(
                trainer -> {
                    TrainingResult result = trainer.getTrainingResult();
                    Model model = trainer.getModel();
                    float accuracy = result.getValidateEvaluation("Accuracy");
                    model.setProperty("Accuracy", String.format("%.5f", accuracy));
                    model.setProperty("Loss", String.format("%.5f", result.getValidateLoss()));
                });

        DefaultTrainingConfig trainingConfig
                = new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
                .addEvaluator(new Accuracy())
                .optDevices(Engine.getInstance().getDevices(1))
                .addTrainingListeners(TrainingListener.Defaults.logging(modelDir))
                .addTrainingListeners(listener);

        log.debug("setupTrainingConfig ended, trainingConfig: {}", trainingConfig);
        return trainingConfig;
    }

    @KafkaListener(id = LISTENER_ID, topics = "${spring.kafka.topics}", containerFactory = "kafkaListenerContainerFactory",
                   autoStartup = "false")
    public void kafkaListener(List<TrainingMessage> list) {
        messageList = (ArrayList<TrainingMessage>) list;
        log.debug("kafkaListener received: {} messages", messageList.size());
        threadLatch.countDown();
    }

    private void startListener() {
        listenerController.getListenerContainer(LISTENER_ID).start();
    }

    private void stopListener() {
        listenerController.getListenerContainer(LISTENER_ID).stop(()->{
            log.info("Kafka Listener Stopped.");
        });
    }

    private File getLastModifiedFile(String fileDirectory)
    {
        log.debug("getLastModifiedFile started");
        File directory = new File(fileDirectory);
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;

        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastModifiedTime) {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }

        log.debug("getLastModifiedFile ended, chosenFile: {}", chosenFile.getName());
        return chosenFile;
    }

    private void uploadModelIntoDb(Path modelPath)
    {
        log.debug("uploadModelIntoDb started");
        try {
            File modelFile = getLastModifiedFile(modelPath.toString());
            byte[] fileBytes = new byte[(int) modelFile.length()];
            FileInputStream fileInputStream = new FileInputStream(modelFile);
            fileInputStream.read(fileBytes);
            MLModelEntity entity = new MLModelEntity();
            entity.setModelFileName(modelFile.getName());
            entity.setFileContent(fileBytes);

            String minFtrStr = "";
            String maxFtrStr = "";
            for(int idx = 0; idx < minFtrArr.length; idx++) {
                if(idx > 0) {
                    minFtrStr += "@" + minFtrArr[idx];
                    maxFtrStr += "@" + maxFtrArr[idx];
                } else {
                    minFtrStr = String.valueOf(minFtrArr[idx]);
                    maxFtrStr = String.valueOf(maxFtrArr[idx]);
                }
            }
            log.debug("minStr: {}, maxStr: {}", minFtrStr, maxFtrStr);
            entity.setMinFtrVals(minFtrStr);
            entity.setMaxFtrVals(maxFtrStr);
            modelRepository.save(entity);

        } catch (Exception exception) {
            log.error("uploadModelIntoDb Got Exception: {}", exception.getMessage());
        }
        log.debug("uploadModelIntoDb ended");
    }

}
