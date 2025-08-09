package com.radioip.backend.controller;

import com.radioip.backend.model.Article;
import com.radioip.backend.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class ArticleController {

    private final ArticleService service;

    public ArticleController(ArticleService service) {
        this.service = service;
    }

    // === PUBLIC ===
    @GetMapping("/public/articles")
    public List<Article> getPublishedArticles() {
        return service.findAll().stream()
                .filter(Article::isPublished)
                .toList();
    }

    @GetMapping("/public/articles/{slug}")
    public ResponseEntity<Article> getArticleBySlug(@PathVariable String slug) {
        return service.findBySlug(slug)
                .filter(Article::isPublished)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // === ADMIN ===
    @GetMapping("/admin/articles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Article> getAllArticles() {
        return service.findAll();
    }

    @PostMapping("/admin/articles")
    @PreAuthorize("hasRole('ADMIN')")
    public Article createArticle(@RequestBody Article article) {
        return service.save(article);
    }

    @PutMapping("/admin/articles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Article> updateArticle(@PathVariable Long id, @RequestBody Article updated) {
        return service.findById(id)
                .map(existing -> {
                    existing.setTitle(updated.getTitle());
                    existing.setSlug(updated.getSlug());
                    existing.setContent(updated.getContent());
                    existing.setPublished(updated.isPublished());
                    return ResponseEntity.ok(service.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/articles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        if (service.findById(id).isPresent()) {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
