package prolink.com.prolink.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import prolink.com.prolink.entities.Notification;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.repositories.UserRepository;
import prolink.com.prolink.services.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationApiController(NotificationService notificationService,
                                     UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // Retourne toutes les notifications de l'utilisateur connecté
    @GetMapping
    public ResponseEntity<List<Notification>> getMesNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUtilisateur(userDetails.getUsername());
        return ResponseEntity.ok(notificationService.getMesNotifications(user));
    }

    // Retourne le nombre de notifications non lues — badge navbar
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> compterNonLues(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUtilisateur(userDetails.getUsername());
        long count = notificationService.compterNonLues(user);
        return ResponseEntity.ok(Map.of("nonLues", count));
    }

    // Marque toutes les notifications comme lues
    @PostMapping("/lire-tout")
    public ResponseEntity<Map<String, String>> marquerToutesLues(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUtilisateur(userDetails.getUsername());
        notificationService.marquerToutesLues(user);
        return ResponseEntity.ok(Map.of("statut", "ok"));
    }

    // UTILITAIRE PRIVÉ
    private User getUtilisateur(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilisateur introuvable : " + email));
    }
}