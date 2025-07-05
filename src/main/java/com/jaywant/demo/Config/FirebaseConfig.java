package com.jaywant.demo.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private boolean firebaseInitialized = false;

    // Firebase Environment Variables
    @Value("${firebase.type:}")
    private String firebaseType;

    @Value("${firebase.project-id:}")
    private String firebaseProjectId;

    @Value("${firebase.private-key-id:}")
    private String firebasePrivateKeyId;

    @Value("${firebase.private-key:}")
    private String firebasePrivateKey;

    @Value("${firebase.client-email:}")
    private String firebaseClientEmail;

    @Value("${firebase.client-id:}")
    private String firebaseClientId;

    @Value("${firebase.auth-uri:}")
    private String firebaseAuthUri;

    @Value("${firebase.token-uri:}")
    private String firebaseTokenUri;

    @Value("${firebase.auth-provider-x509-cert-url:}")
    private String firebaseAuthProviderX509CertUrl;

    @Value("${firebase.client-x509-cert-url:}")
    private String firebaseClientX509CertUrl;

    @Value("${firebase.universe-domain:}")
    private String firebaseUniverseDomain;

    @PostConstruct
    public void initialize() {
        try {
            System.out.println("🔥 Initializing Firebase...");

            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = null;

                // Try Base64 encoded Firebase credentials first (GitHub-safe)
                String encodedCredentials = System.getenv("FIREBASE_CREDENTIALS_BASE64");
                if (encodedCredentials != null && !encodedCredentials.trim().isEmpty()) {
                    System.out.println("🔐 Using Base64 encoded Firebase credentials...");
                    credentials = createCredentialsFromBase64(encodedCredentials);
                    System.out.println("✅ Firebase credentials decoded from Base64!");
                } else if (isEnvironmentVariablesConfigured()) {
                    System.out.println("🌍 Using Firebase environment variables...");
                    credentials = createCredentialsFromEnvironmentVariables();
                    System.out.println("✅ Firebase credentials created from environment variables!");
                } else {
                    // Fallback to JSON file
                    System.out.println("📁 Using firebase-service-account.json file...");
                    ClassPathResource resource = new ClassPathResource("firebase-service-account.json");

                    if (!resource.exists()) {
                        System.err.println("❌ Firebase service account file not found!");
                        System.err.println(
                                "Expected: FIREBASE_CREDENTIALS_BASE64 env var or firebase-service-account.json");
                        firebaseInitialized = false;
                        return;
                    }

                    System.out.println("✅ Firebase service account file found!");
                    InputStream serviceAccount = resource.getInputStream();
                    credentials = GoogleCredentials.fromStream(serviceAccount);
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                firebaseInitialized = true;
                System.out.println("✅ Firebase initialized successfully!");
            } else {
                firebaseInitialized = true;
                System.out.println("✅ Firebase was already initialized");
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to initialize Firebase (IOException): " + e.getMessage());
            e.printStackTrace();
            firebaseInitialized = false;
        } catch (Exception e) {
            System.err.println("❌ Unexpected Firebase error: " + e.getMessage());
            e.printStackTrace();
            firebaseInitialized = false;
        }
    }

    private boolean isEnvironmentVariablesConfigured() {
        return firebasePrivateKey != null && !firebasePrivateKey.trim().isEmpty() &&
                firebaseClientEmail != null && !firebaseClientEmail.trim().isEmpty() &&
                firebaseProjectId != null && !firebaseProjectId.trim().isEmpty();
    }

    private GoogleCredentials createCredentialsFromEnvironmentVariables() throws IOException {
        // Create JSON string from environment variables
        String jsonCredentials = String.format(
                "{\n" +
                        "  \"type\": \"%s\",\n" +
                        "  \"project_id\": \"%s\",\n" +
                        "  \"private_key_id\": \"%s\",\n" +
                        "  \"private_key\": \"%s\",\n" +
                        "  \"client_email\": \"%s\",\n" +
                        "  \"client_id\": \"%s\",\n" +
                        "  \"auth_uri\": \"%s\",\n" +
                        "  \"token_uri\": \"%s\",\n" +
                        "  \"auth_provider_x509_cert_url\": \"%s\",\n" +
                        "  \"client_x509_cert_url\": \"%s\",\n" +
                        "  \"universe_domain\": \"%s\"\n" +
                        "}",
                firebaseType != null ? firebaseType : "service_account",
                firebaseProjectId,
                firebasePrivateKeyId != null ? firebasePrivateKeyId : "",
                firebasePrivateKey.replace("\\n", "\n"), // Handle escaped newlines
                firebaseClientEmail,
                firebaseClientId != null ? firebaseClientId : "",
                firebaseAuthUri != null ? firebaseAuthUri : "https://accounts.google.com/o/oauth2/auth",
                firebaseTokenUri != null ? firebaseTokenUri : "https://oauth2.googleapis.com/token",
                firebaseAuthProviderX509CertUrl != null ? firebaseAuthProviderX509CertUrl
                        : "https://www.googleapis.com/oauth2/v1/certs",
                firebaseClientX509CertUrl != null ? firebaseClientX509CertUrl : "",
                firebaseUniverseDomain != null ? firebaseUniverseDomain : "googleapis.com");

        return GoogleCredentials.fromStream(new ByteArrayInputStream(jsonCredentials.getBytes()));
    }

    private GoogleCredentials createCredentialsFromBase64(String encodedCredentials) throws IOException {
        // Decode Base64 encoded Firebase credentials
        byte[] decodedBytes = java.util.Base64.getDecoder().decode(encodedCredentials);
        String jsonCredentials = new String(decodedBytes);
        return GoogleCredentials.fromStream(new ByteArrayInputStream(jsonCredentials.getBytes()));
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        System.out.println("🔧 Creating FirebaseMessaging bean...");

        if (!firebaseInitialized) {
            System.err.println("⚠️ Firebase not initialized - returning null FirebaseMessaging");
            return null;
        }

        try {
            FirebaseMessaging messaging = FirebaseMessaging.getInstance();
            System.out.println("✅ FirebaseMessaging bean created successfully!");
            return messaging;
        } catch (Exception e) {
            System.err.println("❌ Failed to get FirebaseMessaging: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Bean
    public boolean isFirebaseEnabled() {
        System.out.println("🔧 Firebase enabled status: " + firebaseInitialized);
        return firebaseInitialized;
    }
}