package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.Alert;
import com.example.floodmonitor.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "Warnungen")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    @Operation(summary = "Aktuelle Warnungen abrufen (Stationen mit WARNING oder CRITICAL)")
    public List<Alert> getAlerts(@RequestParam(required = false) String warningLevel,
                                 @RequestParam(required = false) String river) {
        return alertService.getAlerts(warningLevel, river);
    }
}
