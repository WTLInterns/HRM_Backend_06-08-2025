package com.jaywant.demo.Repo;

import com.jaywant.demo.Entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

        List<NotificationLog> findByRecipientTypeAndRecipientIdOrderBySentAtDesc(
                        String recipientType, Integer recipientId);

        // For subadmins - exclude JOB_OPENING notifications
        List<NotificationLog> findByRecipientTypeAndRecipientIdAndNotificationTypeNotOrderBySentAtDesc(
                        String recipientType, Integer recipientId, String notificationType);

        @Query("SELECT COUNT(n) FROM NotificationLog n WHERE n.recipientType = :recipientType " +
                        "AND n.recipientId = :recipientId AND n.isRead = false")
        long countUnreadNotifications(@Param("recipientType") String recipientType,
                        @Param("recipientId") Integer recipientId);

        // For subadmins - count unread notifications excluding specific type
        @Query("SELECT COUNT(n) FROM NotificationLog n WHERE n.recipientType = :recipientType " +
                        "AND n.recipientId = :recipientId AND n.isRead = false AND n.notificationType != :excludeType")
        long countUnreadNotificationsExcludingType(@Param("recipientType") String recipientType,
                        @Param("recipientId") Integer recipientId,
                        @Param("excludeType") String excludeType);

        // Mark all notifications as read for a user
        @Modifying
        @Transactional
        @Query("UPDATE NotificationLog n SET n.isRead = true WHERE n.recipientType = :recipientType " +
                        "AND n.recipientId = :recipientId AND n.isRead = false")
        int markAllAsReadForUser(@Param("recipientType") String recipientType,
                        @Param("recipientId") Integer recipientId);

        // Mark all notifications as read for subadmin (excluding JOB_OPENING
        // notifications)
        @Modifying
        @Transactional
        @Query("UPDATE NotificationLog n SET n.isRead = true WHERE n.recipientType = :recipientType " +
                        "AND n.recipientId = :recipientId AND n.isRead = false AND n.notificationType != :excludeType")
        int markAllAsReadForSubadminExcludingType(@Param("recipientType") String recipientType,
                        @Param("recipientId") Integer recipientId,
                        @Param("excludeType") String excludeType);
}