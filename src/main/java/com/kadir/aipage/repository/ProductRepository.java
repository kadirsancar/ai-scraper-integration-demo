package com.kadir.aipage.repository;

import com.kadir.aipage.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
     Optional<Product> findByUrl(String url);

     List<Product> findByNameContainingIgnoreCase(String name);

}
