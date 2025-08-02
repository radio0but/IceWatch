package com.radioip.backend.service;

import com.radioip.backend.model.SlotInfo;
import com.radioip.backend.repository.SlotInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmissionService {

    @Autowired
    private SlotInfoRepository repository;

    public Optional<SlotInfo> getEmission(String day, String hour) {
        return repository.findByDayAndHour(day, hour);
    }

    public void setEmission(String day, String hour, String audio, String video) {
        SlotInfo slot = repository.findByDayAndHour(day, hour).orElseGet(() -> {
            SlotInfo s = new SlotInfo();
            s.setDay(day);
            s.setHour(hour);
            return s;
        });

        if (audio != null) slot.setAudioInfo(audio);
        if (video != null) slot.setVideoInfo(video);

        repository.save(slot);
    }
}
