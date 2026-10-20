package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Measurement;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Die Methoden sind synchronized, weil der Simulator und die HTTP-Anfragen
// in verschiedenen Threads laufen und auf dieselben Listen zugreifen.
@Repository
public class MeasurementRepository {

    private static final int MAX_MEASUREMENTS_PER_STATION = 1000;

    private final Map<String, List<Measurement>> measurementsByStation = new HashMap<>();

    public synchronized void save(Measurement measurement) {
        List<Measurement> measurements = measurementsByStation.get(measurement.getStationId());
        if (measurements == null) {
            measurements = new ArrayList<>();
            measurementsByStation.put(measurement.getStationId(), measurements);
        }
        measurements.add(measurement);
        if (measurements.size() > MAX_MEASUREMENTS_PER_STATION) {
            measurements.remove(0);
        }
    }

    public synchronized List<Measurement> findByStationId(String stationId) {
        List<Measurement> measurements = measurementsByStation.get(stationId);
        if (measurements == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(measurements);
    }

    public synchronized Measurement findLatest(String stationId) {
        List<Measurement> measurements = measurementsByStation.get(stationId);
        if (measurements == null || measurements.isEmpty()) {
            return null;
        }
        return measurements.get(measurements.size() - 1);
    }

    public synchronized void deleteAll() {
        measurementsByStation.clear();
    }
}
