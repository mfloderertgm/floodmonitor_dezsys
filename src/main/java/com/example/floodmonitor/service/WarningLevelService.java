package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import org.springframework.stereotype.Service;

@Service
public class WarningLevelService {

    public WarningLevel calculate(Station station, Measurement measurement) {
        if (measurement == null || measurement.getWaterLevel() < 0) {
            return WarningLevel.UNKNOWN;
        }
        if (measurement.getWaterLevel() >= station.getCriticalWaterLevel()) {
            return WarningLevel.CRITICAL;
        }
        if (measurement.getWaterLevel() >= station.getWarningWaterLevel()) {
            return WarningLevel.WARNING;
        }
        return WarningLevel.NORMAL;
    }
}
