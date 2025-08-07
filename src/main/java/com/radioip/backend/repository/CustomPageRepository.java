package com.radioip.backend.repository;

import com.radioip.backend.model.CustomPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomPageRepository extends JpaRepository<CustomPage, Long> {
    Optional<CustomPage> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
