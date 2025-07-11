package com.jaywant.demo.Repo;

import com.jaywant.demo.Entity.SubadminTerminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubadminTerminalRepo extends JpaRepository<SubadminTerminal, Long> {

    // Find by subadmin ID
    List<SubadminTerminal> findBySubadminId(Integer subadminId);

    // Find by terminal serial (unique)
    Optional<SubadminTerminal> findByTerminalSerial(String terminalSerial);

    // Find by subadmin and terminal serial
    Optional<SubadminTerminal> findBySubadminIdAndTerminalSerial(Integer subadminId, String terminalSerial);

    // Find active terminals for a subadmin
    List<SubadminTerminal> findBySubadminIdAndStatus(Integer subadminId, SubadminTerminal.TerminalStatus status);

    // Find all active terminals
    List<SubadminTerminal> findByStatus(SubadminTerminal.TerminalStatus status);

    // Check if terminal serial exists
    boolean existsByTerminalSerial(String terminalSerial);

    // Check if subadmin has a specific terminal
    boolean existsBySubadminIdAndTerminalSerial(Integer subadminId, String terminalSerial);

    // Find terminals by location
    List<SubadminTerminal> findBySubadminIdAndLocationContainingIgnoreCase(Integer subadminId, String location);

    // Find terminals that haven't synced recently
    @Query("SELECT st FROM SubadminTerminal st WHERE st.lastSyncAt < :cutoffTime OR st.lastSyncAt IS NULL")
    List<SubadminTerminal> findTerminalsNotSyncedSince(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Find terminals by EasyTime terminal ID
    Optional<SubadminTerminal> findByEasytimeTerminalId(Integer easytimeTerminalId);

    // Count active terminals for a subadmin
    @Query("SELECT COUNT(st) FROM SubadminTerminal st WHERE st.subadminId = :subadminId AND st.status = 'ACTIVE'")
    Long countActiveTerminalsBySubadmin(@Param("subadminId") Integer subadminId);

    // Find terminals with API token
    @Query("SELECT st FROM SubadminTerminal st WHERE st.apiToken IS NOT NULL AND st.apiToken != ''")
    List<SubadminTerminal> findTerminalsWithApiToken();

    // Find terminals by subadmin with status
    @Query("SELECT st FROM SubadminTerminal st WHERE st.subadminId = :subadminId AND st.status IN :statuses")
    List<SubadminTerminal> findBySubadminIdAndStatusIn(
            @Param("subadminId") Integer subadminId, 
            @Param("statuses") List<SubadminTerminal.TerminalStatus> statuses);

    // Delete terminals by subadmin ID
    void deleteBySubadminId(Integer subadminId);

    // Find terminals needing maintenance
    @Query("SELECT st FROM SubadminTerminal st WHERE st.status = 'ERROR' OR st.status = 'MAINTENANCE'")
    List<SubadminTerminal> findTerminalsNeedingMaintenance();
}
