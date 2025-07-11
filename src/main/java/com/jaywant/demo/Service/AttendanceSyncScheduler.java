package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.SubadminTerminal;
import com.jaywant.demo.Repo.SubadminTerminalRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttendanceSyncScheduler {

    @Autowired
    private EasyTimeProIntegrationService integrationService;

    @Autowired
    private SubadminTerminalRepo terminalRepo;

    /**
     * Sync attendance data every 5 minutes for all active terminals
     * This ensures real-time attendance updates
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 milliseconds
    public void syncAttendanceDataPeriodically() {
        try {
            System.out.println("🔄 Starting periodic attendance sync at " + LocalDateTime.now());

            List<SubadminTerminal> activeTerminals = terminalRepo.findByStatus(SubadminTerminal.TerminalStatus.ACTIVE);

            for (SubadminTerminal terminal : activeTerminals) {
                try {
                    // Sync today's data
                    String today = LocalDate.now().toString();
                    integrationService.syncAttendanceData(terminal.getTerminalSerial(), today, today);

                    System.out.println("✅ Synced attendance for terminal: " + terminal.getTerminalSerial());

                } catch (Exception e) {
                    System.err.println(
                            "❌ Failed to sync terminal " + terminal.getTerminalSerial() + ": " + e.getMessage());

                    // Mark terminal as error if sync fails multiple times
                    terminal.setStatus(SubadminTerminal.TerminalStatus.ERROR);
                    terminalRepo.save(terminal);
                }
            }

            System.out.println(
                    "🏁 Completed periodic attendance sync. Processed " + activeTerminals.size() + " terminals");

        } catch (Exception e) {
            System.err.println("💥 Error in periodic attendance sync: " + e.getMessage());
        }
    }

    /**
     * Sync yesterday's data every hour to catch any missed transactions
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3,600,000 milliseconds
    public void syncYesterdayData() {
        try {
            System.out.println("🔄 Starting yesterday's data sync at " + LocalDateTime.now());

            List<SubadminTerminal> activeTerminals = terminalRepo.findByStatus(SubadminTerminal.TerminalStatus.ACTIVE);
            String yesterday = LocalDate.now().minusDays(1).toString();

            for (SubadminTerminal terminal : activeTerminals) {
                try {
                    integrationService.syncAttendanceData(terminal.getTerminalSerial(), yesterday, yesterday);
                    System.out.println("✅ Synced yesterday's data for terminal: " + terminal.getTerminalSerial());
                } catch (Exception e) {
                    System.err.println("❌ Failed to sync yesterday's data for terminal " +
                            terminal.getTerminalSerial() + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("💥 Error in yesterday's data sync: " + e.getMessage());
        }
    }

    /**
     * Health check for terminals every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes = 900,000 milliseconds
    public void performTerminalHealthCheck() {
        try {
            System.out.println("🏥 Starting terminal health check at " + LocalDateTime.now());

            List<SubadminTerminal> allTerminals = terminalRepo.findAll();

            for (SubadminTerminal terminal : allTerminals) {
                try {
                    // Skip if terminal is in maintenance
                    if (terminal.getStatus() == SubadminTerminal.TerminalStatus.MAINTENANCE) {
                        continue;
                    }

                    // Test connection
                    boolean isHealthy = testTerminalHealth(terminal);

                    if (isHealthy) {
                        if (terminal.getStatus() != SubadminTerminal.TerminalStatus.ACTIVE) {
                            terminal.setStatus(SubadminTerminal.TerminalStatus.ACTIVE);
                            terminalRepo.save(terminal);
                            System.out.println("✅ Terminal " + terminal.getTerminalSerial() + " is back online");
                        }
                    } else {
                        if (terminal.getStatus() == SubadminTerminal.TerminalStatus.ACTIVE) {
                            terminal.setStatus(SubadminTerminal.TerminalStatus.ERROR);
                            terminalRepo.save(terminal);
                            System.out.println("❌ Terminal " + terminal.getTerminalSerial() + " is offline");
                        }
                    }

                } catch (Exception e) {
                    System.err.println("❌ Health check failed for terminal " +
                            terminal.getTerminalSerial() + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("💥 Error in terminal health check: " + e.getMessage());
        }
    }

    /**
     * Weekly data sync every Sunday at 2 AM
     * Syncs the entire week's data to ensure no transactions are missed
     */
    @Scheduled(cron = "0 0 2 * * SUN") // Every Sunday at 2:00 AM
    public void weeklyDataSync() {
        try {
            System.out.println("📅 Starting weekly data sync at " + LocalDateTime.now());

            List<SubadminTerminal> activeTerminals = terminalRepo.findByStatus(SubadminTerminal.TerminalStatus.ACTIVE);

            // Sync last 7 days
            String fromDate = LocalDate.now().minusDays(7).toString();
            String toDate = LocalDate.now().toString();

            for (SubadminTerminal terminal : activeTerminals) {
                try {
                    integrationService.syncAttendanceData(terminal.getTerminalSerial(), fromDate, toDate);
                    System.out.println("✅ Weekly sync completed for terminal: " + terminal.getTerminalSerial());
                } catch (Exception e) {
                    System.err.println("❌ Weekly sync failed for terminal " +
                            terminal.getTerminalSerial() + ": " + e.getMessage());
                }
            }

            System.out.println("🏁 Weekly data sync completed");

        } catch (Exception e) {
            System.err.println("💥 Error in weekly data sync: " + e.getMessage());
        }
    }

    /**
     * Test terminal health by making a simple API call
     */
    private boolean testTerminalHealth(SubadminTerminal terminal) {
        try {
            // Use the API service to test connection
            EasyTimeProApiService apiService = new EasyTimeProApiService();
            return apiService.testConnection(terminal.getEasytimeApiUrl(), terminal.getApiToken());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Manual trigger for immediate sync of all terminals
     */
    public void triggerImmediateSync() {
        System.out.println("🚀 Manual sync triggered at " + LocalDateTime.now());
        syncAttendanceDataPeriodically();
    }

    /**
     * Sync specific date range for all terminals
     */
    public void syncDateRange(String fromDate, String toDate) {
        try {
            System.out.println("📊 Syncing date range " + fromDate + " to " + toDate + " at " + LocalDateTime.now());

            List<SubadminTerminal> activeTerminals = terminalRepo.findByStatus(SubadminTerminal.TerminalStatus.ACTIVE);

            for (SubadminTerminal terminal : activeTerminals) {
                try {
                    integrationService.syncAttendanceData(terminal.getTerminalSerial(), fromDate, toDate);
                    System.out.println("✅ Date range sync completed for terminal: " + terminal.getTerminalSerial());
                } catch (Exception e) {
                    System.err.println("❌ Date range sync failed for terminal " +
                            terminal.getTerminalSerial() + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("💥 Error in date range sync: " + e.getMessage());
        }
    }
}
