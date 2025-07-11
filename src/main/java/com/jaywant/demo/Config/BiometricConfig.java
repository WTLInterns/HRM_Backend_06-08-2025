package com.jaywant.demo.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "biometric")
public class BiometricConfig {

    private Realtime realtime = new Realtime();
    private Storage storage = new Storage();

    // Getters and setters
    public Realtime getRealtime() {
        return realtime;
    }

    public void setRealtime(Realtime realtime) {
        this.realtime = realtime;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    // Real-time configuration
    public static class Realtime {
        private boolean enabled = true;
        private int tcpPort = 8000;
        private boolean processingEnabled = true;
        private boolean duplicateDetection = true;
        private int duplicateWindowMinutes = 2;

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTcpPort() {
            return tcpPort;
        }

        public void setTcpPort(int tcpPort) {
            this.tcpPort = tcpPort;
        }

        public boolean isProcessingEnabled() {
            return processingEnabled;
        }

        public void setProcessingEnabled(boolean processingEnabled) {
            this.processingEnabled = processingEnabled;
        }

        public boolean isDuplicateDetection() {
            return duplicateDetection;
        }

        public void setDuplicateDetection(boolean duplicateDetection) {
            this.duplicateDetection = duplicateDetection;
        }

        public int getDuplicateWindowMinutes() {
            return duplicateWindowMinutes;
        }

        public void setDuplicateWindowMinutes(int duplicateWindowMinutes) {
            this.duplicateWindowMinutes = duplicateWindowMinutes;
        }
    }

    // Storage configuration
    public static class Storage {
        private boolean storeRawData = true;
        private boolean storeTemplates = false;
        private List<String> verifyTypes = Arrays.asList("fingerprint", "face", "palm");

        // Getters and setters
        public boolean isStoreRawData() {
            return storeRawData;
        }

        public void setStoreRawData(boolean storeRawData) {
            this.storeRawData = storeRawData;
        }

        public boolean isStoreTemplates() {
            return storeTemplates;
        }

        public void setStoreTemplates(boolean storeTemplates) {
            this.storeTemplates = storeTemplates;
        }

        public List<String> getVerifyTypes() {
            return verifyTypes;
        }

        public void setVerifyTypes(List<String> verifyTypes) {
            this.verifyTypes = verifyTypes;
        }
    }
}

@Configuration
@ConfigurationProperties(prefix = "attendance")
class AttendanceConfig {

    private Processing processing = new Processing();

    // Getters and setters
    public Processing getProcessing() {
        return processing;
    }

    public void setProcessing(Processing processing) {
        this.processing = processing;
    }

    // Processing configuration
    public static class Processing {
        private boolean smartPunchDetection = true;
        private LocalTime autoCheckoutTime = LocalTime.of(18, 0);
        private boolean workingHoursCalculation = true;
        private boolean breakDurationCalculation = true;

        // Getters and setters
        public boolean isSmartPunchDetection() {
            return smartPunchDetection;
        }

        public void setSmartPunchDetection(boolean smartPunchDetection) {
            this.smartPunchDetection = smartPunchDetection;
        }

        public LocalTime getAutoCheckoutTime() {
            return autoCheckoutTime;
        }

        public void setAutoCheckoutTime(LocalTime autoCheckoutTime) {
            this.autoCheckoutTime = autoCheckoutTime;
        }

        public boolean isWorkingHoursCalculation() {
            return workingHoursCalculation;
        }

        public void setWorkingHoursCalculation(boolean workingHoursCalculation) {
            this.workingHoursCalculation = workingHoursCalculation;
        }

        public boolean isBreakDurationCalculation() {
            return breakDurationCalculation;
        }

        public void setBreakDurationCalculation(boolean breakDurationCalculation) {
            this.breakDurationCalculation = breakDurationCalculation;
        }
    }
}
