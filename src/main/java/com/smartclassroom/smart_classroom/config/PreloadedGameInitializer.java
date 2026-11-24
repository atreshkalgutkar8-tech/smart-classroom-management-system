package com.smartclassroom.smart_classroom.config;

import com.smartclassroom.smart_classroom.service.PreloadedGameService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PreloadedGameInitializer implements ApplicationRunner {

    private final PreloadedGameService preloadedGameService;

    public PreloadedGameInitializer(PreloadedGameService preloadedGameService) {
        this.preloadedGameService = preloadedGameService;
    }

    @Override
    public void run(ApplicationArguments args) {
        preloadedGameService.seedIfMissing();
    }
}
