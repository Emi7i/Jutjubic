package isa.jutjub.repository;

import isa.jutjub.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByPriceLessThanEqual(BigDecimal maxPrice);
    List<Product> findByDeletedFalse();
    
    // Additional methods needed by ProductService
    java.util.Optional<Product> findByIdAndDeletedFalse(Long id);
    List<Product> findByCategoryAndDeletedFalse(String category);
    List<Product> findByNameContainingIgnoreCaseAndDeletedFalse(String name);
    List<Product> findByPriceLessThanEqualAndDeletedFalse(BigDecimal maxPrice);
}
