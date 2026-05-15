package com.admin.adminlfarma_mini.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.admin.adminlfarma_mini.config.CloudinaryConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

/**
 * Servicio genérico para subir y eliminar imágenes en Cloudinary.
 * Soporta fotos de perfil y fotos de productos.
 * 
 * Si las credenciales de Cloudinary no están configuradas,
 * el servicio retornará null y los llamadores deben manejar
 * la alternativa (e.g. Base64 en BD).
 */
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;
    private final CloudinaryConfig cloudinaryConfig;

    /**
     * Verifica si el servicio Cloudinary está configurado con credenciales reales.
     */
    public boolean isConfigured() {
        return cloudinaryConfig.isConfigured();
    }

    /**
     * Sube una imagen a Cloudinary.
     *
     * @param file   El archivo a subir
     * @param folder La carpeta en Cloudinary (e.g. "perfiles", "productos")
     * @return La URL segura de la imagen subida, o null si no está configurado
     * @throws IOException si hay error en la subida
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (!isConfigured()) {
            log.warn("Cloudinary no configurado. No se puede subir la imagen.");
            return null;
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "lfarma/" + folder,
                        "resource_type", "image",
                        "transformation", "w_500,h_500,c_limit,q_auto"
                ));

        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Imagen subida exitosamente a Cloudinary: {}", secureUrl);
        return secureUrl;
    }

    /**
     * Elimina una imagen de Cloudinary por su public ID.
     *
     * @param publicId El identificador público de la imagen en Cloudinary
     */
    public void deleteImage(String publicId) {
        if (!isConfigured()) {
            log.warn("Cloudinary no configurado. No se puede eliminar la imagen.");
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Imagen eliminada de Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Error al eliminar imagen de Cloudinary: {}", e.getMessage());
        }
    }

    /**
     * Extrae el public ID de una URL de Cloudinary.
     *
     * @param url La URL completa de la imagen
     * @return El public ID, o null si no se puede extraer
     */
    public String extractPublicId(String url) {
        if (url == null || !url.contains("cloudinary")) return null;
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

    /**
     * Método simplificado para subir una imagen (usado por ProductoController).
     */
    public String subirImagen(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) return null;
        return uploadImage(archivo, "productos");
    }
}
