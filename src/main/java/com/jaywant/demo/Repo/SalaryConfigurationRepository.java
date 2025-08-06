package com.jaywant.demo.Repo;

import com.jaywant.demo.Entity.SalaryConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryConfigurationRepository extends JpaRepository<SalaryConfiguration, Long> {

    // Get all salary configurations for a subadmin (to handle multiple configs)
    List<SalaryConfiguration> findAllBySubadminId(Integer subadminId);

    // Get the first salary configuration for a subadmin (fallback method)
    Optional<SalaryConfiguration> findFirstBySubadminIdOrderByIdDesc(Integer subadminId);

    // Original method - kept for backward compatibility but may return multiple
    // results
    Optional<SalaryConfiguration> findBySubadminId(Integer subadminId);

    boolean existsBySubadminId(Integer subadminId);

    // Find salary config by employee empId
    Optional<SalaryConfiguration> findByEmployee_EmpId(Integer empId);
}
