package com.hotel.service;

import com.hotel.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Saves uploaded room photos to a folder on disk (default: "uploads" next to where the backend runs). */
@Service
public class FileStorageService {
    private final Path dir;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        System.out.println("Room photos are stored in: " + dir);
    }

    public Path getDir() {
        return dir;
    }

    /** Validates the file really is a JPG/PNG/WEBP, stores it under a random name and returns its URL path. */
    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Please choose an image file");
        try {
            byte[] head;
            try (InputStream in = file.getInputStream()) {
                head = in.readNBytes(12);
            }
            String ext = detectExtension(head);
            if (ext == null) throw ApiException.badRequest("Only JPG, PNG or WEBP images are allowed");

            String name = UUID.randomUUID() + ext;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the image");
        }
    }

    /** Deletes a previously stored image given its URL path (e.g. /uploads/abc.jpg). Ignores anything else. */
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;
        try {
            Path p = dir.resolve(url.substring("/uploads/".length())).normalize();
            if (p.startsWith(dir)) Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // an orphaned file is harmless
        }
    }

    // Checks the file's first bytes (magic numbers) instead of trusting the filename or content type.
    private static String detectExtension(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return ".jpg";
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return ".png";
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return ".webp";
        return null;
    }
}
