package com.petsupplies.product.service;

import com.petsupplies.product.domain.Category;
import com.petsupplies.product.repo.CategoryRepository;
import com.petsupplies.product.repo.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryService {
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;

  public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
    this.categoryRepository = categoryRepository;
    this.productRepository = productRepository;
  }

  public record CategoryRow(Long id, String name, int level, Long parentId) {}

  @Transactional(readOnly = true)
  public java.util.List<CategoryRow> listRows(String merchantId) {
    return categoryRepository.findByMerchantIdOrderByNameAsc(merchantId).stream()
        .map(c -> new CategoryRow(
            c.getId(),
            c.getName(),
            c.getLevel(),
            c.getParent() != null ? c.getParent().getId() : null
        ))
        .toList();
  }

  @Transactional
  public Category create(String merchantId, String name, Long parentId) {
    Category c = new Category();
    c.setMerchantId(merchantId);
    c.setName(name);
    if (parentId != null) {
      Category parent = categoryRepository.findByIdAndMerchantId(parentId, merchantId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent category not found"));
      c.setParent(parent);
    }
    return categoryRepository.save(c);
  }

  @Transactional(readOnly = true)
  public Category requireScoped(Long id, String merchantId) {
    return categoryRepository.findByIdAndMerchantId(id, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
  }

  @Transactional
  public void deleteIfEmpty(String merchantId, Long id) {
    Category c = requireScoped(id, merchantId);
    if (productRepository.countByCategory_Id(c.getId()) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category still has products");
    }
    if (categoryRepository.countByParent_Id(c.getId()) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category has child categories");
    }
    categoryRepository.delete(c);
  }
}
