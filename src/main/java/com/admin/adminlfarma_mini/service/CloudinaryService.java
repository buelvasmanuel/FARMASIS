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
}
