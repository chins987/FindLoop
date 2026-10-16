package com.findloop.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
public class RuntimeConfigController {

    private final ObjectMapper objectMapper;

    @Value("${SUPABASE_URL:}")
    private String supabaseUrl;

    @Value("${SUPABASE_PUBLISHABLE_KEY:}")
    private String supabasePublishableKey;

    public RuntimeConfigController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/config.js", produces = "application/javascript")
    public ResponseEntity<String> runtimeConfig() throws JsonProcessingException {
        String script = "window.__FINDLOOP_CONFIG__ = { supabaseUrl: "
                + objectMapper.writeValueAsString(supabaseUrl)
                + ", supabasePublishableKey: "
                + objectMapper.writeValueAsString(supabasePublishableKey)
                + " };";

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("application/javascript"))
                .body(script);
    }
}
