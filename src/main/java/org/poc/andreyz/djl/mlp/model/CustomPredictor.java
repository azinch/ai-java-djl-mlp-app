package org.poc.andreyz.djl.mlp.model;

import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomPredictor {
    private Predictor<NDList, NDList> predictor;
    private ArrayList<Float> minFtrVals;
    private ArrayList<Float> maxFtrVals;
}
