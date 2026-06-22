package prolink.com.prolink.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import prolink.com.prolink.entities.ChatMessage;
import prolink.com.prolink.entities.LinkAction;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.repositories.ChatMessageRepository;
import prolink.com.prolink.repositories.UserRepository;
import prolink.com.prolink.services.LinkActionService;

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
    private final UserRepository userRepository;
    private final LinkActionService linkActionService;
    // PAGE DE CHAT
    @GetMapping("/{idDestinataire}")
    public String afficherChat(@PathVariable Long idDestinataire,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {

        User moi = getUtilisateur(userDetails.getUsername());

        // NOUVEAU : bloque l'accès si pas de connexion acceptée
        if (!linkActionService.peuventCommuniquer(moi.getId(), idDestinataire)) {
            return "redirect:/connexions/demander/" + idDestinataire;
        }

        User destinataire = getUtilisateurParId(idDestinataire);
        String roomId = construireRoomId(moi.getId(), idDestinataire);

        List<ChatMessage> historique = chatMessageRepository
                .findByRoomIdOrderByHorodatageAsc(roomId);

        model.addAttribute("moi", moi);
        model.addAttribute("destinataire", destinataire);
        model.addAttribute("roomId", roomId);
        model.addAttribute("historique", historique);

        return "messages/chat";
    }
    // RÉCEPTION ET DIFFUSION D'UN MESSAGE WEBSOCKET

    // RÉCEPTION ET DIFFUSION D'UN MESSAGE WEBSOCKET
    @MessageMapping("/chat.envoyer")
    public void envoyerMessage(@Payload Map<String, String> payload,
                               @AuthenticationPrincipal UserDetails userDetails) {

        String contenu = payload.get("contenu");
        String roomId  = payload.get("roomId");

        if (contenu == null || contenu.isBlank() || roomId == null) {
            return;
        }

        User expediteur = getUtilisateur(userDetails.getUsername());
        Long destinataireId = extraireIdDestinataire(roomId, expediteur.getId());

        // NOUVEAU : vérification d'accès avant tout envoi/sauvegarde
        if (!linkActionService.peuventCommuniquer(expediteur.getId(), destinataireId)) {
            messagingTemplate.convertAndSendToUser(
                    userDetails.getUsername(),
                    "/queue/erreurs",
                    Map.of("erreur", "Vous devez d'abord être en contact avec cet utilisateur.")
            );
            return;
        }

        ChatMessage message = new ChatMessage();
        message.setExpediteur(expediteur);
        message.setContenu(contenu.trim());
        message.setRoomId(roomId);
        message.setHorodatage(LocalDateTime.now());
        chatMessageRepository.save(message);

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
                               @AuthenticationPrincipal UserDetails userDetails) {

        String roomId = payload.get("roomId");
        if (roomId == null) return;

        User expediteur = getUtilisateur(userDetails.getUsername());

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
}