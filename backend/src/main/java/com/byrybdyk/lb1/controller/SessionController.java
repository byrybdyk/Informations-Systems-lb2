//package com.byrybdyk.lb1.controller;
//
//import com.byrybdyk.lb1.model.SessionMapping;
//import com.byrybdyk.lb1.repository.SessionMappingRepository;
//import com.byrybdyk.lb1.security.SessionTrackingListener;
//import jakarta.servlet.http.HttpSession;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/sessions")
//public class SessionController {
//
//    private final SessionTrackingListener sessionTrackingListener;
//
//    public SessionController(SessionTrackingListener sessionTrackingListener) {
//        this.sessionTrackingListener = sessionTrackingListener;
//    }
//
//    @Autowired
//    private SessionMappingRepository sessionMappingRepository;
//
//    public void onUserLogin(HttpSession session, String keycloakSid) {
//        SessionMapping mapping = new SessionMapping();
//        mapping.setSpringSessionId(session.getId());
//        mapping.setKeycloakSid(keycloakSid);
//
//        sessionMappingRepository.save(mapping);
//    }
//
//
//    @GetMapping
//    public List<String> getActiveSessions() {
//        return sessionTrackingListener.getActiveSessions().stream()
//                .map(HttpSession::getId)
//                .collect(Collectors.toList());
//    }
//
//    @PostMapping("/invalidate/{sessionId}")
//    public ResponseEntity<String> invalidateSession(@PathVariable String sessionId) {
//        HttpSession session = sessionTrackingListener.getSessionById(sessionId);
//        if (session != null) {
//            session.invalidate();
//            return ResponseEntity.ok("Session invalidated: " + sessionId);
//        }
//        return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                .body("Session not found: " + sessionId);
//    }
//}
