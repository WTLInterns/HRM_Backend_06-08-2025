package com.jaywant.demo.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subadmin_terminals")
public class SubadminTerminal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subadmin_id", nullable = false)
    private Integer subadminId;

    @Column(name = "terminal_serial", nullable = false, length = 50)
    private String terminalSerial;

    @Column(name = "terminal_name", length = 100)
    private String terminalName;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "easytime_terminal_id")
    private Integer easytimeTerminalId;

    @Column(name = "easytime_api_url", length = 255)
    private String easytimeApiUrl;

    @Column(name = "api_token", length = 500)
    private String apiToken;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TerminalStatus status = TerminalStatus.INACTIVE;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subadmin_id", insertable = false, updatable = false)
    private Subadmin subadmin;

    public enum TerminalStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE,
        ERROR
    }

    // Constructors
    public SubadminTerminal() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public SubadminTerminal(Integer subadminId, String terminalSerial, String terminalName, String location) {
        this();
        this.subadminId = subadminId;
        this.terminalSerial = terminalSerial;
        this.terminalName = terminalName;
        this.location = location;
    }

    // Lifecycle callbacks
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSubadminId() {
        return subadminId;
    }

    public void setSubadminId(Integer subadminId) {
        this.subadminId = subadminId;
    }

    public String getTerminalSerial() {
        return terminalSerial;
    }

    public void setTerminalSerial(String terminalSerial) {
        this.terminalSerial = terminalSerial;
    }

    public String getTerminalName() {
        return terminalName;
    }

    public void setTerminalName(String terminalName) {
        this.terminalName = terminalName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getEasytimeTerminalId() {
        return easytimeTerminalId;
    }

    public void setEasytimeTerminalId(Integer easytimeTerminalId) {
        this.easytimeTerminalId = easytimeTerminalId;
    }

    public String getEasytimeApiUrl() {
        return easytimeApiUrl;
    }

    public void setEasytimeApiUrl(String easytimeApiUrl) {
        this.easytimeApiUrl = easytimeApiUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public TerminalStatus getStatus() {
        return status;
    }

    public void setStatus(TerminalStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Subadmin getSubadmin() {
        return subadmin;
    }

    public void setSubadmin(Subadmin subadmin) {
        this.subadmin = subadmin;
    }

    // Utility methods
    public boolean isActive() {
        return this.status == TerminalStatus.ACTIVE;
    }

    public void updateLastSync() {
        this.lastSyncAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "SubadminTerminal{" +
                "id=" + id +
                ", subadminId=" + subadminId +
                ", terminalSerial='" + terminalSerial + '\'' +
                ", terminalName='" + terminalName + '\'' +
                ", location='" + location + '\'' +
                ", status=" + status +
                ", lastSyncAt=" + lastSyncAt +
                '}';
    }
}
