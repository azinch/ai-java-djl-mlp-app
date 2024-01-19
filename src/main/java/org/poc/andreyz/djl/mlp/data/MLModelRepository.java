package org.poc.andreyz.djl.mlp.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface MLModelRepository extends JpaRepository<MLModelEntity, Integer> {
    @Query(value = "select t.* from mlp_demo_model t where t.model_id = "
                   + "(select max(model_id) from mlp_demo_model)", nativeQuery = true)
    MLModelEntity findLastModelNative();
}


