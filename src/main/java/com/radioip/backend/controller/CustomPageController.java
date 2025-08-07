package com.radioip.backend.controller;

import com.radioip.backend.model.CustomPage;
import com.radioip.backend.repository.CustomPageRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/pages")
public class CustomPageController {

    @Autowired
    private CustomPageRepository repository;

    @GetMapping
    public List<CustomPage> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<CustomPage> create(@RequestBody CustomPage page) {
        if (repository.existsBySlug(page.getSlug())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(repository.save(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomPage> update(@PathVariable Long id, @RequestBody CustomPage page) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setTitle(page.getTitle());
                    existing.setSlug(page.getSlug());
                    existing.setHtmlContent(page.getHtmlContent());
                    existing.setEnabled(page.isEnabled());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggle(@PathVariable Long id) {
        Optional<CustomPage> opt = repository.findById(id);
        if (opt.isPresent()) {
            CustomPage page = opt.get();
            page.setEnabled(!page.isEnabled());
            repository.save(page);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
