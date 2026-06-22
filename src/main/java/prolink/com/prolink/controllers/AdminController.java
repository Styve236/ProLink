package prolink.com.prolink.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.services.AdminService;
import prolink.com.prolink.services.NotificationService;
import prolink.com.prolink.services.OffreService;


@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final OffreService offreService;
    private final NotificationService notificationService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", adminService.getStatsDashboard());
        return "admin/admin-dashboard";
    }

    @GetMapping("/users")
    public String listeUtilisateurs(Model model) {
        model.addAttribute("users", adminService.getTousLesUtilisateurs());
        return "admin/admin-users";
    }

    @GetMapping("/users/{role}")
    public String listeParRole(@PathVariable String role, Model model) {
        try {
            RoleUtilisateur r = RoleUtilisateur.valueOf(role.toUpperCase());
            model.addAttribute("users", adminService.getUtilisateursParRole(r));
            model.addAttribute("roleFiltre", role);
        } catch (IllegalArgumentException e) {
            model.addAttribute("users", adminService.getTousLesUtilisateurs());
        }
        return "admin/admin-users";
    }

    @GetMapping("/valider/{id}")
    public String afficherValidation(@PathVariable Long id, Model model) {
        model.addAttribute("utilisateur", adminService.getUtilisateurParId(id));
        return "admin/admin-validation";
    }

    @PostMapping("/valider")
    public String validerCompte(@RequestParam Long userId,
                                @RequestParam int trustScore,
                                @RequestParam(required = false) String commentaire,
                                RedirectAttributes redirectAttributes) {
        try {
            adminService.validerCompte(userId, trustScore, commentaire);
            redirectAttributes.addFlashAttribute("succes",
                    "Compte validé et activé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/bannir/{id}")
    public String bannirUtilisateur(@PathVariable Long id,
                                    @RequestParam(required = false) String raison,
                                    RedirectAttributes redirectAttributes) {
        try {
            adminService.changerStatutCompte(id, StatutCompte.SUSPENDU, raison);
            redirectAttributes.addFlashAttribute("succes", "Compte suspendu.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/archiver/{id}")
    public String archiverUtilisateur(@PathVariable Long id,
                                      RedirectAttributes redirectAttributes) {
        try {
            adminService.changerStatutCompte(id, StatutCompte.ARCHIVE, null);
            redirectAttributes.addFlashAttribute("succes", "Compte archivé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/reactiver/{id}")
    public String reactiverUtilisateur(@PathVariable Long id,
                                       RedirectAttributes redirectAttributes) {
        try {
            adminService.changerStatutCompte(id, StatutCompte.ACTIF, null);
            redirectAttributes.addFlashAttribute("succes", "Compte réactivé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimerUtilisateur(@PathVariable Long id,
                                       RedirectAttributes redirectAttributes) {
        try {
            adminService.supprimerUtilisateur(id);
            redirectAttributes.addFlashAttribute("succes",
                    "Utilisateur supprimé définitivement.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/trust-score/{id}")
    public String mettreAJourTrustScore(@PathVariable Long id,
                                        @RequestParam int score,
                                        RedirectAttributes redirectAttributes) {
        try {
            adminService.mettreAJourTrustScore(id, score);
            redirectAttributes.addFlashAttribute("succes",
                    "Trust Score mis à jour : " + score + "/100");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/valider/" + id;
    }

    // GESTION DES OFFRES

    @GetMapping("/offres/pending")
    public String offresEnAttente(Model model) {
        model.addAttribute("offres", offreService.getOffresEnAttente());
        return "admin/admin-offres-pending";
    }

    @PostMapping("/offres/approuver")
    public String approuverOffre(@RequestParam Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            offreService.approuverOffre(id);
            redirectAttributes.addFlashAttribute("succes", "Offre approuvée et publiée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/offres/pending";
    }

    @PostMapping("/offres/rejeter")
    public String rejeterOffre(@RequestParam Long id,
                               @RequestParam(required = false) String motif,
                               RedirectAttributes redirectAttributes) {
        try {
            offreService.rejeterOffre(id, motif);
            redirectAttributes.addFlashAttribute("succes", "Offre rejetée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/offres/pending";
    }

    // VALIDATION DES DOCUMENTS
    @GetMapping("/documents")
    public String documentsEnAttente(Model model) {
        model.addAttribute("documents", adminService.getDocumentsEnAttente());
        return "admin/admin-documents";
    }

    @PostMapping("/documents/valider")
    public String validerDocument(@RequestParam Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            adminService.validerDocument(id);
            redirectAttributes.addFlashAttribute("succes", "Document validé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/documents";
    }

    @PostMapping("/documents/rejeter")
    public String rejeterDocument(@RequestParam Long id,
                                  @RequestParam(required = false) String commentaire,
                                  RedirectAttributes redirectAttributes) {
        try {
            adminService.rejeterDocument(id, commentaire);
            redirectAttributes.addFlashAttribute("succes", "Document rejeté.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/documents";
    }
}