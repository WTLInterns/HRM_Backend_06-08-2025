package com.jaywant.demo.Service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

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

            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .setAndroidConfig(androidConfig);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();
            String response = firebaseMessaging.send(message);
            System.out.println("? Notification sent successfully: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("? Failed to send notification: " + e.getMessage());
            return null;
        }
    }

    public boolean isValidToken(String token) {
        return token != null && !token.trim().isEmpty();
    }

    public boolean isFirebaseAvailable() {
        return isFirebaseEnabled && firebaseMessaging != null;
    }
}