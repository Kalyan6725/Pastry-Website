package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.dto.product.ProductImageResponse;
import org.example.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ImageStorageService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String folder;

    public ImageStorageService(
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret,
            @Value("${app.cloudinary.folder:pastry-products}") String folder
    ) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.folder = folder;
    }

    public ProductImageResponse uploadProductImage(MultipartFile file) {
        validate(file);
        ensureConfigured();

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("folder", folder);
        params.put("overwrite", "false");
        params.put("resource_type", "image");
        params.put("timestamp", timestamp);
        params.put("unique_filename", "true");
        params.put("use_filename", "true");
        String signature = sign(params);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl()))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary())
                    .POST(buildMultipartBody(file, params, signature))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = parseJson(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadRequestException(extractErrorMessage(body, "Unable to upload the image to Cloudinary."));
            }
            String publicId = text(body, "public_id");
            String secureUrl = text(body, "secure_url");
            if (publicId == null || secureUrl == null) {
                throw new BadRequestException("Cloudinary did not return an image URL.");
            }
            return new ProductImageResponse(publicId, secureUrl);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to upload the image to Cloudinary.");
        } catch (IOException ex) {
            throw new BadRequestException("Unable to upload the image to Cloudinary.");
        }
    }

    public void deleteProductImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        ensureConfigured();

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("public_id", publicId);
        params.put("timestamp", timestamp);
        String signature = sign(params);
        String body = formEncode(Map.of(
                "api_key", apiKey,
                "public_id", publicId,
                "signature", signature,
                "timestamp", timestamp
        ));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(destroyUrl()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = parseJson(response.body());
            Object result = json.path("result").isMissingNode() ? null : json.path("result").asText();
            if (response.statusCode() < 200 || response.statusCode() >= 300 || result == null || (!"ok".equals(result) && !"not found".equals(result))) {
                throw new BadRequestException(extractErrorMessage(json, "Unable to remove the previous image from Cloudinary."));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to remove the previous image from Cloudinary.");
        } catch (IOException ex) {
            throw new BadRequestException("Unable to remove the previous image from Cloudinary.");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Product image file is required.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Product image must be 10 MB or smaller.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed.");
        }
    }

    private void ensureConfigured() {
        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new BadRequestException("Cloudinary credentials are not configured.");
        }
    }

    private String uploadUrl() {
        return "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";
    }

    private String destroyUrl() {
        return "https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy";
    }

    private HttpRequest.BodyPublisher buildMultipartBody(MultipartFile file, Map<String, String> params, String signature) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeTextPart(output, "api_key", apiKey);
        writeTextPart(output, "folder", params.get("folder"));
        writeTextPart(output, "overwrite", params.get("overwrite"));
        writeTextPart(output, "resource_type", params.get("resource_type"));
        writeTextPart(output, "timestamp", params.get("timestamp"));
        writeTextPart(output, "unique_filename", params.get("unique_filename"));
        writeTextPart(output, "use_filename", params.get("use_filename"));
        writeTextPart(output, "signature", signature);
        writeFilePart(output, file);
        output.write(("--" + boundary() + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArray(output.toByteArray());
    }

    private void writeTextPart(ByteArrayOutputStream output, String name, String value) throws IOException {
        output.write(("--" + boundary() + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeFilePart(ByteArrayOutputStream output, MultipartFile file) throws IOException {
        output.write(("--" + boundary() + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename(file.getOriginalFilename()) + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + (file.getContentType() != null ? file.getContentType() : "application/octet-stream") + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        try (InputStream in = file.getInputStream()) {
            in.transferTo(output);
        }
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String sign(Map<String, String> params) {
        try {
            TreeMap<String, String> sorted = new TreeMap<String, String>();
            sorted.putAll(params);
            StringBuilder payload = new StringBuilder();
            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isBlank()) {
                    continue;
                }
                if (payload.length() > 0) {
                    payload.append('&');
                }
                payload.append(entry.getKey()).append('=').append(entry.getValue());
            }
            payload.append(apiSecret);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 is not available", ex);
        }
    }

    private JsonNode parseJson(String body) throws IOException {
        return objectMapper.readTree(body);
    }

    private String extractErrorMessage(JsonNode body, String fallback) {
        if (body == null) {
            return fallback;
        }
        JsonNode messageNode = body.path("error").path("message");
        if (!messageNode.isMissingNode() && !messageNode.asText().isBlank()) {
            return messageNode.asText();
        }
        JsonNode message = body.path("message");
        if (!message.isMissingNode() && !message.asText().isBlank()) {
            return message.asText();
        }
        return fallback;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.asText().isBlank() ? null : value.asText();
    }

    private String formEncode(Map<String, String> values) {
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (encoded.length() > 0) {
                encoded.append('&');
            }
            encoded.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return encoded.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String boundary() {
        return "----PastryBoundary" + Integer.toHexString(hashCode());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image";
        }
        return filename.replace("\"", "");
    }
}
