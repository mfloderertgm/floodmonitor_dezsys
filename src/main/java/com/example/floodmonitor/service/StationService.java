package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.ResourceNotFoundException;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StationService {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;

    public StationService(StationRepository stationRepository, MeasurementRepository measurementRepository) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
    }

    public Station getStation(String stationId) {
        Station station = stationRepository.findById(stationId);
        if (station == null) {
            throw new ResourceNotFoundException("Station " + stationId + " wurde nicht gefunden");
        }
        return station;
    }

    public List<Station> getStations(String river, Boolean isActive, String warningLevelText) {
        WarningLevel warningLevel = ParameterParser.parseWarningLevel(warningLevelText);

        List<Station> result = new ArrayList<>();
        for (Station station : stationRepository.findAll()) {
            if (river != null && !station.getRiver().equalsIgnoreCase(river)) {
                continue;
            }
            if (isActive != null && station.isActive() != isActive) {
                continue;
            }
            if (warningLevel != null && getCurrentWarningLevel(station) != warningLevel) {
                continue;
            }
            result.add(station);
        }
        return result;
    }

    public WarningLevel getCurrentWarningLevel(Station station) {
        Measurement latest = measurementRepository.findLatest(station.getId());
        if (latest == null) {
            return WarningLevel.UNKNOWN;
        }
        return latest.getWarningLevel();
    }
}
