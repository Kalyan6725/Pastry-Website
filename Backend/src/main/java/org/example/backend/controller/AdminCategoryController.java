package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.category.CategoryRequest;
import org.example.backend.dto.category.CategoryResponse;
import org.example.backend.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @PatchMapping("/categories/{id}/active")
    public CategoryResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return categoryService.setCategoryActive(id, active);
    }
}
