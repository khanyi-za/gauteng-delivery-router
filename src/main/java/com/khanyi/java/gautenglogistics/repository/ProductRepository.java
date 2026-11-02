package com.khanyi.java.gautenglogistics.repository;

import com.khanyi.java.gautenglogistics.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
