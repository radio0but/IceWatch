package com.radioip.backend.repository;

import com.radioip.backend.model.AppearanceSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppearanceSettingsRepository extends JpaRepository<AppearanceSettings, Long> {
}
