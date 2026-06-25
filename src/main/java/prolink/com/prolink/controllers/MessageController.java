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
import prolink.com.prolink.services.NotificationService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @GetMapping
    public String boiteReception(@AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {

        User moi = getUtilisateur(userDetails.getUsername());
        List<User> contacts = getContacts(moi.getId());

        model.addAttribute("contacts", contacts);
        model.addAttribute("moi", moi);
        model.addAttribute("nonLus", messageRepository.countByDestinataireAndLuFalse(moi));
        model.addAttribute("messagesRecus", messageRepository.findByDestinataireOrderByDateEnvoiDesc(moi));

        return "messages/boite-reception";
    }

    @GetMapping("/conversation/{idDestinataire}")
    public String afficherConversation(@PathVariable Long idDestinataire,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       Model model) {
        User moi = getUtilisateur(userDetails.getUsername());

        User destinataire = getUtilisateurParId(idDestinataire);
        List<Message> conversation = messageRepository.findConversation(moi.getId(), idDestinataire);

        conversation.stream()
                .filter(m -> m.getDestinataire().getId().equals(moi.getId()) && !m.isLu())
                .forEach(m -> {
                    m.setLu(true);
                    m.setDateLecture(LocalDateTime.now());
                    messageRepository.save(m);
                });

        List<User> contacts = getContacts(moi.getId());

        model.addAttribute("conversation", conversation);
        model.addAttribute("destinataire", destinataire);
        model.addAttribute("moi", moi);
        model.addAttribute("contacts", contacts);
        model.addAttribute("nonLus", messageRepository.countByDestinataireAndLuFalse(moi));
        model.addAttribute("peutEnvoyer", peutEnvoyerMessage(moi, destinataire));

        return "messages/boite-reception";
    }

    @PostMapping("/envoyer/{idDestinataire}")
    public String envoyerMessage(@PathVariable Long idDestinataire,
                                 @RequestParam String contenu,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {

        if (contenu == null || contenu.isBlank()) {
            redirectAttributes.addFlashAttribute("erreur", "Le message ne peut pas être vide.");
            return "redirect:/messages/conversation/" + idDestinataire;
        }

        User expediteur = getUtilisateur(userDetails.getUsername());
        User destinataire = getUtilisateurParId(idDestinataire);

        if (!peutEnvoyerMessage(expediteur, destinataire)) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Vous devez attendre que " + destinataire.getPrenom()
                    + " lise votre message et vous réponde avant d'en envoyer un autre.");
            return "redirect:/messages/conversation/" + idDestinataire;
        }

        if (expediteur.getId().equals(idDestinataire)) {
            redirectAttributes.addFlashAttribute("erreur", "Vous ne pouvez pas vous envoyer un message.");
            return "redirect:/messages";
        }

        Message message = new Message();
        message.setExpediteur(expediteur);
        message.setDestinataire(destinataire);
        message.setContenu(contenu.trim());
        messageRepository.save(message);

        notificationService.notifierNouveauMessage(expediteur, destinataire, contenu.trim());

        return "redirect:/messages/conversation/" + idDestinataire;
    }

    private List<User> getContacts(Long userId) {
        Set<User> contactsSet = new HashSet<>();
        contactsSet.addAll(messageRepository.findExpediteurs(userId));
        contactsSet.addAll(messageRepository.findDestinataires(userId));
        return List.copyOf(contactsSet);
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

    private boolean peutEnvoyerMessage(User expediteur, User destinataire) {
        long envoyeParMoi = messageRepository
                .countByExpediteurAndDestinataire(expediteur, destinataire);
        long envoyeParLui = messageRepository
                .countByExpediteurAndDestinataire(destinataire, expediteur);
        // Premier message toujours autorisé, ou si le destinataire a déjà répondu
        return envoyeParMoi == 0 || envoyeParLui > 0;
    }
}
