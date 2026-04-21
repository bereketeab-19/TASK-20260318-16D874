package com.petsupplies.product.service;

import com.petsupplies.product.domain.Brand;
import com.petsupplies.product.repo.BrandRepository;
import com.petsupplies.product.repo.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BrandService {
  private final BrandRepository brandRepository;
  private final ProductRepository productRepository;

  public BrandService(BrandRepository brandRepository, ProductRepository productRepository) {
    this.brandRepository = brandRepository;
    this.productRepository = productRepository;
  }

  @Transactional(readOnly = true)
  public java.util.List<Brand> list(String merchantId) {
    return brandRepository.findByMerchantIdOrderByNameAsc(merchantId);
  }

  @Transactional
  public Brand create(String merchantId, String name) {
    if (brandRepository.existsByMerchantIdAndName(merchantId, name)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Brand name already exists");
    }
    Brand b = new Brand();
    b.setMerchantId(merchantId);
    b.setName(name);
    return brandRepository.save(b);
  }

  @Transactional(readOnly = true)
  public Brand requireScoped(Long id, String merchantId) {
    return brandRepository.findByIdAndMerchantId(id, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
  }

  @Transactional
  public void deleteIfUnused(String merchantId, Long id) {
    Brand b = requireScoped(id, merchantId);
    if (productRepository.countByBrand_Id(b.getId()) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brand still referenced by products");
    }
    brandRepository.delete(b);
  }
}
