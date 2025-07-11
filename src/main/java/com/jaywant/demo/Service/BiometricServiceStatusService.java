package com.jaywant.demo.Service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BiometricServiceStatusService {

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 BIOMETRIC SERVICES STATUS - " + timestamp);
        System.out.println("=".repeat(80));
        System.out.println("✅ EnhancedBiometricSyncService: ACTIVE (10-second real-time sync)");
        System.out.println("✅ SimpleBiometricTestService: ACTIVE (for testing)");
        System.out.println("❌ BiometricAttendanceProcessor: DISABLED (replaced by enhanced service)");
        System.out.println("=".repeat(80));
        System.out.println("📊 AVAILABLE TEST ENDPOINTS:");
        System.out.println("   GET  /api/simple-biometric-test/health");
        System.out.println("   GET  /api/simple-biometric-test/stats");
        System.out.println("   POST /api/simple-biometric-test/test-processing");
        System.out.println("   GET  /api/iclock-verification/verify-iclock-table");
        System.out.println("   POST /api/iclock-verification/test-data-flow");
        System.out.println("   GET  /api/enhanced-biometric/sync-stats");
        System.out.println("   POST /api/enhanced-biometric/sync-now");
        System.out.println("=".repeat(80));
        System.out.println("🔄 Real-time sync will start automatically every 10 seconds...");
        System.out.println("📝 Monitor console for processing logs");
        System.out.println("=".repeat(80) + "\n");
    }
}
