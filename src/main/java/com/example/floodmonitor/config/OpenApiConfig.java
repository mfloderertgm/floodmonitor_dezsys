package com.example.floodmonitor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI floodMonitorOpenApi() {
        Info info = new Info();
        info.setTitle("FloodMonitor API");
        info.setVersion("v1");
        info.setDescription("REST-Schnittstelle des Hochwasser-Frühwarnsystems. "
                + "Alle Endpunkte liefern JSON oder XML, je nach Accept-Header. "
                + "Authentifizierung: keine. Die API bietet nur lesende GET-Endpunkte an, "
                + "deshalb ist kein Login und kein API-Key nötig.");
        return new OpenAPI().info(info);
    }
}
