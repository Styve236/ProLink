package prolink.com.prolink.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import prolink.com.prolink.entities.Message;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.repositories.MessageRepository;
import prolink.com.prolink.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    // BOITE DE RÉCEPTION
    @GetMapping
    public String boiteReception(@AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {

        User moi = getUtilisateur(userDetails.getUsername());

        // Messages reçus — triés par date décroissante
        List<Message> messagesRecus = messageRepository
                .findByDestinataireOrderByDateEnvoiDesc(moi);

        // Messages envoyés
        List<Message> messagesEnvoyes = messageRepository
                .findByExpediteurOrderByDateEnvoiDesc(moi);

        model.addAttribute("messagesRecus", messagesRecus);
        model.addAttribute("messagesEnvoyes", messagesEnvoyes);
        model.addAttribute("nonLus", messageRepository.countByDestinataireAndLuFalse(moi));

        return "messages/boite-reception";
    }

    // CONVERSATION AVEC UN UTILISATEUR
    @GetMapping("/conversation/{idDestinataire}")
    public String afficherConversation(@PathVariable Long idDestinataire,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       Model model) {
        User moi = getUtilisateur(userDetails.getUsername());
        User destinataire = getUtilisateurParId(idDestinataire);

        // Récupère tous les messages entre les deux utilisateurs
        List<Message> conversation = messageRepository
                .findConversation(moi.getId(), idDestinataire);

        // Marque les messages reçus comme lus
        conversation.stream()
                .filter(m -> m.getDestinataire().getId().equals(moi.getId()) && !m.isLu())
                .forEach(m -> {
                    m.setLu(true);
                    m.setDateLecture(LocalDateTime.now());
                    messageRepository.save(m);
                });

        model.addAttribute("conversation", conversation);
        model.addAttribute("destinataire", destinataire);
        model.addAttribute("moi", moi);

        return "messages/conversation";
    }

    // ENVOYER UN MESSAGE
    @PostMapping("/envoyer/{idDestinataire}")
    public String envoyerMessage(@PathVariable Long idDestinataire,
                                 @RequestParam String contenu,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {

        if (contenu == null || contenu.isBlank()) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Le message ne peut pas être vide.");
            return "redirect:/messages/conversation/" + idDestinataire;
        }

        User expediteur = getUtilisateur(userDetails.getUsername());
        User destinataire = getUtilisateurParId(idDestinataire);

        // Empêche d'envoyer un message à soi-même
        if (expediteur.getId().equals(idDestinataire)) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Vous ne pouvez pas vous envoyer un message.");
            return "redirect:/messages";
        }

        Message message = new Message();
        message.setExpediteur(expediteur);
        message.setDestinataire(destinataire);
        message.setContenu(contenu.trim());
        messageRepository.save(message);

        return "redirect:/messages/conversation/" + idDestinataire;
    }

    // NOUVEAU MESSAGE — formulaire
    @GetMapping("/nouveau/{idDestinataire}")
    public String nouveauMessage(@PathVariable Long idDestinataire,
                                 Model model,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User destinataire = getUtilisateurParId(idDestinataire);
        model.addAttribute("destinataire", destinataire);
        return "messages/nouveau-message";
    }

    // UTILITAIRES PRIVÉS
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
}