package com.byrybdyk.lb1.security;

import com.byrybdyk.lb1.model.SessionMapping;
import com.byrybdyk.lb1.repository.SessionMappingRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionTrackingListener implements HttpSessionListener {

    @Autowired
    private SessionMappingRepository sessionMappingRepository; // Добавляем репозиторий

    private final Map<String, HttpSession> activeSessions = new ConcurrentHashMap<>();

//    // При создании сессии
//    @Override
//    public void sessionCreated(HttpSessionEvent event) {
//        HttpSession session = event.getSession();
//        activeSessions.put(session.getId(), session);
//        System.out.println("Session created: " + session.getId());
//
//        // Логирование всех активных сессий
//        activeSessions.forEach((id, sess) -> System.out.println("Active session: " + id));
//
//        // Проверяем, если пользователь уже авторизован через Keycloak
//        if (SecurityContextHolder.getContext().getAuthentication() != null
//                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
//
//            OAuth2User principal = (OAuth2User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//
//            if (principal != null) {
//                // Получаем sid из атрибутов пользователя после авторизации
//                String sid = (String) principal.getAttributes().get("sid");
//                if (sid != null) {
//                    System.out.println("Authenticated user's sid: " + sid);
//
//                    // Сохраняем в базу данных
//                    SessionMapping mapping = new SessionMapping();
//                    mapping.setSpringSessionId(session.getId());
//                    mapping.setKeycloakSid(sid);
//                    sessionMappingRepository.save(mapping); // Сохраняем сессию
//                } else {
//                    System.out.println("SID not found for authenticated user.");
//                }
//            }
//        }
//    }
//
//
//    // При уничтожении сессии
//    @Override
//    public void sessionDestroyed(HttpSessionEvent event) {
//        HttpSession session = event.getSession();
//        activeSessions.remove(session.getId());
//        System.out.println("Session destroyed: " + session.getId());
//
//        // Удаляем сессию из базы данных
//        Optional<SessionMapping> mappingOptional = sessionMappingRepository.findBySpringSessionId(session.getId());
//        mappingOptional.ifPresent(sessionMappingRepository::delete);  // Удаляем объект, если он существует
//    }
//
//    public void onUserLogin(HttpSession session, String keycloakSid) {
//        SessionMapping mapping = new SessionMapping();
//        mapping.setSpringSessionId(session.getId());
//        mapping.setKeycloakSid(keycloakSid);
//
//        // Сохраняем в базу
//        sessionMappingRepository.save(mapping);
//    }
//
//    // Получение всех активных сессий
//    public Collection<HttpSession> getActiveSessions() {
//        return activeSessions.values();
//    }
//
//    // Получение сессии по ID
//    public HttpSession getSessionById(String sessionId) {
//        return activeSessions.get(sessionId);
//    }
}
