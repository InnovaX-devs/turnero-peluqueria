package com.innovax.barberia.barbero;

import com.innovax.barberia.common.DatosInvalidosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/** Sube y borra fotos en un bucket PÚBLICO de Supabase Storage. */
@Slf4j
@Service
public class FotoStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private record Formato(String mime, String ext) {
    }

    private final RestClient http = RestClient.create();
    private final String baseUrl;
    private final String serviceKey;
    private final String bucket;

    public FotoStorageService(
            @Value("${app.storage.supabase-url:}") String url,
            @Value("${app.storage.service-key:}") String serviceKey,
            @Value("${app.storage.bucket:fotos-barberos}") String bucket) {
        this.baseUrl = url.replaceAll("/+$", "");
        this.serviceKey = serviceKey;
        this.bucket = bucket;
    }

    /** Devuelve la URL pública de la foto subida. */
    public String subir(UUID barberiaId, UUID barberoId, MultipartFile archivo) {
        return subirArchivo(barberiaId, barberoId.toString(), archivo);
    }

    /** Sube el logo de la barbería (mismo bucket, carpeta de la barbería). Devuelve la URL pública. */
    public String subirLogo(UUID barberiaId, MultipartFile archivo) {
        return subirArchivo(barberiaId, "logo", archivo);
    }

    private String subirArchivo(UUID barberiaId, String nombre, MultipartFile archivo) {
        if (baseUrl.isBlank() || serviceKey.isBlank()) {
            throw new IllegalStateException("Falta configurar SUPABASE_URL y SUPABASE_SERVICE_KEY");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new DatosInvalidosException("foto", "Elegí una imagen");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new DatosInvalidosException("foto", "La foto no puede pesar más de 5 MB");
        }
        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // Se detecta por el contenido real, no por el Content-Type que manda el cliente
        Formato formato = detectar(bytes);
        if (formato == null) {
            throw new DatosInvalidosException("foto", "Solo se aceptan imágenes JPG, PNG o WEBP");
        }

        // Nombre único por subida: evita que el navegador/CDN muestre la foto vieja cacheada
        String ruta = barberiaId + "/" + nombre + "-" + UUID.randomUUID() + "." + formato.ext();
        try {
            http.post()
                    .uri(baseUrl + "/storage/v1/object/" + bucket + "/" + ruta)
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .contentType(MediaType.parseMediaType(formato.mime()))
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("No se pudo subir la foto a Supabase (revisá la service key y el bucket)", e);
            throw new IllegalStateException("No se pudo subir la foto", e);
        }
        return prefijoPublico() + ruta;
    }

    /** Borra una foto vieja. Si falla solo se loguea: no debe romper la operación principal. */
    public void borrarSilencioso(String fotoUrl) {
        if (fotoUrl == null || baseUrl.isBlank() || !fotoUrl.startsWith(prefijoPublico())) {
            return;
        }
        String ruta = fotoUrl.substring(prefijoPublico().length());
        try {
            http.delete()
                    .uri(baseUrl + "/storage/v1/object/" + bucket + "/" + ruta)
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo borrar la foto vieja {}", ruta, e);
        }
    }

    private String prefijoPublico() {
        return baseUrl + "/storage/v1/object/public/" + bucket + "/";
    }

    private static Formato detectar(byte[] b) {
        if (b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return new Formato("image/jpeg", "jpg");
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return new Formato("image/png", "png");
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return new Formato("image/webp", "webp");
        }
        return null;
    }
}