package com.example.floodmonitor;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest(properties = "simulation.enabled=false")
@AutoConfigureMockMvc
class FloodMonitorApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeasurementRepository measurementRepository;

    @BeforeEach
    void createTestMeasurements() {
        measurementRepository.deleteAll();
        saveMeasurement("2026-05-01T08:00:00Z", 300, WarningLevel.NORMAL);
        saveMeasurement("2026-05-01T10:00:00Z", 420, WarningLevel.WARNING);
        saveMeasurement("2026-05-01T12:00:00Z", 560, WarningLevel.CRITICAL);
    }

    private void saveMeasurement(String timestamp, double waterLevel, WarningLevel warningLevel) {
        measurementRepository.save(new Measurement("ST-001", Instant.parse(timestamp), waterLevel,
                waterLevel * 2.3, 5.0, 14.0, 90, StationStatus.ONLINE, warningLevel));
    }

    @Test
    void allStationsAsJson() throws Exception {
        mockMvc.perform(get("/api/v1/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value("ST-001"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void allStationsAsXml() throws Exception {
        mockMvc.perform(get("/api/v1/stations").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/List/item").nodeCount(4))
                .andExpect(xpath("/List/item[1]/id").string("ST-001"));
    }

    @Test
    void jsonIsDefaultWithoutAcceptHeader() throws Exception {
        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void existingStation() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ST-002"))
                .andExpect(jsonPath("$.river").value("Donau"))
                .andExpect(jsonPath("$.location.latitude").value(48.2286));
    }

    @Test
    void unknownStation() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Station ST-999 wurde nicht gefunden"))
                .andExpect(jsonPath("$.path").value("/api/v1/stations/ST-999"));
    }

    @Test
    void unknownStationAsXml() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-999").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotFound())
                .andExpect(xpath("/error/status").string("404"));
    }

    @Test
    void latestMeasurement() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("ST-001"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-01T12:00:00Z"))
                .andExpect(jsonPath("$.waterLevel").value(560.0))
                .andExpect(jsonPath("$.warningLevel").value("CRITICAL"));
    }

    @Test
    void latestMeasurementMissing() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-002/measurements/latest"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filterByTimeRange() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements")
                        .param("from", "2026-05-01T09:00:00Z")
                        .param("to", "2026-05-01T11:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].timestamp").value("2026-05-01T10:00:00Z"));
    }

    @Test
    void filterByWarningLevel() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements").param("warningLevel", "WARNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].waterLevel").value(420.0));
    }

    @Test
    void fromAfterToIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements")
                        .param("from", "2026-05-01T12:00:00Z")
                        .param("to", "2026-05-01T08:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidDateFormatIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements").param("from", "01.05.2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidWarningLevelIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/stations").param("warningLevel", "HIGH"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedFormat() throws Exception {
        mockMvc.perform(get("/api/v1/stations").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.status").value(406));
    }

    @Test
    void limitReturnsNewestMeasurements() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].timestamp").value("2026-05-01T12:00:00Z"));
    }

    @Test
    void maximumLimitIsAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "1000"))
                .andExpect(status().isOk());
    }

    @Test
    void limitAboveMaximumIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "1001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeLimitIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void limitThatIsNotANumberIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stationsFilteredByRiverAndWarningLevel() throws Exception {
        mockMvc.perform(get("/api/v1/stations").param("river", "Donau"))
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/stations").param("isActive", "false"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("ST-004"));

        mockMvc.perform(get("/api/v1/stations").param("warningLevel", "CRITICAL"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("ST-001"));
    }

    @Test
    void alerts() throws Exception {
        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stationId").value("ST-001"))
                .andExpect(jsonPath("$[0].warningLevel").value("CRITICAL"));

        mockMvc.perform(get("/api/v1/alerts").param("river", "Salzach"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void statistics() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurementCount").value(3))
                .andExpect(jsonPath("$.minWaterLevel").value(300.0))
                .andExpect(jsonPath("$.maxWaterLevel").value(560.0))
                .andExpect(jsonPath("$.averageWaterLevel").value(426.7))
                .andExpect(jsonPath("$.rainfallSum").value(15.0))
                .andExpect(jsonPath("$.warningCount").value(1))
                .andExpect(jsonPath("$.criticalCount").value(1));
    }
}
