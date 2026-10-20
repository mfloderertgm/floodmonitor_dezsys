package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.BadRequestException;
import com.example.floodmonitor.exception.ResourceNotFoundException;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Statistics;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class MeasurementService {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 1000;

    private final StationService stationService;
    private final MeasurementRepository measurementRepository;

    public MeasurementService(StationService stationService, MeasurementRepository measurementRepository) {
        this.stationService = stationService;
        this.measurementRepository = measurementRepository;
    }

    public Measurement getLatest(String stationId) {
        stationService.getStation(stationId);

        Measurement latest = measurementRepository.findLatest(stationId);
        if (latest == null) {
            throw new ResourceNotFoundException("Für Station " + stationId + " gibt es noch keine Messung");
        }
        return latest;
    }

    public List<Measurement> getHistory(String stationId, String fromText, String toText,
                                        Integer limit, String warningLevelText) {
        stationService.getStation(stationId);

        WarningLevel warningLevel = ParameterParser.parseWarningLevel(warningLevelText);
        if (limit == null) {
            limit = DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BadRequestException("'limit' muss zwischen 1 und " + MAX_LIMIT + " liegen");
        }

        List<Measurement> inTimeRange = findInTimeRange(stationId, fromText, toText);

        List<Measurement> result = new ArrayList<>();
        for (int i = inTimeRange.size() - 1; i >= 0; i--) {
            Measurement measurement = inTimeRange.get(i);
            if (warningLevel != null && measurement.getWarningLevel() != warningLevel) {
                continue;
            }
            result.add(measurement);
            if (result.size() == limit) {
                break;
            }
        }
        return result;
    }

    public Statistics getStatistics(String stationId, String fromText, String toText) {
        stationService.getStation(stationId);

        List<Measurement> measurements = findInTimeRange(stationId, fromText, toText);

        Statistics statistics = new Statistics();
        statistics.setStationId(stationId);
        statistics.setMeasurementCount(measurements.size());
        if (measurements.isEmpty()) {
            return statistics;
        }

        double min = measurements.get(0).getWaterLevel();
        double max = measurements.get(0).getWaterLevel();
        double waterLevelSum = 0;
        double flowRateSum = 0;
        double rainfallSum = 0;
        int warningCount = 0;
        int criticalCount = 0;

        for (Measurement measurement : measurements) {
            if (measurement.getWaterLevel() < min) {
                min = measurement.getWaterLevel();
            }
            if (measurement.getWaterLevel() > max) {
                max = measurement.getWaterLevel();
            }
            waterLevelSum += measurement.getWaterLevel();
            flowRateSum += measurement.getFlowRate();
            rainfallSum += measurement.getRainfall();
            if (measurement.getWarningLevel() == WarningLevel.WARNING) {
                warningCount++;
            }
            if (measurement.getWarningLevel() == WarningLevel.CRITICAL) {
                criticalCount++;
            }
        }

        statistics.setMinWaterLevel(min);
        statistics.setMaxWaterLevel(max);
        statistics.setAverageWaterLevel(roundToOneDecimal(waterLevelSum / measurements.size()));
        statistics.setAverageFlowRate(roundToOneDecimal(flowRateSum / measurements.size()));
        statistics.setRainfallSum(roundToOneDecimal(rainfallSum));
        statistics.setWarningCount(warningCount);
        statistics.setCriticalCount(criticalCount);
        return statistics;
    }

    private List<Measurement> findInTimeRange(String stationId, String fromText, String toText) {
        Instant from = ParameterParser.parseTimestamp("from", fromText);
        Instant to = ParameterParser.parseTimestamp("to", toText);
        ParameterParser.checkTimeRange(from, to);

        List<Measurement> result = new ArrayList<>();
        for (Measurement measurement : measurementRepository.findByStationId(stationId)) {
            if (from != null && measurement.getTimestamp().isBefore(from)) {
                continue;
            }
            if (to != null && measurement.getTimestamp().isAfter(to)) {
                continue;
            }
            result.add(measurement);
        }
        return result;
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
