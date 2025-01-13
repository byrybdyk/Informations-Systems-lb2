    package com.byrybdyk.lb1.controller;

    import com.byrybdyk.lb1.dto.LabWorkDTO;
    import com.byrybdyk.lb1.model.*;
    import com.byrybdyk.lb1.model.util.LabWorkDeleteMessage;
    import com.byrybdyk.lb1.service.*;
    import jakarta.validation.Valid;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.domain.Sort;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.messaging.handler.annotation.MessageMapping;
    import org.springframework.messaging.handler.annotation.Payload;
    import org.springframework.messaging.simp.SimpMessagingTemplate;
    import org.springframework.security.oauth2.core.user.OAuth2User;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.security.core.Authentication;

    import java.security.Principal;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.Optional;

    @RestController
    @RequestMapping("/labworks")
    public class LabWorkController {

        private final LabWorkService labWorkService;
        private final SimpMessagingTemplate messagingTemplate;
        private final DisciplineService disciplineService;
        private final CoordinatesService coordinatesService;
        private final PersonService personService;
        private final LocationService locationService;


        @Autowired
        public LabWorkController(LabWorkService labWorkService, SimpMessagingTemplate messagingTemplate, PersonService personService, DisciplineService disciplineService, CoordinatesService coordinatesService, LocationService locationService) {
            this.labWorkService = labWorkService;
            this.messagingTemplate = messagingTemplate;
            this.personService = personService;
            this.disciplineService = disciplineService;
            this.coordinatesService = coordinatesService;
            this.locationService = locationService;
        }

        @GetMapping("/form-data")
        public ResponseEntity<?> getFormData() {
            List<Discipline> disciplines = disciplineService.findAll();
            List<Coordinates> coordinates = coordinatesService.findAll();
            List<Person> authors = personService.findAll();
            List<Location> locations = locationService.findAll();

            FormData formData = new FormData();
            formData.setDisciplines(disciplines);
            formData.setCoordinates(coordinates);
            formData.setAuthors(authors);
            formData.setLocations(locations);

            return new ResponseEntity<>(formData, HttpStatus.OK);
        }

        @MessageMapping("/labworks/add")
        public ResponseEntity<LabWork> createLabWork(@Valid @RequestBody LabWorkDTO labWorkDTO,Authentication authentication) {
            try {
                LabWork createdLabWork = labWorkService.createLabWorkFromDTO(labWorkDTO, authentication);

                System.out.println("Sending message to /topic/labworks: " + createdLabWork);
                messagingTemplate.convertAndSend("/topic/labworks", createdLabWork);
                System.out.println("Message sent to /topic/labworks.");

                return new ResponseEntity<>(HttpStatus.CREATED);
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        @MessageMapping("/labworks/update")
        public ResponseEntity<LabWork> updateLabWork(@Valid @RequestBody LabWorkDTO labWorkDTO,Authentication authentication) {
            try {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                String username = oauth2User.getAttribute("preferred_username");
                LabWork createdLabWork = labWorkService.updateLabWorkFromDTO(labWorkDTO, username,authentication);

                System.out.println("Sending message to /topic/labworks: " + createdLabWork);
                messagingTemplate.convertAndSend("/topic/labworks", createdLabWork);
                System.out.println("Message sent to /topic/labworks.");

                return new ResponseEntity<>(HttpStatus.CREATED);
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        @MessageMapping("/labworks/delete")
        public ResponseEntity deleteLabWork(@Payload LabWorkDeleteMessage message,Authentication authentication) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                String username = oauth2User.getAttribute("preferred_username");
            Long labWorkId = message.getLabWorkId();

            try {
                labWorkService.deleteLabWork(labWorkId, username,authentication);
                LabWorkDeleteMessage deleteMessage = new LabWorkDeleteMessage(labWorkId);
                deleteMessage.setType("delete");
                messagingTemplate.convertAndSend("/topic/labworks", deleteMessage);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        @GetMapping("/{labWorkId}")
        public ResponseEntity<LabWork> getLabWorkById(@PathVariable Long labWorkId) {
            try {
                Optional<LabWork> labWorkOpt = labWorkService.findById(labWorkId);
                if (labWorkOpt.isPresent()) {
                    LabWork labWork = labWorkOpt.get();
                    return new ResponseEntity<>(labWork, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            } catch (Exception e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        @GetMapping("/{labWorkId}/can-edit")
        public ResponseEntity<Boolean> canEditLabWork(@PathVariable Long labWorkId,  Authentication authentication) {
            try {


                Optional<LabWork> existingLabWorkOpt = labWorkService.findById(labWorkId);
                if (!existingLabWorkOpt.isPresent()) {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }

                LabWork labWork = existingLabWorkOpt.get();
                System.out.println("LabWork ID: " + labWork.getId() + ", Owner ID: " + labWork.getOwner().getId());

                boolean canEdit = labWorkService.canThisUserEditLabWork(labWork, authentication);
                System.out.println("Can edit ПОЛУЧЕН" + canEdit);
                if (canEdit) {
                    return new ResponseEntity<>(true, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(false, HttpStatus.FORBIDDEN);
                }
            } catch (Exception e) {
                System.out.println("Error while checking edit permissions: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        @DeleteMapping("/deleteByAuthor/{authorId}")
        public ResponseEntity<String> deleteByAuthor(@PathVariable Long authorId) {
            boolean deleted = labWorkService.deleteOneByAuthor(authorId );
            return ResponseEntity.ok(deleted ? "LabWork deleted successfully." : "No LabWork found with the specified author.");
        }

        @GetMapping("/countByAuthorLessThenWeight/{weight}")
        public ResponseEntity<String> countByAuthor(@PathVariable Double weight) {
            long count = personService.countByAuthorLessThan(weight);
            return ResponseEntity.ok("Count of LabWorks with author weight less than " + weight + ": " + count);
        }

        @GetMapping("/findByDescriptionPrefix/{prefix}")
        public ResponseEntity<List<LabWork>> findByDescriptionPrefix(@PathVariable String prefix) {
            List<LabWork> labWorks = labWorkService.findByDescriptionPrefix(prefix);
            return ResponseEntity.ok(labWorks);
        }

        @PostMapping("/decreaseDifficulty/{labWorkId}/{steps}")
        public ResponseEntity<String> decreaseDifficulty(
                @PathVariable Long labWorkId,
                @PathVariable int steps){

            try {
                labWorkService.decreaseDifficulty(labWorkId, steps);
                return ResponseEntity.ok("LabWork difficulty decreased successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
            }}

        @GetMapping("/home")
        public ResponseEntity<Map<String, Object>> showUserHome(
                @RequestParam(defaultValue = "1") int page,
                @RequestParam(defaultValue = "10") int size,
                @RequestParam(required = false) String sort,
                @RequestParam(defaultValue = "asc") String order,
                @RequestParam(required = false) Long id,
                @RequestParam(required = false) String name,
                @RequestParam(required = false) String description,
                @RequestParam(required = false) String authorName,
                @RequestParam(required = false) String ownerUsername,
                @RequestParam(required = false) String creationDate,
                @RequestParam(required = false) String disciplineName,
                @RequestParam(required = false) Integer disciplinePracticeHours,
                @RequestParam(required = false) String difficulty,
                @RequestParam(required = false) Double coordinatesX,
                @RequestParam(required = false) Double coordinatesY,
                @RequestParam(required = false) Double minimalPoint,
                @RequestParam(required = false) Double personalQualitiesMaximum,
                @RequestParam(required = false) Double personalQualitiesMinimum,
                @RequestParam(required = false) Double authorWeight,
                @RequestParam(required = false) String authorEyeColor,
                @RequestParam(required = false) String authorHairColor,
                @RequestParam(required = false) Double authorLocationX,
                @RequestParam(required = false) Double authorLocationY,
                @RequestParam(required = false) String authorLocationName,
                @RequestParam(required = false) Integer authorPassportID) {

            Sort sorting = Sort.unsorted();
            if (sort != null) {
                sorting = Sort.by(order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort);
            }

            Pageable pageable = PageRequest.of(page - 1, size, sorting);

            Page<LabWork> labWorksPage = labWorkService.getLabWorksPage(id,
                    name, description, authorName, ownerUsername, creationDate, disciplineName, disciplinePracticeHours,
                    difficulty, coordinatesX, coordinatesY, minimalPoint, personalQualitiesMaximum, personalQualitiesMinimum,
                    authorWeight, authorEyeColor, authorHairColor, authorLocationX, authorLocationY, authorLocationName,
                    authorPassportID, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("labWorks", labWorksPage.getContent());
            response.put("currentPage", page);
            response.put("totalPages", labWorksPage.getTotalPages());

            return ResponseEntity.ok(response);
        }

//        @PostMapping("/decreaseDifficulty")
//        public ResponseEntity<String> decreaseDifficulty(@RequestParam Long id, @RequestParam int steps) {
//            labWorkService.decreaseDifficulty(id, steps);
//            return ResponseEntity.ok("Difficulty decreased by " + steps + " steps.");
//        }
//
//        @DeleteMapping("/removeFromDiscipline")
//        public ResponseEntity<String> removeFromDiscipline(@RequestParam Long id) {
//            labWorkService.removeFromDiscipline(id);
//            return ResponseEntity.ok("LabWork removed from discipline.");
//        }



    }
