package com.radioip.backend.service;

import com.radioip.backend.model.Article;
import com.radioip.backend.repository.ArticleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ArticleService {

    private final ArticleRepository repository;

    public ArticleService(ArticleRepository repository) {
        this.repository = repository;
    }

    public List<Article> findAll() {
        return repository.findAll();
    }

    public Optional<Article> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Article> findBySlug(String slug) {
        return repository.findBySlug(slug);
    }

    public Article save(Article article) {
        return repository.save(article);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
