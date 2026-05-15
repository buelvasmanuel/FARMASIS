package com.admin.adminlfarma_mini.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    @Autowired
    private Cloudinary cloudinary;

    public String subirImagen(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }

        try {
            log.info("Subiendo imagen a Cloudinary: {}", archivo.getOriginalFilename());
            Map uploadResult = cloudinary.uploader().upload(archivo.getBytes(), ObjectUtils.emptyMap());
            String url = uploadResult.get("url").toString();
            log.info("Imagen subida con éxito. URL: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Error al subir imagen a Cloudinary: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extrae el public ID de una URL de Cloudinary.
     *
     * @param url La URL completa de la imagen
     * @return El public ID, o null si no se puede extraer
     */
    public String extractPublicId(String url) {
        if (url == null || !url.contains("cloudinary"))
            return null;
        try {
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                // Remover versión si existe (v1234567890/)
                if (path.matches("^v\\d+/.*")) {
                    path = path.substring(path.indexOf("/") + 1);
                }
                // Remover extensión
                return path.replaceAll("\\.[^.]+$", "");
            }
        } catch (Exception e) {
            log.error("Error al extraer publicId de URL: {}", e.getMessage());
        }
        return null;
    }
}
