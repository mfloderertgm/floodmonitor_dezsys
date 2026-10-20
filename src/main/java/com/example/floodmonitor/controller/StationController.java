package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.service.StationService;
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
@RequestMapping(value = "/api/v1", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "Stationen")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/stations")
    @Operation(summary = "Alle Messstationen abrufen, optional gefiltert")
    public List<Station> getStations(@RequestParam(required = false) String river,
                                     @RequestParam(required = false) Boolean isActive,
                                     @RequestParam(required = false) String warningLevel) {
        return stationService.getStations(river, isActive, warningLevel);
    }

    @GetMapping("/stations/{stationId}")
    @Operation(summary = "Einzelne Station abrufen")
    public Station getStation(@PathVariable String stationId) {
        return stationService.getStation(stationId);
    }
}
