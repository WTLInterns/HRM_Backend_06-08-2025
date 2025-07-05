package com.jaywant.demo.Repo;

import com.jaywant.demo.Entity.LocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LocationHistoryRepository extends JpaRepository<LocationHistory, Long> {

    // Find location history for a specific employee within a time range
    List<LocationHistory> findByEmployeeEmpIdAndTimestampBetweenOrderByTimestampAsc(int empId, LocalDateTime start, LocalDateTime end);

    // Find location history for all employees of a subadmin within a time range
    @Query("SELECT lh FROM LocationHistory lh WHERE lh.employee.subadmin.id = :subadminId AND lh.timestamp BETWEEN :start AND :end ORDER BY lh.employee.empId, lh.timestamp ASC")
    List<LocationHistory> findBySubadminIdAndTimestampBetween(
            @Param("subadminId") int subadminId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
} 