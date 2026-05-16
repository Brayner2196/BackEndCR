package com.backendcr.residentialcomplex.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.json:}")
    private String firebaseCredentialsJson;

    @PostConstruct
    public void inicializar() {
        if (firebaseCredentialsJson == null || firebaseCredentialsJson.isBlank()) {
            log.warn("Firebase no configurado: la variable FIREBASE_CREDENTIALS_JSON está vacía. Las notificaciones push estarán deshabilitadas.");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            InputStream credenciales = new ByteArrayInputStream(
                firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8)
            );

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credenciales))
                .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase inicializado correctamente.");
        } catch (Exception e) {
            log.error("Error al inicializar Firebase: {}", e.getMessage(), e);
        }
    }
}
