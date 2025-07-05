package com.jaywant.demo.Repo;

import com.jaywant.demo.Entity.SalaryConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalaryConfigurationRepository extends JpaRepository<SalaryConfiguration, Long> {
    
    Optional<SalaryConfiguration> findBySubadminId(Integer subadminId);
    
    boolean existsBySubadminId(Integer subadminId);
}
