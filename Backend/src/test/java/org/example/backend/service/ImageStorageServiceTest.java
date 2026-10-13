package org.example.backend.service;

import org.example.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageStorageServiceTest {

    private final ImageStorageService service = new ImageStorageService("demo", "key", "secret", "pastry-products");

    @Test
    void rejectsEmptyFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThrows(BadRequestException.class, () -> service.uploadProductImage(file));
    }

    @Test
    void rejectsNonImageContentTypes() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "plain text".getBytes());

        assertThrows(BadRequestException.class, () -> service.uploadProductImage(file));
    }
}
