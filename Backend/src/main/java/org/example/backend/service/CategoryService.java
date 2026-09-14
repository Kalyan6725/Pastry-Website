package org.example.backend.service;

import org.example.backend.dto.category.CategoryRequest;
import org.example.backend.dto.category.CategoryResponse;
import org.example.backend.entity.Category;
import org.example.backend.exception.DuplicateResourceException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new DuplicateResourceException("Category already exists");
        }
        Category category = Category.builder().name(name).description(request.description()).active(true).build();
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        String name = request.name().trim();
        if (!category.getName().equalsIgnoreCase(name) && categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new DuplicateResourceException("Category already exists");
        }
        category.setName(name);
        category.setDescription(request.description());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse setCategoryActive(Long id, boolean active) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setActive(active);
        return toResponse(categoryRepository.save(category));
    }

    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    public CategoryResponse getActiveCategory(Long id) {
        Category category = categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(category);
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(category);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(), category.isActive());
    }
}
