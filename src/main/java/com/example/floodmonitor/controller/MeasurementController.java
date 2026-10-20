package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Statistics;
import com.example.floodmonitor.service.MeasurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/stations/{stationId}",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "Messwerte")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping("/measurements/latest")
    @Operation(summary = "Aktuellsten Messwert einer Station abrufen")
    public Measurement getLatest(@PathVariable String stationId) {
        return measurementService.getLatest(stationId);
    }

    @GetMapping("/measurements")
    @Operation(summary = "Historische Messwerte abrufen, neueste zuerst",
            description = "from und to im Format ISO-8601 (z.B. 2026-05-01T08:00:00Z), limit von 1 bis 1000")
    public List<Measurement> getHistory(@PathVariable String stationId,
                                        @RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to,
                                        @RequestParam(required = false) Integer limit,
                                        @RequestParam(required = false) String warningLevel) {
        return measurementService.getHistory(stationId, from, to, limit, warningLevel);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Statistik über die Messwerte einer Station")
    public Statistics getStatistics(@PathVariable String stationId,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to) {
        return measurementService.getStatistics(stationId, from, to);
    }
}
