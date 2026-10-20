package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Service
public class SimulationService {

    private static final double MAX_RAINFALL = 30;
    private static final double RAIN_EFFECT = 0.5;
    private static final double DRAIN_RATE = 0.05;
    private static final double FLOW_PER_CM = 2.3;
    private static final int LOW_BATTERY = 15;

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final WarningLevelService warningLevelService;
    private final boolean enabled;
    private final Random random = new Random();

    public SimulationService(StationRepository stationRepository,
                             MeasurementRepository measurementRepository,
                             WarningLevelService warningLevelService,
                             @Value("${simulation.enabled:true}") boolean enabled) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.warningLevelService = warningLevelService;
        this.enabled = enabled;
    }

    @Scheduled(fixedRateString = "${simulation.interval:10000}")
    public void simulateAllStations() {
        if (!enabled) {
            return;
        }
        for (Station station : stationRepository.findAll()) {
            if (!station.isActive() || station.getStatus() == StationStatus.OFFLINE) {
                continue;
            }
            Measurement previous = measurementRepository.findLatest(station.getId());
            Measurement next = createNextMeasurement(station, previous);
            station.setStatus(next.getStatus());
            measurementRepository.save(next);
        }
    }

    public Measurement createNextMeasurement(Station station, Measurement previous) {
        if (previous == null) {
            previous = createStartMeasurement(station);
        }

        double rainfall = previous.getRainfall() + randomBetween(-2, 2);
        rainfall = limit(rainfall, 0, MAX_RAINFALL);

        // Regen lässt den Pegel steigen, gleichzeitig fließt umso mehr ab,
        // je weiter der Pegel über dem Normalwert liegt.
        double rise = rainfall * RAIN_EFFECT;
        double drain = (previous.getWaterLevel() - station.getNormalWaterLevel()) * DRAIN_RATE;
        double waterLevel = previous.getWaterLevel() + rise - drain + randomBetween(-2, 2);
        waterLevel = Math.max(waterLevel, 0);

        double flowRate = waterLevel * FLOW_PER_CM;

        double temperature = previous.getTemperature() + randomBetween(-0.3, 0.3);
        temperature = limit(temperature, -10, 35);

        int batteryLevel;
        StationStatus status;
        if (previous.getStatus() == StationStatus.MAINTENANCE) {
            batteryLevel = 100;
            status = StationStatus.ONLINE;
        } else {
            batteryLevel = Math.max(previous.getBatteryLevel() - random.nextInt(2), 0);
            status = StationStatus.ONLINE;
            if (batteryLevel < LOW_BATTERY) {
                status = StationStatus.MAINTENANCE;
            }
        }

        Measurement next = new Measurement();
        next.setStationId(station.getId());
        next.setTimestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        next.setWaterLevel(roundToOneDecimal(waterLevel));
        next.setFlowRate(roundToOneDecimal(flowRate));
        next.setRainfall(roundToOneDecimal(rainfall));
        next.setTemperature(roundToOneDecimal(temperature));
        next.setBatteryLevel(batteryLevel);
        next.setStatus(status);
        next.setWarningLevel(warningLevelService.calculate(station, next));
        return next;
    }

    private Measurement createStartMeasurement(Station station) {
        Measurement start = new Measurement();
        start.setWaterLevel(station.getNormalWaterLevel());
        start.setRainfall(2);
        start.setTemperature(15);
        start.setBatteryLevel(100);
        start.setStatus(StationStatus.ONLINE);
        return start;
    }

    private double randomBetween(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private double limit(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
