package org.poc.andreyz.djl.mlp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingMessage {
    //customer income in rubles per month
    private float income;
    //service price in rubles per month
    private float price;
    //service usage in minutes per month
    private float usage;
    //customer loyalty to company in years
    private float loyalty;
    //customer age
    private float age;
    //customer sex (man/woman)
    private String sex;
    //customer marital status (married/single)
    private String family;
    //customer region (north/west/south/east)
    private String region;
    //whether customer pays timely for his service or not (1/0)
    private int timelyPayLabel;
}
