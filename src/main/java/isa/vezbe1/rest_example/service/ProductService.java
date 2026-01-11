package com.example.springcombined.service;

import com.example.springcombined.exception.ResourceNotFoundException;
import com.example.springcombined.model.Product;
import com.example.springcombined.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findByDeletedFalse();
    }

    public Product getProductById(Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndDeletedFalse(category);
    }

    @Transactional
    public Product createProduct(Product product) {
        validateProduct(product);
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);
        
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setQuantity(productDetails.getQuantity());
        product.setCategory(productDetails.getCategory());
        
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setDeleted(true);
        productRepository.save(product);
    }

    public List<Product> searchProducts(String name, BigDecimal maxPrice, String category) {
        if (name != null && !name.isEmpty()) {
            return productRepository.findByNameContainingIgnoreCaseAndDeletedFalse(name);
        } else if (maxPrice != null) {
            return productRepository.findByPriceLessThanEqualAndDeletedFalse(maxPrice);
        } else if (category != null && !category.isEmpty()) {
            return getProductsByCategory(category);
        }
        return getAllProducts();
    }

    private void validateProduct(Product product) {
        if (product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        if (product.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
}
