package org.poc.andreyz.djl.mlp.service;

import ai.djl.Model;
import ai.djl.basicmodelzoo.basic.Mlp;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDList;
import ai.djl.translate.NoopTranslator;
import ai.djl.translate.Translator;
import lombok.extern.slf4j.Slf4j;
import org.poc.andreyz.djl.mlp.data.MLModelEntity;
import org.poc.andreyz.djl.mlp.data.MLModelRepository;
import org.poc.andreyz.djl.mlp.model.CustomPredictor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;


@Service
@Slf4j
public class MLPredictorService
{
    @Value("${modelName: mlp}")
    private String modelName;
    @Value("${predictModelDir: predict-model}")
    private String predictModelDir;
    @Value(("${featureCount: 8}"))
    private String featureCount;

    @Autowired
    MLModelRepository repository;

    private int ftrCnt;
    private ArrayList<Float> minFtrVals;
    private ArrayList<Float> maxFtrVals;

    private Predictor<NDList, NDList> predictor;


    @PostConstruct
    private void init() {
        log.info("MLPredictorService.init");
        ftrCnt = Integer.parseInt(featureCount);
        log.debug("MLTrainerService.init, ftrCnt: {}", ftrCnt);
    }

    public CustomPredictor createPredictor()
    {
        log.debug("createPredictor started");
        predictor = null;
        CustomPredictor customPredictor = null;

        try {
            downloadModelFromDb();
            Model model = Model.newInstance(modelName);
            model.setBlock(new Mlp(ftrCnt, 2, new int[]{12, 12, 12}));
            model.load(Paths.get(predictModelDir));
            Translator translator = new NoopTranslator();
            predictor = model.newPredictor(translator);
            customPredictor = new CustomPredictor(predictor, minFtrVals, maxFtrVals);
        } catch (Exception exception) {
            log.error("createPredictor Got Exception: {}", exception.getMessage());
        }

        log.debug("createPredictor ended, customPredictor: {}", customPredictor);
        return customPredictor;
    }

    private void downloadModelFromDb() {
        log.debug("downloadModelFromDb started");
        MLModelEntity entity = repository.findLastModelNative();

        try (FileOutputStream fos =
                     new FileOutputStream(predictModelDir + "/" + entity.getModelFileName()))
        {
            fos.write(entity.getFileContent());
            log.debug("ModelFile downloaded: {}", entity.getModelFileName());

            minFtrVals = new ArrayList<>();
            maxFtrVals = new ArrayList<>();
            String [] minStrArr = entity.getMinFtrVals().split("@");
            String [] maxStrArr = entity.getMaxFtrVals().split("@");

            for(int idx = 0; idx < minStrArr.length; idx++) {
                minFtrVals.add(Float.valueOf(minStrArr[idx]));
                maxFtrVals.add(Float.valueOf(maxStrArr[idx]));
            }

            log.debug("minFtrVals (MLPredictorService):\n{}\n", minFtrVals);
            log.debug("maxFtrVals (MLPredictorService):\n{}\n", maxFtrVals);

        } catch (Exception exception) {
            log.error("downloadModelFromDb Got Exception: {}", exception.getMessage());
        }

        log.debug("downloadModelFromDb ended");
    }

}
