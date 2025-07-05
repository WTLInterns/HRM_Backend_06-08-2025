package com.jaywant.demo.Repo;

import com.jaywant.demo.Entity.Opening;
import com.jaywant.demo.Entity.Subadmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OpeningRepo extends JpaRepository<Opening, Integer> {
    @Query("SELECT o FROM Opening o WHERE o.subadmin.id = :subadminId")
    List<Opening> findBySubadminId(@Param("subadminId") int subadminId);
}
