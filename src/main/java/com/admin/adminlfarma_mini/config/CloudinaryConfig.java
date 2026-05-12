package com.admin.adminlfarma_mini.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryConfig.class);

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        if (cloudName == null || cloudName.isEmpty() ||
            apiKey == null || apiKey.isEmpty() ||
            apiSecret == null || apiSecret.isEmpty()) {
            log.warn("⚠️ Cloudinary NO configurado — las credenciales están vacías. El servicio de subida de imágenes no estará disponible.");
            // Retornar instancia vacía para evitar NPE; el servicio verificará isConfigured()
            return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "not-configured",
                "api_key", "not-configured",
                "api_secret", "not-configured"
            ));
        }

        log.info("✅ Cloudinary configurado correctamente para cloud: {}", cloudName);
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret
        ));
    }

    public boolean isConfigured() {
        return cloudName != null && !cloudName.isEmpty()
            && apiKey != null && !apiKey.isEmpty()
            && apiSecret != null && !apiSecret.isEmpty();
    }
}
