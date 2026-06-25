package prolink.com.prolink.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import prolink.com.prolink.entities.ChatMessage;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.repositories.ChatMessageRepository;
import prolink.com.prolink.repositories.MessageRepository;
import prolink.com.prolink.repositories.UserRepository;
import prolink.com.prolink.services.NotificationService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller du chat temps réel WebSocket.
 *
 * Flux complet d'un message :
 *  1. Client JS envoie vers /app/chat.envoyer
 *  2. @MessageMapping("/chat.envoyer") reçoit le message
 *  3. On sauvegarde en base et on diffuse vers /topic/chat.{roomId}
 *  4. Tous les abonnés reçoivent le message instantanément
 *
 * roomId = "user_{minId}_user_{maxId}" — garantit l'unicité
 * peu importe qui initie la conversation.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    // PAGE DE CHAT
    @GetMapping("/{idDestinataire}")
    public String afficherChat(@PathVariable Long idDestinataire,
                               Model model,
                               Principal principal) {

        User moi = getUtilisateur(principal.getName());
        User destinataire = getUtilisateurParId(idDestinataire);
        String roomId = construireRoomId(moi.getId(), idDestinataire);

        List<ChatMessage> historique = chatMessageRepository
                .findByRoomIdOrderByHorodatageAsc(roomId);

        boolean peutEnvoyer = peutEnvoyerMessage(moi, destinataire);

        model.addAttribute("moi", moi);
        model.addAttribute("destinataire", destinataire);
        model.addAttribute("roomId", roomId);
        model.addAttribute("historique", historique);
        model.addAttribute("peutEnvoyer", peutEnvoyer);

        return "messages/chat";
    }
    // RÉCEPTION ET DIFFUSION D'UN MESSAGE WEBSOCKET

    // RÉCEPTION ET DIFFUSION D'UN MESSAGE WEBSOCKET
    @MessageMapping("/chat.envoyer")
    public void envoyerMessage(@Payload Map<String, String> payload,
                               Principal principal) {

        String contenu = payload.get("contenu");
        String roomId  = payload.get("roomId");

        if (contenu == null || contenu.isBlank() || roomId == null) {
            return;
        }

        User expediteur = getUtilisateur(principal.getName());
        Long destinataireId = extraireIdDestinataire(roomId, expediteur.getId());
        User destinataire = getUtilisateurParId(destinataireId);

        if (!peutEnvoyerMessage(expediteur, destinataire)) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/erreurs",
                    Map.of("erreur", "Attendez que " + destinataire.getPrenom()
                            + " vous réponde avant d'envoyer un autre message.")
            );
            return;
        }

        ChatMessage message = new ChatMessage();
        message.setExpediteur(expediteur);
        message.setContenu(contenu.trim());
        message.setRoomId(roomId);
        message.setHorodatage(LocalDateTime.now());
        chatMessageRepository.save(message);

        notificationService.notifierNouveauMessage(expediteur, destinataire, contenu.trim());

        Map<String, String> reponse = Map.of(
                "expediteurId",  String.valueOf(expediteur.getId()),
                "expediteurNom", expediteur.getNomComplet(),
                "contenu",       message.getContenu(),
                "horodatage",    message.getHorodatage().toString(),
                "roomId",        roomId
        );

        messagingTemplate.convertAndSend("/topic/chat." + roomId, reponse);
    }

    // NOTIFICATION DE FRAPPE (optionnel — "X est en train d'écrire...")
    @MessageMapping("/chat.frappe")
    public void notifierFrappe(@Payload Map<String, String> payload,
                               Principal principal) {

        String roomId = payload.get("roomId");
        if (roomId == null) return;

        User expediteur = getUtilisateur(principal.getName());

        Map<String, String> frappe = Map.of(
                "expediteurNom", expediteur.getNomComplet(),
                "type",          "FRAPPE"
        );

        messagingTemplate.convertAndSend("/topic/chat." + roomId, frappe);
    }

    // UTILITAIRES PRIVÉS
    private String construireRoomId(Long idA, Long idB) {
        long min = Math.min(idA, idB);
        long max = Math.max(idA, idB);
        return "user_" + min + "_user_" + max;
    }

    private User getUtilisateur(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilisateur introuvable : " + email));
    }

    private User getUtilisateurParId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilisateur introuvable : " + id));
    }
    private Long extraireIdDestinataire(String roomId, Long monId) {
        String[] parties = roomId.replace("user_", "").split("_user_");
        long idA = Long.parseLong(parties[0]);
        long idB = Long.parseLong(parties[1]);
        return idA == monId ? idB : idA;
    }

    private boolean peutEnvoyerMessage(User expediteur, User destinataire) {
        long envoyeParMoi = messageRepository
                .countByExpediteurAndDestinataire(expediteur, destinataire);
        long envoyeParLui = messageRepository
                .countByExpediteurAndDestinataire(destinataire, expediteur);
        return envoyeParMoi == 0 || envoyeParLui > 0;
    }
}