package prolink.com.prolink.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import prolink.com.prolink.entities.Etudiant;
import prolink.com.prolink.entities.Freelance;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.services.AuthService;
import prolink.com.prolink.services.CandidatureService;
import prolink.com.prolink.services.DocumentService;
import prolink.com.prolink.services.NotificationService;
import prolink.com.prolink.services.OffreService;
import prolink.com.prolink.services.ProfilService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/profil")
public class ProfilController {

    private final ProfilService profilService;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final OffreService offreService;
    private final CandidatureService candidatureService;
    private final DocumentService documentService;

    // DASHBOARD — redirection selon le rôle


    @GetMapping("/dashboard") // Ou l'URL que tu as définie au-dessus de la méthode
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                            Model model) {

        User user = profilService.getProfilComplet(userDetails.getUsername());

        if (user.getStatut() != StatutCompte.ACTIF){
            return "redirect:/compte/en-attente";
        }
        model.addAttribute("utilisateur", user);

        // Notifications non lues pour le badge dans la navbar
        long notifNonLues = notificationService.compterNonLues(user);
        model.addAttribute("notifNonLues", notifNonLues);

        // Redirige vers le bon dashboard selon le rôle et charge les données nécessaires
        return switch (user.getRole()) {
            case ETUDIANT -> {
                if(user instanceof Etudiant etudiant){
                    model.addAttribute("etudiant", etudiant);
                }
                model.addAttribute("documents",
                        documentService.getMesDocuments(userDetails.getUsername()));
                model.addAttribute("dernieresCandidatures",
                        candidatureService.getMesCandidatures(userDetails.getUsername()));
                yield "profil/dashboard-etudiant";
            }

            case FREELANCE -> {
                model.addAttribute("documents",
                        documentService.getMesDocuments(userDetails.getUsername()));
                model.addAttribute("dernieresCandidatures",
                        candidatureService.getMesCandidatures(userDetails.getUsername()));
                yield "profil/dashboard-freelance";
            }

            case RECRUTEUR -> {
                // 1. On passe l'utilisateur sous le nom 'entreprise' pour le HTML
                model.addAttribute("entreprise", user);

                // 2. On charge les offres spécifiques de ce recruteur
                var offresRecruteur = offreService.getOffresParEntreprise(user.getId());
                model.addAttribute("offres", offresRecruteur);

                model.addAttribute("candidaturesParOffre",
                        offreService.getCandidaturesParOffre(user.getId()));
                // 3. On crée et on passe les statistiques rapides pour les cartes Bootstrap
                java.util.Map<String, Object> stats = new java.util.HashMap<>();
                stats.put("totalOffres", offresRecruteur.size());
                stats.put("totalCandidatures", offreService.compterCandidaturesPourEntreprise(user.getId()));
                model.addAttribute("stats", stats);

                yield "profil/dashboard-recruteur";
            }

            case ADMIN -> "redirect:/admin/dashboard";
        };
    }

    // MON PROFIL — affichage

    @GetMapping("/mon-profil")
    public String afficherMonProfil(@AuthenticationPrincipal UserDetails userDetails,
                                    Model model)
    {
        if (userDetails == null)
            return "redirect:/auth/connexion";
        User user = profilService.getProfilComplet(userDetails.getUsername());
        model.addAttribute("utilisateur", user);

        long notifNonLues = notificationService.compterNonLues(user);
        model.addAttribute("notifNonLues", notifNonLues);

        return "profil/mon-profil";
    }

    // PROFIL PUBLIC D'UN AUTRE UTILISATEUR

    @GetMapping("/{id}")
    public String afficherProfilPublic(@PathVariable Long id,
                                       Model model,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = profilService.getProfilParId(id);

            // Ne pas afficher les profils suspendus ou archivés
            switch (user.getStatut()) {
                case SUSPENDU, ARCHIVE -> { return "redirect:/offres"; }
                default -> {}
            }

            model.addAttribute("profil", user);

            // Documents validés de l'utilisateur (photos visibles sur le profil)
            model.addAttribute("documentsValides", documentService.getDocumentsValides(user));

            // Info pour savoir si c'est notre propre profil et badge notifications
            if (userDetails != null) {
                User moi = profilService.getProfilComplet(userDetails.getUsername());
                model.addAttribute("estMonProfil", moi.getId().equals(id));
                model.addAttribute("notifNonLues", notificationService.compterNonLues(moi));
                model.addAttribute("utilisateur", moi);
            }

            return "profil/profil-public";

        } catch (IllegalArgumentException e) {
            return "redirect:/offres";
        }
    }


    // MODIFIER MON PROFIL — formulaire

    @GetMapping("/modifier")
    public String afficherFormulaireModification(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = profilService.getProfilComplet(userDetails.getUsername());
        model.addAttribute("utilisateur", user);
        return "profil/modifier-profil";
    }


    // MODIFIER INFOS COMMUNES — traitement

    @PostMapping("/modifier/infos")
    public String modifierInfosCommunes(
            @RequestParam String prenom,
            @RequestParam String nom,
            @RequestParam(required = false) String telephone,
            @RequestParam(required = false) String ville,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            profilService.mettreAJourInfosCommunes(
                    principal.getName(), prenom, nom, telephone, ville);
            redirectAttributes.addFlashAttribute("succes",
                    "Informations mises à jour avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/profil/mon-profil";
    }

    // MODIFIER PROFIL ÉTUDIANT

    @PostMapping("/modifier/etudiant")
    public String modifierProfilEtudiant(
            @RequestParam(required = false) String universite,
            @RequestParam(required = false) String filiere,
            @RequestParam(required = false) String niveauEtude,
            @RequestParam(required = false) String competences,
            @RequestParam(required = false) String disponibilite,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            profilService.mettreAJourProfilEtudiant(
                    principal.getName(), universite, filiere,
                    niveauEtude, competences, disponibilite);
            redirectAttributes.addFlashAttribute("succes",
                    "Profil étudiant mis à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/profil/mon-profil";
    }

    // MODIFIER PROFIL FREELANCE

    @PostMapping("/modifier/freelance")
    public String modifierProfilFreelance(
            @RequestParam(required = false) String specialite,
            @RequestParam(required = false) String portfolioUrl,
            @RequestParam(required = false) String tjm,
            @RequestParam(required = false) String competences,
            @RequestParam(required = false) String disponibilite,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            profilService.mettreAJourProfilFreelance(
                    principal.getName(), specialite, portfolioUrl,
                    tjm, competences, disponibilite);
            redirectAttributes.addFlashAttribute("succes",
                    "Profil freelance mis à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/profil/mon-profil";
    }

    // MODIFIER PROFIL RECRUTEUR
    @PostMapping("/modifier/recruteur")
    public String modifierProfilRecruteur(
            @RequestParam(required = false) String nomEntreprise,
            @RequestParam(required = false) String secteurActivite,
            @RequestParam(required = false) String siteWeb,
            @RequestParam(required = false) String descriptionEntreprise,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            profilService.mettreAJourProfilRecruteur(
                    principal.getName(), nomEntreprise, secteurActivite,
                    siteWeb, descriptionEntreprise);
            redirectAttributes.addFlashAttribute("succes",
                    "Profil entreprise mis à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/profil/mon-profil";
    }


    // UPLOAD PHOTO DE PROFIL
    @PostMapping("/photo")
    public String uploadPhoto(@RequestParam("photo") MultipartFile photo,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            profilService.uploadPhoto(principal.getName(), photo);
            redirectAttributes.addFlashAttribute("succes", "Photo mise à jour.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/profil/mon-profil";
    }


    // PAGE NOTIFICATIONS
    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = profilService.getProfilComplet(userDetails.getUsername());
        model.addAttribute("utilisateur", user);
        model.addAttribute("notifications",
                notificationService.getMesNotifications(user));
        model.addAttribute("notifNonLues",
                notificationService.compterNonLues(user));
        return "notification";
    }

    // RECHERCHE DE PROFILS
    @GetMapping("/recherche")
    public String rechercherProfils(@RequestParam(required = false) String terme,
                                    Model model) {
        if (terme != null && !terme.isBlank()) {
            model.addAttribute("resultats", profilService.rechercherProfils(terme));
            model.addAttribute("terme", terme);
        }
        return "profil/recherche";
    }
}