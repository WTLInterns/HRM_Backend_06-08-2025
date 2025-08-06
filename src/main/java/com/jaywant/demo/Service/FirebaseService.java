package com.jaywant.demo.Service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
public class FirebaseService {

    @Autowired(required = false) // Make autowiring optional
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private boolean isFirebaseEnabled;

    public String sendNotification(String token, String title, String body, Map<String, String> data) {
        // Check if Firebase is available
        if (!isFirebaseEnabled || firebaseMessaging == null) {
            System.out.println("?? Firebase not available - logging notification instead:");
            System.out.println("?? Title: " + title);
            System.out.println("?? Body: " + body);
            System.out.println("?? Token: " + token);
            return "firebase-not-available";
        }

        if (token == null || token.trim().isEmpty()) {
            System.out.println("?? No FCM token available for user");
            return "no-token";
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // Create Android-specific configuration for better notification display
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .setIcon("ic_notification") // Default icon
                            .setColor("#FF6B35") // Orange color for notification
                            .setSound("default")
                            .setChannelId("default")
                            .build())
                    .build();

            // Create iOS-specific configuration
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setAlert(ApsAlert.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .setBadge(1)
                            .setSound("default")
                            .setContentAvailable(true)
                            .setMutableContent(true)
                            .build())
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .setAndroidConfig(androidConfig)
                    .setApnsConfig(apnsConfig); // Add iOS configuration

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();
            String response = firebaseMessaging.send(message);
            System.out.println("✅ Notification sent successfully: " + response);
            System.out.println("📱 Token type detected: " + (isIOSToken(token) ? "iOS" : "Android"));
            return response;
        } catch (Exception e) {
            System.err.println("❌ Failed to send notification: " + e.getMessage());
            System.err.println("🔍 Token: " + token);
            System.err.println("📱 Token type: " + (isIOSToken(token) ? "iOS" : "Android"));
            if (isIOSToken(token)) {
                System.err.println("🍎 iOS notification failed - check Apple Push Notification service configuration");
                System.err.println("💡 Ensure your Firebase project has valid APNs certificates");
            }
            e.printStackTrace();
            return null;
        }
    }

    public boolean isValidToken(String token) {
        return token != null && !token.trim().isEmpty();
    }

    public boolean isFirebaseAvailable() {
        return isFirebaseEnabled && firebaseMessaging != null;
    }

    /**
     * Detect if the FCM token is from an iOS device
     * iOS tokens are typically longer and have different patterns than Android
     * tokens
     */
    private boolean isIOSToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        // iOS tokens are typically 64 characters long and contain only hexadecimal
        // characters
        // Android tokens are longer and contain various characters including
        // underscores, hyphens

        // Check for typical iOS token characteristics:
        // 1. Length around 64 characters
        // 2. Only contains hexadecimal characters (0-9, a-f, A-F)
        if (token.length() == 64 && token.matches("^[0-9a-fA-F]+$")) {
            return true;
        }

        // Additional check: Android tokens typically contain colons, underscores, or
        // hyphens
        // iOS tokens typically don't
        if (!token.contains(":") && !token.contains("_") && !token.contains("-") && token.length() >= 60) {
            return true;
        }

        return false;
    }
}