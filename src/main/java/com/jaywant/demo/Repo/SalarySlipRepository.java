package com.jaywant.demo.Repo;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.SalarySlip;
import com.jaywant.demo.Entity.Subadmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalarySlipRepository extends JpaRepository<SalarySlip, Long> {

    // Find salary slips by employee
    List<SalarySlip> findByEmployee(Employee employee);

    // Find salary slips by subadmin
    List<SalarySlip> findBySubadmin(Subadmin subadmin);

    // Find salary slips by employee and date range
    @Query("SELECT s FROM SalarySlip s WHERE s.employee = :employee AND s.startDate = :startDate AND s.endDate = :endDate")
    Optional<SalarySlip> findByEmployeeAndDateRange(@Param("employee") Employee employee, 
                                                   @Param("startDate") String startDate, 
                                                   @Param("endDate") String endDate);

    // Find salary slips by subadmin and employee
    List<SalarySlip> findBySubadminAndEmployee(Subadmin subadmin, Employee employee);

    // Find salary slips by employee ID and subadmin ID
    @Query("SELECT s FROM SalarySlip s WHERE s.employee.empId = :empId AND s.subadmin.id = :subadminId")
    List<SalarySlip> findByEmployeeIdAndSubadminId(@Param("empId") int empId, @Param("subadminId") int subadminId);

    // Find latest salary slip for an employee
    @Query("SELECT s FROM SalarySlip s WHERE s.employee = :employee ORDER BY s.createdAt DESC")
    List<SalarySlip> findLatestByEmployee(@Param("employee") Employee employee);

    // Find salary slips by pay slip month
    List<SalarySlip> findByPaySlipMonth(String paySlipMonth);

    // Find salary slips by subadmin and pay slip month
    List<SalarySlip> findBySubadminAndPaySlipMonth(Subadmin subadmin, String paySlipMonth);
}
