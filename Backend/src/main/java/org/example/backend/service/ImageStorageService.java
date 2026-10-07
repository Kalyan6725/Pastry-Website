package org.example.backend.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final Path storageDir = Path.of("uploads", "images");

    public ImageStorageService() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create image storage directory", e);
        }
    }

    /**
     * Download image from remote URL and store under uploads/images, returning the public path
     * that can be served by the application (e.g. /images/{filename}).
     */
    public String storeFromUrl(String imageUrl) {
        try (InputStream in = openUrlStream(imageUrl)) {
            String ext = extractExtension(imageUrl);
            String filename = UUID.randomUUID().toString() + (ext.isEmpty() ? "" : "." + ext);
            Path target = storageDir.resolve(filename);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return "/images/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to download and store image", e);
        }
    }

    private InputStream openUrlStream(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        conn.setRequestProperty("User-Agent", "Pastry-Backend-ImageFetcher/1.0");
        return conn.getInputStream();
    }

    private String extractExtension(String imageUrl) {
        try {
            String path = new URL(imageUrl).getPath();
            int idx = path.lastIndexOf('.');
            if (idx > 0 && idx < path.length() - 1) {
                String ext = path.substring(idx + 1).toLowerCase();
                // Basic validation
                if (ext.matches("[a-z0-9]{1,6}")) return ext;
            }
        } catch (Exception ignored) {
        }
        return "jpg"; // default
    }
}
