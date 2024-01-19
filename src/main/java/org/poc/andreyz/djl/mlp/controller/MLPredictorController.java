package org.poc.andreyz.djl.mlp.controller;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import lombok.extern.slf4j.Slf4j;
import org.poc.andreyz.djl.mlp.model.CustomPredictor;
import org.poc.andreyz.djl.mlp.model.PredictionRequest;
import org.poc.andreyz.djl.mlp.service.MLPredictorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/mlp-demo")
@Slf4j
public class MLPredictorController
{
    @Value(("${featureCount: 8}"))
    private String featureCount;
    @Autowired
    MLPredictorService predictorService;

    String [] sexArr;
    String [] familyArr;
    String [] regionArr;
    Map<String, Float> regionMap;


    @PostConstruct
    private void init() {
        log.info("MLPredictorController.init");
        sexArr = new String[] {"man", "woman"};
        familyArr = new String[] {"married", "single"};
        regionArr = new String[] {"north", "west", "south", "east"};
        regionMap = new HashMap<>();
        regionMap.put("north", 0.7f);
        regionMap.put("west", 0.6f);
        regionMap.put("south", 0.4f);
        regionMap.put("east", 0.3f);
    }

    @PostMapping(value = "/predict")
    public String getPrediction(@RequestBody PredictionRequest request)
    {
        log.info("getPrediction Request: {}", request);
        String response = null;

        try
        {
            CustomPredictor customPredictor = predictorService.createPredictor();

            float [] ftrArr = new float [Integer.parseInt(featureCount)];
            float ftr = 0f; float min = 0f; float max = 0f;

            for(int idx = 0; idx < ftrArr.length; idx++)
            {
                if(idx == 0) {
                    ftr = request.getIncome();
                } else if(idx == 1) {
                    ftr = request.getPrice();
                } else if(idx == 2) {
                    ftr = request.getUsage();
                } else if(idx == 3) {
                    ftr = request.getLoyalty();
                } else if(idx == 4) {
                    ftr = request.getAge();
                } else if(idx == 5) {
                    String ftrInput = request.getSex();
                    boolean contains = Arrays.stream(sexArr).anyMatch(ftrInput::equals);
                    if(!contains) {
                        throw new Exception(ftrInput + " is invalid, valid values: " + Arrays.toString(sexArr));
                    }
                    ftr = 0.4f;
                    if(ftrInput.equals("woman")) {
                        ftr = 0.6f;
                    }
                } else if(idx == 6) {
                    String ftrInput = request.getFamily();
                    boolean contains = Arrays.stream(familyArr).anyMatch(ftrInput::equals);
                    if(!contains) {
                        throw new Exception(ftrInput + " is invalid, valid values: " + Arrays.toString(familyArr));
                    }
                    ftr = 0f;
                    if(ftrInput.equals("single")) {
                        ftr = 1f;
                    }
                } else if(idx == 7) {
                    String ftrInput = request.getRegion();
                    boolean contains = Arrays.stream(regionArr).anyMatch(ftrInput::equals);
                    if(!contains) {
                        throw new Exception(ftrInput + " is invalid, valid values: " + Arrays.toString(regionArr));
                    }
                    ftr = regionMap.get(ftrInput);
                }

                min = customPredictor.getMinFtrVals().get(idx);
                max = customPredictor.getMaxFtrVals().get(idx);
                min = min < ftr ? min : ftr;
                max = max > ftr ? max : ftr;

                ftrArr[idx] = (ftr - min)/(max - min);
            }

            log.debug("\nrequest: {}", request);
            log.debug("minFtrVals: {} , maxFtrVals: {}",
                    customPredictor.getMinFtrVals(), customPredictor.getMaxFtrVals());
            log.debug("normArray: {}\n", ftrArr);

            NDManager manager = NDManager.newBaseManager();
            NDArray data = manager.create(ftrArr);
            log.debug("data = shape: {} , type: {} , data: {}\n",
                    data.getShape(), data.getDataType(), data.toFloatArray());

            NDArray result =
                    ((NDList) customPredictor.getPredictor().predict(new NDList(data)))
                            .singletonOrThrow()
                            .softmax(1);
            log.debug("result = getDataType: {} , toDebugString: {} , toString: {}\n",
                    result.getDataType(), result.toDebugString(), result.toString());

            response = result.toDebugString();

        } catch (Exception exception) {
            log.error("getPrediction Exception: {}", exception.getMessage());
        }

        log.info("getPrediction Response: {}", response);
        return response;
    }

}
