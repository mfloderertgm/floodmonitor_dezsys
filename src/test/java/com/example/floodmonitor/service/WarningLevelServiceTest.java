package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WarningLevelServiceTest {

    private final WarningLevelService warningLevelService = new WarningLevelService();

    private final Station station = new Station("ST-001", "Teststation", "Donau",
            new Location(48.0, 14.0), 250, 400, 550, true, StationStatus.ONLINE);

    private Measurement measurementWithWaterLevel(double waterLevel) {
        Measurement measurement = new Measurement();
        measurement.setWaterLevel(waterLevel);
        return measurement;
    }

    @Test
    void belowWarningValueIsNormal() {
        assertEquals(WarningLevel.NORMAL, warningLevelService.calculate(station, measurementWithWaterLevel(399.9)));
    }

    @Test
    void fromWarningValueIsWarning() {
        assertEquals(WarningLevel.WARNING, warningLevelService.calculate(station, measurementWithWaterLevel(400)));
        assertEquals(WarningLevel.WARNING, warningLevelService.calculate(station, measurementWithWaterLevel(549.9)));
    }

    @Test
    void fromCriticalValueIsCritical() {
        assertEquals(WarningLevel.CRITICAL, warningLevelService.calculate(station, measurementWithWaterLevel(550)));
        assertEquals(WarningLevel.CRITICAL, warningLevelService.calculate(station, measurementWithWaterLevel(700)));
    }

    @Test
    void missingOrInvalidMeasurementIsUnknown() {
        assertEquals(WarningLevel.UNKNOWN, warningLevelService.calculate(station, null));
        assertEquals(WarningLevel.UNKNOWN, warningLevelService.calculate(station, measurementWithWaterLevel(-1)));
    }
}
