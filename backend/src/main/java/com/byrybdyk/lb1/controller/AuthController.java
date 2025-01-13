package com.byrybdyk.lb1.controller;

import com.byrybdyk.lb1.model.SessionMapping;
import com.byrybdyk.lb1.model.User;
import com.byrybdyk.lb1.repository.SessionMappingRepository;
import com.byrybdyk.lb1.security.CustomSessionRegistry;
import com.byrybdyk.lb1.security.SessionTrackingListener;
import com.byrybdyk.lb1.service.AdminRequestService;
import com.byrybdyk.lb1.service.KeycloakAdminClientService;
import com.byrybdyk.lb1.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;
import org.springframework.security.oauth2.jwt.Jwt;



import java.net.URL;
import java.util.*;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AdminRequestService adminRequestService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private KeycloakAdminClientService keycloakAdminClientService;
    private SessionTrackingListener sessionTrackingListener;
    private final CustomSessionRegistry sessionRegistry;
    private SessionMappingRepository sessionMappingRepository;


    @Autowired
    public AuthController(UserService userService, PasswordEncoder passwordEncoder, AdminRequestService adminRequestService,
                          OAuth2AuthorizedClientService authorizedClientService, ClientRegistrationRepository clientRegistrationRepository, KeycloakAdminClientService keycloakAdminClientService, SessionTrackingListener sessionTrackingListener, CustomSessionRegistry sessionRegistry, SessionMappingRepository sessionMappingRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.adminRequestService = adminRequestService;
        this.authorizedClientService = authorizedClientService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.keycloakAdminClientService = keycloakAdminClientService;
        this.sessionTrackingListener = sessionTrackingListener;
        this.sessionRegistry = sessionRegistry;
        this.sessionMappingRepository = sessionMappingRepository;
    }



    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("roles", Arrays.asList("USER", "ADMIN"));
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {
        try {
            keycloakAdminClientService.registerUser(username, password, role);
            redirectAttributes.addFlashAttribute("message", "Регистрация успешна. Дождитесь подтверждения, если выбрали роль ADMIN.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка регистрации: " + e.getMessage());
        }
        return "redirect:/login/oauth2/code/keycloak";
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return new ResponseEntity<>("Login successful.", HttpStatus.OK);
        }
        return new ResponseEntity<>("Invalid username or password.", HttpStatus.UNAUTHORIZED);
    }



    @PostMapping("/logout/backchannel")
    public ResponseEntity<String> handleBackchannelLogout(@RequestParam("logout_token") String logoutToken, HttpServletResponse response) {
        System.out.println("Received logout token: " + logoutToken);
        try {
            String sessionId = validateAndExtractSid(logoutToken);
            System.out.println("Extracted sessionId: " + sessionId);

            if (sessionId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token: missing 'sid'");
            }

            invalidateSession(sessionId);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("Error processing logout token: " + e.getMessage()); // Логируем ошибки
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid logout token: " + e.getMessage());
        }
    }

    private void invalidateSession(String sessionId) {
        List<SessionMapping> sessionMappings = sessionMappingRepository.findAllByKeycloakSid(sessionId);

        if (!sessionMappings.isEmpty()) {
            for (SessionMapping sessionMapping : sessionMappings) {
                String springSessionId = sessionMapping.getSpringSessionId();

                sessionMappingRepository.deleteById(sessionMapping.getId());
                System.out.println("Session invalidated from database: " + sessionId);

                HttpSession sessionToInvalidateInMemory = sessionRegistry.getSession(springSessionId);
                if (sessionToInvalidateInMemory != null) {
                    sessionToInvalidateInMemory.invalidate();
                    System.out.println("Session invalidated from memory: " + springSessionId);
                } else {
                    System.out.println("Session not found in memory: " + springSessionId);
                }
            }
        } else {
            System.out.println("Session not found in database: " + sessionId);
        }
    }

    public String validateAndExtractSid(String logoutToken) {
        try {
            Jwt jwt = decodeJwtWithoutCheckingType(logoutToken);
            System.out.println("Decoded JWT: " + jwt.getClaims());

            return jwt.getClaim("sid");
        } catch (Exception ex) {

            System.err.println("Error processing JWT token: " + ex.getMessage());
            throw new IllegalArgumentException("Failed to validate logout token", ex);
        }
    }

    private Jwt decodeJwtWithoutCheckingType(String jwtToken) throws Exception {

        String[] jwtParts = jwtToken.split("\\.");
        if (jwtParts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token format");
        }

        String header = new String(Base64.getUrlDecoder().decode(jwtParts[0]));
        String payload = new String(Base64.getUrlDecoder().decode(jwtParts[1]));

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

        return Jwt.withTokenValue(jwtToken)
                .header("alg", "RS256")  // Тип алгоритма
                .claim("sid", claims.get("sid"))
                .build();
    }

    public JWKSource<SecurityContext> getKeycloakJwkSource() throws Exception {
        URL jwkSetURL = new URL("http://localhost:8180/realms/IS-realm/protocol/openid-connect/certs");
        return new RemoteJWKSet<>(jwkSetURL);
    }


    private PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
