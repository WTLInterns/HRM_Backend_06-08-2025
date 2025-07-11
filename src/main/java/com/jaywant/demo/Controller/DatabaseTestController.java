package com.jaywant.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/database-test")
@CrossOrigin(origins = "*")
public class DatabaseTestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Test database connectivity and list available databases
     */
    @GetMapping("/connectivity")
    public ResponseEntity<?> testConnectivity() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test basic connection
            String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            result.put("mysqlVersion", version);
            result.put("connectionStatus", "OK");
            
            // List all databases
            List<Map<String, Object>> databases = jdbcTemplate.queryForList(
                "SELECT SCHEMA_NAME as database_name FROM INFORMATION_SCHEMA.SCHEMATA"
            );
            result.put("availableDatabases", databases);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("connectionStatus", "FAILED");
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Test specific database and table access
     */
    @GetMapping("/check-table")
    public ResponseEntity<?> checkTable(
            @RequestParam String database, 
            @RequestParam String table) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Check if database exists
            String checkDbSql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
            List<Map<String, Object>> dbResult = jdbcTemplate.queryForList(checkDbSql, database);
            
            if (dbResult.isEmpty()) {
                result.put("status", "DATABASE_NOT_FOUND");
                result.put("message", "Database '" + database + "' does not exist");
                return ResponseEntity.ok(result);
            }
            
            // Check if table exists
            String checkTableSql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
            List<Map<String, Object>> tableResult = jdbcTemplate.queryForList(checkTableSql, database, table);
            
            if (tableResult.isEmpty()) {
                result.put("status", "TABLE_NOT_FOUND");
                result.put("message", "Table '" + database + "." + table + "' does not exist");
                
                // List available tables in the database
                String listTablesSql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
                List<Map<String, Object>> availableTables = jdbcTemplate.queryForList(listTablesSql, database);
                result.put("availableTables", availableTables);
                
                return ResponseEntity.ok(result);
            }
            
            // Test table access and get basic info
            String countSql = String.format("SELECT COUNT(*) as record_count FROM %s.%s", database, table);
            Integer recordCount = jdbcTemplate.queryForObject(countSql, Integer.class);
            
            // Get table structure
            String structureSql = String.format(
                "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?"
            );
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(structureSql, database, table);
            
            result.put("status", "OK");
            result.put("database", database);
            result.put("table", table);
            result.put("recordCount", recordCount);
            result.put("columns", columns);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("database", database);
            error.put("table", table);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get sample data from a table
     */
    @GetMapping("/sample-data")
    public ResponseEntity<?> getSampleData(
            @RequestParam String database, 
            @RequestParam String table,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            // First check if table exists
            String checkTableSql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
            List<Map<String, Object>> tableResult = jdbcTemplate.queryForList(checkTableSql, database, table);
            
            if (tableResult.isEmpty()) {
                return ResponseEntity.badRequest().body("Table " + database + "." + table + " does not exist");
            }
            
            // Get sample data
            String sampleSql = String.format("SELECT * FROM %s.%s LIMIT ?", database, table);
            List<Map<String, Object>> sampleData = jdbcTemplate.queryForList(sampleSql, limit);
            
            Map<String, Object> result = new HashMap<>();
            result.put("database", database);
            result.put("table", table);
            result.put("sampleData", sampleData);
            result.put("recordCount", sampleData.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("database", database);
            error.put("table", table);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Test EasyTimePro specific tables
     */
    @GetMapping("/easytimepro-tables")
    public ResponseEntity<?> testEasyTimeProTables() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test new_hrm database
            result.put("new_hrm", testDatabaseTables("new_hrm"));
            
            // Test easywdms database
            result.put("easywdms", testDatabaseTables("easywdms"));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error testing EasyTimePro tables: " + e.getMessage());
        }
    }

    private Map<String, Object> testDatabaseTables(String database) {
        Map<String, Object> dbInfo = new HashMap<>();
        
        try {
            // Check if database exists
            String checkDbSql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
            List<Map<String, Object>> dbResult = jdbcTemplate.queryForList(checkDbSql, database);
            
            if (dbResult.isEmpty()) {
                dbInfo.put("exists", false);
                dbInfo.put("message", "Database does not exist");
                return dbInfo;
            }
            
            dbInfo.put("exists", true);
            
            // List all tables
            String listTablesSql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(listTablesSql, database);
            dbInfo.put("tables", tables);
            
            // Check for iclock_transaction table specifically
            boolean hasIclockTransaction = tables.stream()
                .anyMatch(table -> "iclock_transaction".equals(table.get("TABLE_NAME")));
            
            dbInfo.put("hasIclockTransaction", hasIclockTransaction);
            
            if (hasIclockTransaction) {
                try {
                    String countSql = String.format("SELECT COUNT(*) FROM %s.iclock_transaction", database);
                    Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
                    dbInfo.put("iclockTransactionCount", count);
                    
                    // Get recent records
                    String recentSql = String.format(
                        "SELECT COUNT(*) FROM %s.iclock_transaction WHERE punch_time >= CURDATE() - INTERVAL 7 DAYS", 
                        database
                    );
                    Integer recentCount = jdbcTemplate.queryForObject(recentSql, Integer.class);
                    dbInfo.put("recentRecords", recentCount);
                    
                } catch (Exception e) {
                    dbInfo.put("iclockTransactionError", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            dbInfo.put("error", e.getMessage());
        }
        
        return dbInfo;
    }
}
