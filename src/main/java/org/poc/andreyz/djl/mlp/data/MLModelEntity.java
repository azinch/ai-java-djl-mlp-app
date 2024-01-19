package org.poc.andreyz.djl.mlp.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mlp_demo_model")
public class MLModelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "model_id")
    private Integer modelId;

    @Column(name = "model_file_name")
    private String modelFileName;

    @Lob
    @Column(name = "file_content")
    private byte [] fileContent;

    @Column(name = "min_feature_values")
    private String minFtrVals;

    @Column(name = "max_feature_values")
    private String maxFtrVals;
}
