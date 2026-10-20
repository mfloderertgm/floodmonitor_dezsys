package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class StationRepository {

    private final List<Station> stations = new ArrayList<>();

    public StationRepository() {
        stations.add(new Station("ST-001", "Donaubrücke Linz", "Donau",
                new Location(48.3069, 14.2858), 250, 400, 550, true, StationStatus.ONLINE));
        stations.add(new Station("ST-002", "Reichsbrücke Wien", "Donau",
                new Location(48.2286, 16.4106), 300, 450, 600, true, StationStatus.ONLINE));
        stations.add(new Station("ST-003", "Staatsbrücke Salzburg", "Salzach",
                new Location(47.8009, 13.0447), 180, 300, 420, true, StationStatus.ONLINE));
        stations.add(new Station("ST-004", "Innbrücke Innsbruck", "Inn",
                new Location(47.2692, 11.3914), 200, 330, 450, false, StationStatus.OFFLINE));
    }

    public List<Station> findAll() {
        return new ArrayList<>(stations);
    }

    public Station findById(String id) {
        for (Station station : stations) {
            if (station.getId().equals(id)) {
                return station;
            }
        }
        return null;
    }
}
