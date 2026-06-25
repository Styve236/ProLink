package prolink.com.prolink.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.TypeDocument;
import prolink.com.prolink.repositories.UserRepository;
import prolink.com.prolink.services.DocumentService;
import prolink.com.prolink.services.NotificationService;

import java.security.Principal;

@Controller
@RequestMapping("/profil/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public DocumentController(DocumentService documentService,
                              NotificationService notificationService,
                              UserRepository userRepository) {
        this.documentService = documentService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String pageDocuments(@AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = getUtilisateur(userDetails.getUsername());
        model.addAttribute("documents", documentService.getMesDocuments(userDetails.getUsername()));
        model.addAttribute("utilisateur", user);
        model.addAttribute("typesDocument", TypeDocument.values());
        long notifNonLues = notificationService.compterNonLues(user);
        model.addAttribute("notifNonLues", notifNonLues);
        return "profil/mes-documents";
    }

    @PostMapping("/upload")
    public String uploadDocument(@RequestParam("fichier") MultipartFile fichier,
                                 @RequestParam("type") TypeDocument type,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            documentService.televerserDocument(principal.getName(), fichier, type);
            redirectAttributes.addFlashAttribute("succes", "Document déposé avec succès. En attente de validation.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/profil/documents";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimerDocument(@PathVariable Long id,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            documentService.supprimerDocument(id, principal.getName());
            redirectAttributes.addFlashAttribute("succes", "Document supprimé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/profil/documents";
    }

    private User getUtilisateur(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + email));
    }
}
