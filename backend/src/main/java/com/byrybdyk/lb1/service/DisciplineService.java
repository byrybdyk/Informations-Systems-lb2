package com.byrybdyk.lb1.service;

import com.byrybdyk.lb1.model.Discipline;
import com.byrybdyk.lb1.repository.DisciplineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;

    @Autowired
    public DisciplineService(DisciplineRepository disciplineRepository) {
        this.disciplineRepository = disciplineRepository;
    }


    public Discipline findById(Long id) {
        return disciplineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discipline with ID " + id + " not found"));
    }

    public Discipline getOrCreateDiscipline(Discipline disciplineData) {
        if (disciplineData.getId() != null && disciplineData.getId() > 0) {
            Discipline existingDiscipline = findById(disciplineData.getId());
            if (existingDiscipline != null) {
                return existingDiscipline;
            }
        }
        if (disciplineData.getName() == null || disciplineData.getName().isEmpty()) {
            throw new IllegalArgumentException("Discipline name cannot be null or empty");
        }
        Discipline newDiscipline = new Discipline();
        newDiscipline.setName(disciplineData.getName());
        newDiscipline.setPracticeHours(disciplineData.getPracticeHours());
        return save(newDiscipline);
    }


    public Discipline save(Discipline discipline) {
        return disciplineRepository.save(discipline);
    }

    public Discipline update(Long id, Discipline updatedDiscipline) {
        Discipline existingDiscipline = findById(id);
        existingDiscipline.setName(updatedDiscipline.getName());
        existingDiscipline.setPracticeHours(updatedDiscipline.getPracticeHours());
        return disciplineRepository.save(existingDiscipline);
    }

    public void delete(Long id) {
        disciplineRepository.deleteById(id);
    }

    public List<Discipline> findAll() {
        return disciplineRepository.findAll();
    }

    public Discipline getOrCreateDisciplineFromRow(Map<String, String> row) {
        Discipline discipline = new Discipline();
        Long id = null;
        try{
            String IdStr = row.get("discipline_id");
            if (IdStr != null && IdStr.contains(".")) {
                IdStr = IdStr.split("\\.")[0];
            }
            id = Long.parseLong(IdStr);
            discipline.setId(id);
        }catch (Exception e){

            discipline.setName(row.get("discipline_name"));
            String discipline_PH = row.get("discipline_practice_hours");
            if (discipline_PH != null && discipline_PH.contains(".")) {
                discipline_PH = discipline_PH.split("\\.")[0];
            }
            discipline.setPracticeHours(Integer.parseInt(discipline_PH));
        }

        return getOrCreateDiscipline(discipline);
    }
}
