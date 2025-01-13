package com.byrybdyk.lb1.service;

import com.byrybdyk.lb1.model.Location;
import com.byrybdyk.lb1.repository.LocationRepository;
import org.postgresql.largeobject.LargeObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    @Autowired
    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public List<Location> findAll() {
        return locationRepository.findAll();
    }

    public Location getOrCreateLocation(Long id, Location locationData) {
        if (id != null) {
            Location existingLocation = findById(id);
            if (existingLocation != null) {
                return existingLocation;
            }
            throw new IllegalArgumentException("Location with specified ID not found");
        }

        if (locationData != null && locationData.getId() > 0) {
            Location existingLocation = findById(locationData.getId());
            if (existingLocation != null) {
                return existingLocation;
            }
            return saveLocation(locationData);
        }

        if (locationData != null) {
            return saveLocation(locationData);
        }

        throw new IllegalArgumentException("Invalid location data");
    }

    private Location saveLocation(Location locationData) {
        return locationRepository.save(locationData);
    }

    private Location findById(Long id) {
        return locationRepository.findById(id)
                .orElse(null);
    }

    public Location getOrCreateLocationFromRow(Map<String, String> row) {
        Long id = null;
        Location location = new Location();
        String IdStr = row.get("location_id");
        try{
            if (IdStr != null && IdStr.contains(".")) {
                IdStr = IdStr.split("\\.")[0];
            }
            id = Long.parseLong(IdStr);
        }catch (Exception e){
            location.setX(Float.parseFloat(row.get("location_x")));
            location.setY(Float.parseFloat(row.get("location_y")));
            location.setName(row.get("location_name"));
        }

        return getOrCreateLocation(id, location);
    }
}
