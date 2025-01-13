package com.byrybdyk.lb1.service;

import com.byrybdyk.lb1.model.Coordinates;
import com.byrybdyk.lb1.repository.CoordinatesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CoordinatesService {
    private final CoordinatesRepository coordinatesRepository;

    @Autowired
    public CoordinatesService(CoordinatesRepository coordinatesRepository) {
        this.coordinatesRepository = coordinatesRepository;
    }

    public Coordinates getOrCreateCoordinates(Long coordinatesId, Coordinates coordinatesData) {
        if (coordinatesId != null) {
            Coordinates existingCoordinates = findById(coordinatesId);
            if (existingCoordinates != null) {
                return existingCoordinates;
            }
            throw new IllegalArgumentException("Coordinates with specified ID not found");
        } else if (coordinatesData.getId() > 0) {
            Coordinates existingCoordinates2 = findById(coordinatesData.getId());
            if (existingCoordinates2 != null) {
                return existingCoordinates2;
            }
        } else if (coordinatesData != null) {
            return saveCoordinates(coordinatesData);
        }
        else  {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }
        throw new IllegalArgumentException("Coordinates cannot be null");
    }

    public Coordinates findById(Long coordinatesId) {
        Optional<Coordinates> coordinatesOptional = coordinatesRepository.findById(coordinatesId);
        return coordinatesOptional.orElse(null);
    }

    public Coordinates saveCoordinates(Coordinates coordinates) {
        return coordinatesRepository.save(coordinates);
    }

    public List<Coordinates> findAll() {
        return coordinatesRepository.findAll();
    }

    public Coordinates getOrCreateCoordinatesFromRow(Map<String, String> row) {
        Coordinates coordinates = new Coordinates();
        Long id= null;
        try{
            String idStr = row.get("coordinates_id");
            if (idStr != null && idStr.contains(".")) {
                idStr = idStr.split("\\.")[0];
            }
            id = Long.parseLong(idStr);
        }catch (Exception e){
            coordinates.setX(Float.parseFloat(row.get("coordinates_x")));
            String yStr = row.get("coordinates_y");
            if (yStr != null && yStr.contains(".")) {
                yStr = yStr.split("\\.")[0];
            }
            coordinates.setY(Integer.parseInt(yStr));
        }

        return getOrCreateCoordinates(id, coordinates);
    }
}
