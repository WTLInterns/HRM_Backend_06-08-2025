package com.jaywant.demo.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "easytimepro")
public class EasyTimeProConfig {

    private Sync sync = new Sync();
    private Middleware middleware = new Middleware();

    // Getters and setters
    public Sync getSync() {
        return sync;
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }

    public Middleware getMiddleware() {
        return middleware;
    }

    public void setMiddleware(Middleware middleware) {
        this.middleware = middleware;
    }

    // Sync configuration
    public static class Sync {
        private boolean enabled = true;
        private long interval = 300000; // 5 minutes
        private String databases = "new_hrm,easywdms";

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getInterval() {
            return interval;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }

        public String getDatabases() {
            return databases;
        }

        public void setDatabases(String databases) {
            this.databases = databases;
        }
    }

    // Middleware configuration
    public static class Middleware {
        private boolean enabled = true;
        private String sourceDatabase = "easywdms";
        private String targetDatabase = "new_hrm";
        private String transactionTable = "iclock_transaction";
        private boolean autoSync = true;
        private long syncInterval = 60000; // 1 minute

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSourceDatabase() {
            return sourceDatabase;
        }

        public void setSourceDatabase(String sourceDatabase) {
            this.sourceDatabase = sourceDatabase;
        }

        public String getTargetDatabase() {
            return targetDatabase;
        }

        public void setTargetDatabase(String targetDatabase) {
            this.targetDatabase = targetDatabase;
        }

        public String getTransactionTable() {
            return transactionTable;
        }

        public void setTransactionTable(String transactionTable) {
            this.transactionTable = transactionTable;
        }

        public boolean isAutoSync() {
            return autoSync;
        }

        public void setAutoSync(boolean autoSync) {
            this.autoSync = autoSync;
        }

        public long getSyncInterval() {
            return syncInterval;
        }

        public void setSyncInterval(long syncInterval) {
            this.syncInterval = syncInterval;
        }
    }
}
