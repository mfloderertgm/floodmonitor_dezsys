package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Alert;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;

    public AlertService(StationRepository stationRepository, MeasurementRepository measurementRepository) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
    }

    public List<Alert> getAlerts(String warningLevelText, String river) {
        WarningLevel warningLevel = ParameterParser.parseWarningLevel(warningLevelText);

        List<Alert> alerts = new ArrayList<>();
        for (Station station : stationRepository.findAll()) {
            Measurement latest = measurementRepository.findLatest(station.getId());
            if (latest == null) {
                continue;
            }
            if (latest.getWarningLevel() != WarningLevel.WARNING
                    && latest.getWarningLevel() != WarningLevel.CRITICAL) {
                continue;
            }
            if (warningLevel != null && latest.getWarningLevel() != warningLevel) {
                continue;
            }
            if (river != null && !station.getRiver().equalsIgnoreCase(river)) {
                continue;
            }
            alerts.add(new Alert(station.getId(), station.getName(), station.getRiver(),
                    latest.getTimestamp(), latest.getWaterLevel(), latest.getWarningLevel()));
        }
        return alerts;
    }
}
