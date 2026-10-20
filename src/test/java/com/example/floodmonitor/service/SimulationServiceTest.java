package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationServiceTest {

    private final StationRepository stationRepository = new StationRepository();
    private final MeasurementRepository measurementRepository = new MeasurementRepository();

    private SimulationService createSimulation(boolean enabled) {
        return new SimulationService(stationRepository, measurementRepository, new WarningLevelService(), enabled);
    }

    // Die Tests prüfen nur Grenzen, die bei jedem Zufallswert gelten müssen.
    @Test
    void valuesStayValidAndChangeSlowly() {
        SimulationService simulation = createSimulation(true);
        Station station = stationRepository.findById("ST-001");

        Measurement previous = simulation.createNextMeasurement(station, null);
        for (int i = 0; i < 2000; i++) {
            Measurement next = simulation.createNextMeasurement(station, previous);

            assertTrue(next.getWaterLevel() >= 0);
            assertTrue(next.getRainfall() >= 0 && next.getRainfall() <= 30);
            assertTrue(next.getBatteryLevel() >= 0 && next.getBatteryLevel() <= 100);
            assertTrue(Math.abs(next.getWaterLevel() - previous.getWaterLevel()) < 20);
            assertNotNull(next.getWarningLevel());

            previous = next;
        }
    }

    @Test
    void higherWaterLevelMeansHigherFlowRate() {
        SimulationService simulation = createSimulation(true);
        Station station = stationRepository.findById("ST-001");

        Measurement low = new Measurement();
        low.setWaterLevel(200);
        low.setStatus(StationStatus.ONLINE);
        Measurement high = new Measurement();
        high.setWaterLevel(500);
        high.setStatus(StationStatus.ONLINE);

        Measurement afterLow = simulation.createNextMeasurement(station, low);
        Measurement afterHigh = simulation.createNextMeasurement(station, high);

        assertTrue(afterHigh.getFlowRate() > afterLow.getFlowRate());
    }

    @Test
    void lowBatteryLeadsToMaintenance() {
        SimulationService simulation = createSimulation(true);
        Station station = stationRepository.findById("ST-001");

        Measurement previous = new Measurement();
        previous.setWaterLevel(250);
        previous.setBatteryLevel(10);
        previous.setStatus(StationStatus.ONLINE);

        Measurement next = simulation.createNextMeasurement(station, previous);
        assertEquals(StationStatus.MAINTENANCE, next.getStatus());

        Measurement afterMaintenance = simulation.createNextMeasurement(station, next);
        assertEquals(StationStatus.ONLINE, afterMaintenance.getStatus());
        assertEquals(100, afterMaintenance.getBatteryLevel());
    }

    @Test
    void inactiveStationGetsNoMeasurements() {
        createSimulation(true).simulateAllStations();

        assertNotNull(measurementRepository.findLatest("ST-001"));
        assertNull(measurementRepository.findLatest("ST-004"));
    }

    @Test
    void disabledSimulationCreatesNothing() {
        createSimulation(false).simulateAllStations();

        assertNull(measurementRepository.findLatest("ST-001"));
    }
}
