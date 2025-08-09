package com.radioip.backend.repository;

import com.radioip.backend.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);

    long countByPublishedTrue(); // ➕ permet de compter uniquement les articles publiés
}
