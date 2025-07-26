package com.jaywant.demo.Repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.Subadmin;

@Repository
public interface EmployeeRepo extends JpaRepository<Employee, Integer> {

        /**
         * Finds an employee by subadmin's id and full name.
         * (Note: This method requires a subadmin id to be provided.)
         */
        @Query("SELECT e FROM Employee e WHERE e.subadmin.id = :subadminId AND CONCAT(e.firstName, ' ', e.lastName) = :fullName")
        Employee findBySubadminIdAndFullName(int subadminId, String fullName);

        Employee findBySubadminIdAndEmpId(int subadminId, int empId);

        Employee findBySubadminIdAndEmail(int subadminId, String Email);

        @Query("SELECT e FROM Employee e WHERE e.subadmin.id = :subadminId AND LOWER(TRIM(CONCAT(e.firstName, ' ', e.lastName))) = LOWER(:fullName) AND e.email = :email")
        Employee findBySubadminIdAndFullNameAndEmail(int subadminId, String fullName, String email);

        @Query("SELECT e FROM Employee e WHERE LOWER(TRIM(CONCAT(e.firstName, ' ', e.lastName))) = LOWER(:fullName)")
        Employee findByFullName(String fullName);

        Optional<Employee> findByEmail(String email);

        List<Employee> findBySubadminId(int subadminId);

        @Query("SELECT e FROM Employee e WHERE e.subadmin = :subadmin AND LOWER(TRIM(e.firstName)) = LOWER(TRIM(:firstName)) AND LOWER(TRIM(e.lastName)) = LOWER(TRIM(:lastName))")
        Employee findBySubadminAndFirstNameAndLastNameIgnoreCase(
                        @Param("subadmin") Subadmin subadmin,
                        @Param("firstName") String firstName,
                        @Param("lastName") String lastName);

        int countBySubadmin(Subadmin subadmin);

        // Enhanced method for multi-subadmin employee lookup with Integer types
        Employee findByEmpIdAndSubadminId(Integer empId, Integer subadminId);

        // Find employees by device serial number for biometric device integration
        List<Employee> findByDeviceSerialNumber(String deviceSerialNumber);

}
