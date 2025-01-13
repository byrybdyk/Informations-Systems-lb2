package com.byrybdyk.lb1.service;

import com.byrybdyk.lb1.dto.LabWorkDTO;
import com.byrybdyk.lb1.model.*;
import com.byrybdyk.lb1.model.enums.ChangeType;
import com.byrybdyk.lb1.model.enums.Difficulty;
import com.byrybdyk.lb1.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LabWorkService {

    private final LabWorkRepository labWorkRepository;
    private final LabWorkHistoryRepository labWorkHistoryRepository;
    private final DisciplineService disciplineService;
    private final PersonService authorService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CoordinatesRepository coordinatesRepository;
    private final CoordinatesService coordinatesService;
    private final PersonRepository personRepository;

    @Autowired
    public LabWorkService(LabWorkRepository labWorkRepository, LabWorkHistoryRepository labWorkHistoryRepository, DisciplineService disciplineService, PersonService authorService, UserRepository userRepository, UserService userService, CoordinatesRepository coordinatesRepository, CoordinatesService coordinatesService, PersonRepository personRepository) {
        this.labWorkRepository = labWorkRepository;
        this.labWorkHistoryRepository = labWorkHistoryRepository;
        this.disciplineService = disciplineService;
        this.authorService = authorService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.coordinatesRepository = coordinatesRepository;
        this.coordinatesService = coordinatesService;
        this.personRepository = personRepository;
    }

    public LabWork saveLabWork(LabWork labWork) {
        LabWork savedLabWork = labWorkRepository.save(labWork);
        return savedLabWork;
    }

    public List<LabWork> getAllLabWorks() {
        return labWorkRepository.findAll();
    }

    private void mapDtoToLabWork(LabWork labWork, LabWorkDTO labWorkDTO, Person author, User owner) {
        labWork.setName(labWorkDTO.getName());
        labWork.setDescription(labWorkDTO.getDescription());
        labWork.setDifficulty(labWorkDTO.getDifficulty());
        labWork.setMinimalPoint(labWorkDTO.getMinimalPoint());
        labWork.setPersonalQualitiesMinimum(labWorkDTO.getPersonalQualitiesMinimum());
        labWork.setPersonalQualitiesMaximum(labWorkDTO.getPersonalQualitiesMaximum());
        labWork.setAuthor(author);
        labWork.setOwner(owner);
    }

    public LabWork createLabWorkFromDTO(LabWorkDTO labWorkDTO, Authentication authentication) {
        try {
            LabWork labWork = new LabWork();


//
            Person author = authorService.getOrCreateAuthor(labWorkDTO.getAuthorId() , labWorkDTO.getAuthor());
            labWork.setAuthor(author);

            Discipline discipline = disciplineService.getOrCreateDiscipline(labWorkDTO.getDiscipline());
            labWork.setDiscipline(discipline);

//            System.out.println("CoordinatesId " + labWorkDTO.getCoordinates().getId() + " CoordinatesId " + labWorkDTO.getCoordinatesId());
            Coordinates coordinates = coordinatesService.getOrCreateCoordinates(labWorkDTO.getCoordinatesId(), labWorkDTO.getCoordinates());
            labWork.setCoordinates(coordinates);

            String userName = labWorkDTO.getOwnerName();
            User owner = userRepository.findByUsername(userName)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            mapDtoToLabWork(labWork, labWorkDTO, author, owner);
            LabWork savedLabWork = saveLabWork(labWork);


            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String currentUser = oauth2User.getAttribute("preferred_username");

            saveLabWorkHistory(savedLabWork, ChangeType.CREATE, currentUser);

            return savedLabWork;
        } catch (Exception e) {
            System.out.println("Error while creating LabWork: " + e.getMessage());
            throw e;
        }
    }

    public LabWork updateLabWorkFromDTO(LabWorkDTO labWorkDTO, String currentUserName,Authentication authentication) {
        try {
            Optional<LabWork> existingLabWorkOpt = labWorkRepository.findById(labWorkDTO.getId());

            if (!existingLabWorkOpt.isPresent()) {
                throw new IllegalArgumentException("LabWork not found for ID: " + labWorkDTO.getId());
            }

            LabWork labWork = existingLabWorkOpt.get();

            Person author = authorService.getOrCreateAuthor(labWorkDTO.getAuthorId(), labWorkDTO.getAuthor());
            labWork.setAuthor(author);

            Discipline discipline = disciplineService.getOrCreateDiscipline(labWorkDTO.getDiscipline());
            labWork.setDiscipline(discipline);

            Coordinates coordinates = coordinatesService.getOrCreateCoordinates(labWorkDTO.getCoordinatesId(), labWorkDTO.getCoordinates());
            labWork.setCoordinates(coordinates);

            String userName = labWorkDTO.getOwnerName();
            System.out.println("username: " + userName);
            User owner = userRepository.findByUsername(userName)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            mapDtoToLabWork(labWork, labWorkDTO, author, owner);

            LabWork updatedLabWork = saveLabWork(labWork);


            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String currentUser = oauth2User.getAttribute("preferred_username");

            saveLabWorkHistory(updatedLabWork, ChangeType.UPDATE, currentUser);

            return updatedLabWork;
        } catch (Exception e) {
            System.out.println("Error while updating LabWork: " + e.getMessage());
            throw e;
        }
    }

    public void deleteLabWork(Long labWorkId, String currentUserName, Authentication authentication) {
        Optional<LabWork> existingLabWorkOpt = labWorkRepository.findById(labWorkId);
        if (existingLabWorkOpt.isPresent()) {
            LabWork labWork = existingLabWorkOpt.get();

            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String currentUser = oauth2User.getAttribute("preferred_username");

            saveLabWorkHistory(labWork, ChangeType.DELETE, currentUser);

            labWorkRepository.delete(labWork);
        }
    }

    public boolean canThisUserEditLabWork(LabWork labWork, Authentication authentication) {
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String currentUsername = oauth2User.getAttribute("preferred_username");

            System.out.println("Current username: " + currentUsername);
            boolean isOwner = labWork.getOwner().getUsername().equals(currentUsername);

            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            Map<String, Object> attributes = token.getPrincipal().getAttributes();

            List<String> roles = (List<String>) attributes.get("roles");
            boolean isAdmin = roles != null && roles.contains("ROLE_ADMIN");

            System.out.println("Is user ADMIN? " + isAdmin);

            return isOwner || isAdmin;
        } catch (Exception e) {
            System.out.println("Error while checking edit permissions: " + e.getMessage());
            return false;
        }
    }


    public Optional<LabWork> findById(Long labWorkId) {
        return labWorkRepository.findById(labWorkId);
    }

    private void saveLabWorkHistory(LabWork labWork, ChangeType changeType, String changedBy) {
        LabWorkHistory history = new LabWorkHistory();
        history.setName(labWork.getName());
        history.setCreationDate(labWork.getCreationDate());
        history.setDescription(labWork.getDescription());
        history.setDifficulty(labWork.getDifficulty());
        history.setDiscipline(labWork.getDiscipline());
        history.setMinimalPoint(labWork.getMinimalPoint());
        history.setPersonalQualitiesMinimum(labWork.getPersonalQualitiesMinimum());
        history.setPersonalQualitiesMaximum(labWork.getPersonalQualitiesMaximum());
        history.setAuthor(labWork.getAuthor());
        history.setOwner_id(labWork.getOwner());
        history.setChangeType(changeType);
        history.setChangedBy(changedBy);
        history.setCoordinates(labWork.getCoordinates());
        history.setUpdateTime(LocalDateTime.now());
        labWorkHistoryRepository.save(history);
    }

    public void addLabWorksFromFile(List<Map<String, String>> rows, Authentication authentication) {
        int rowNum = 1;
        for (Map<String, String> row : rows) {

            LabWork labWork = new LabWork();
            labWork.setName(row.get("name"));
            labWork.setDescription(row.get("description"));
            labWork.setDifficulty(Difficulty.valueOf(row.get("difficulty")));
            labWork.setMinimalPoint(Float.parseFloat(row.get("minimalPoint")));
            labWork.setPersonalQualitiesMinimum(Double.parseDouble(row.get("personalQualitiesMinimum")));
            labWork.setPersonalQualitiesMaximum(Float.parseFloat(row.get("personalQualitiesMaximum")));

            Person author = authorService.getOrCreateAuthorFromRow(row);
            labWork.setAuthor(author);

            Discipline discipline = disciplineService.getOrCreateDisciplineFromRow(row);
            labWork.setDiscipline(discipline);

            Coordinates coordinates = coordinatesService.getOrCreateCoordinatesFromRow(row);
            labWork.setCoordinates(coordinates);

            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String userName = oauth2User.getAttribute("preferred_username");
            labWork.setOwner(userService.findByUsername(userName).orElseThrow(() -> new IllegalArgumentException("User not found")));

            labWorkRepository.save(labWork);
            System.out.println("Строка "+ rowNum + " успешно добавлена");;
            rowNum++;
        }
    }

    public void deleteByAuthor(String author) {
        Person person = personRepository.findByName(author)
                .orElseThrow(() -> new RuntimeException("Person not found"));

        List<LabWork> labWorks = labWorkRepository.findByAuthor(person);
        if (!labWorks.isEmpty()) {
            labWorkRepository.deleteAll(labWorks);
        }
    }





    public List<LabWork> findByDescriptionPrefix(String prefix) {
        return labWorkRepository.findByDescriptionStartingWith(prefix);
    }

    public boolean deleteOneByAuthor(Long authorId) {
        Optional<LabWork> labWork = labWorkRepository.findFirstByAuthorId(authorId);
        if (labWork.isPresent()) {
            labWorkRepository.delete(labWork.get());
            return true;
        }
        return false;
    }


    @Transactional
    public void decreaseDifficulty(Long labWorkId, int steps) {
        LabWork labWork = labWorkRepository.findById(labWorkId)
                .orElseThrow(() -> new RuntimeException("LabWork not found"));

        Difficulty currentDifficulty = labWork.getDifficulty();

        int newDifficultyValue = currentDifficulty.getValue() - steps;

        if (newDifficultyValue <= Difficulty.VERY_EASY.getValue()) {
            labWork.setDifficulty(Difficulty.VERY_EASY);
        } else {
            labWork.setDifficulty(Difficulty.fromValue(newDifficultyValue));
        }

        labWorkRepository.save(labWork);
    }



    @Transactional
    public void removeFromDiscipline(Long labWorkId) {
        LabWork labWork = labWorkRepository.findById(labWorkId)
                .orElseThrow(() -> new IllegalArgumentException("LabWork not found with id: " + labWorkId));

        labWork.setDiscipline(null);
        labWorkRepository.save(labWork);
    }



    public Page<LabWork> getLabWorksPage(
            Long id, String name, String description, String authorName, String ownerUsername, String creationDate,
            String disciplineName, Integer disciplinePracticeHours, String difficulty, Double coordinatesX,
            Double coordinatesY, Double minimalPoint, Double personalQualitiesMaximum, Double personalQualitiesMinimum,
            Double authorWeight, String authorEyeColor, String authorHairColor, Double authorLocationX,
            Double authorLocationY, String authorLocationName, Integer authorPassportID, Pageable pageable) {

        Specification<LabWork> spec = Specification.where(null);

        if (id != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("id"), id));
        }
        if (name != null && !name.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (description != null && !description.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
        }
        if (authorName != null && !authorName.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("author").get("name")), "%" + authorName.toLowerCase() + "%"));
        }
        if (ownerUsername != null && !ownerUsername.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("owner").get("username")), "%" + ownerUsername.toLowerCase() + "%"));
        }
        if (creationDate != null && !creationDate.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("creationDate"), LocalDate.parse(creationDate)));
        }
        if (disciplineName != null && !disciplineName.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("discipline").get("name")), "%" + disciplineName.toLowerCase() + "%"));
        }
        if (disciplinePracticeHours != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("discipline").get("practiceHours"), disciplinePracticeHours));
        }
        if (difficulty != null && !difficulty.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("difficulty")), "%" + difficulty.toLowerCase() + "%"));
        }
        if (coordinatesX != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("coordinates").get("x"), coordinatesX));
        }
        if (coordinatesY != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("coordinates").get("y"), coordinatesY));
        }
        if (minimalPoint != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("minimalPoint"), minimalPoint));
        }
        if (personalQualitiesMaximum != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("personalQualitiesMaximum"), personalQualitiesMaximum));
        }
        if (personalQualitiesMinimum != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("personalQualitiesMinimum"), personalQualitiesMinimum));
        }
        if (authorWeight != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("author").get("weight"), authorWeight));
        }
        if (authorEyeColor != null && !authorEyeColor.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("author").get("eyeColor")), "%" + authorEyeColor.toLowerCase() + "%"));
        }
        if (authorHairColor != null && !authorHairColor.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("author").get("hairColor")), "%" + authorHairColor.toLowerCase() + "%"));
        }
        if (authorLocationX != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("author").get("location").get("x"), authorLocationX));
        }
        if (authorLocationY != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("author").get("location").get("y"), authorLocationY));
        }
        if (authorLocationName != null && !authorLocationName.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("author").get("location").get("name")), "%" + authorLocationName.toLowerCase() + "%"));
        }
        if (authorPassportID != null ) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("author").get("passportID"), authorPassportID));
        }

        return labWorkRepository.findAll(spec, pageable);
    }


}
