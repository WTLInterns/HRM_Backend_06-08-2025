package com.jaywant.demo.Repo;

import com.jaywant.demo.Entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResumeRepo extends JpaRepository<Resume, Integer> {
    List<Resume> findByEmployeeEmpId(int empId);
}
