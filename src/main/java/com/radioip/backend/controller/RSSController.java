// === RSSController.java ===
package com.radioip.backend.controller;

import com.radioip.backend.model.Setting;
import com.radioip.backend.repository.SettingRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

@RestController
@RequestMapping("/api/settings")
public class RSSController {

    private final SettingRepository settingRepository;

    @Autowired
    public RSSController(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    // --- RSS Feed URL ---

    @GetMapping("/rss-url")
    public String getRSSFeedUrl(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        Optional<Setting> setting = settingRepository.findByKey("rss-url");
        return setting.map(s -> "{\"rssUrl\": \"" + s.getValue() + "\"}").orElse("{\"rssUrl\": null}");
    }

    @PostMapping("/rss-url")
    public String updateRSSFeedUrl(@RequestBody Setting rssSetting) {
        Optional<Setting> existing = settingRepository.findByKey("rss-url");
        existing.ifPresent(settingRepository::delete);
        settingRepository.save(new Setting("rss-url", rssSetting.getValue()));
        return "{\"status\": \"ok\"}";
    }

    @GetMapping("/rss-url/test")
    public String testRSSFeedUrl() {
        Optional<Setting> setting = settingRepository.findByKey("rss-url");
        if (setting.isPresent()) {
            try {
                URL url = new URL(setting.get().getValue());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(3000);
                conn.connect();
                int code = conn.getResponseCode();
                if (code == 200) return "{\"valid\": true}";
            } catch (Exception e) {
                return "{\"valid\": false}";
            }
        }
        return "{\"valid\": false}";
    }

    // --- RSS Show Images ---

    @GetMapping("/rss-show-images")
    public String getRssShowImages() {
        Optional<Setting> setting = settingRepository.findByKey("rss-show-images");
        return setting.map(s -> "{\"showImages\": " + Boolean.parseBoolean(s.getValue()) + "}")
                .orElse("{\"showImages\": false}");
    }

    @PostMapping("/rss-show-images")
    public String updateRssShowImages(@RequestBody Setting showImagesSetting) {
        Optional<Setting> existing = settingRepository.findByKey("rss-show-images");
        existing.ifPresent(settingRepository::delete);
        settingRepository.save(new Setting("rss-show-images", showImagesSetting.getValue()));
        return "{\"status\": \"ok\"}";
    }
}
