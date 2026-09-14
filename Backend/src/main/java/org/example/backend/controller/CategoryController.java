package org.example.backend.controller;

import org.example.backend.dto.category.CategoryResponse;
import org.example.backend.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listActiveCategories() {
        return categoryService.getActiveCategories();
    }

    @GetMapping("/categories/{id}")
    public CategoryResponse getActiveCategory(@PathVariable Long id) {
        return categoryService.getActiveCategory(id);
    }
}
