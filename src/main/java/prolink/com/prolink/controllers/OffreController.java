package prolink.com.prolink.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import prolink.com.prolink.dto.request.CandidatureDto;
import prolink.com.prolink.dto.request.OffreDto;
import prolink.com.prolink.entities.JobOffer;
import prolink.com.prolink.services.CandidatureService;
import prolink.com.prolink.services.OffreService;

import java.security.Principal;

@Controller
@RequestMapping("/offres")
@RequiredArgsConstructor
public class OffreController {

    private final OffreService offreService;
    private final CandidatureService candidatureService;

    // LISTE PUBLIQUE DES OFFRES
    @GetMapping
    public String listerOffres(Model model,
                               @RequestParam(required = false) String recherche,
                               @AuthenticationPrincipal UserDetails userDetails) {

        // Recherche ou liste complète des offres approuvées
        if (recherche != null && !recherche.isBlank()) {
            model.addAttribute("listeOffres", offreService.rechercherOffres(recherche));
            model.addAttribute("recherche", recherche);
        } else {
            model.addAttribute("listeOffres", offreService.getOffresPubliques());
        }

        // Indique à Thymeleaf si l'utilisateur a déjà postulé à chaque offre
        // Utilisé pour griser le bouton "Postuler"
        if (userDetails != null) {
            model.addAttribute("emailConnecte", userDetails.getUsername());
        }

        return "offres/offre-liste";
    }

    // DÉTAIL D'UNE OFFRE
    @GetMapping("/{id:[0-9]+}")
    public String detailOffre(@PathVariable Long id,
                              Model model,
                              @AuthenticationPrincipal UserDetails userDetails) {
        try {
            JobOffer offre = offreService.getOffreParId(id);
            model.addAttribute("offre", offre);
            model.addAttribute("candidatureDto", new CandidatureDto());

            // Vérifie si l'utilisateur connecté a déjà postulé
            if (userDetails != null) {
                boolean dejaPostule = candidatureService
                        .aDejaPostule(id, userDetails.getUsername());
                model.addAttribute("dejaPostule", dejaPostule);
            }

            return "offres/offre-detail";

        } catch (IllegalArgumentException e) {
            return "redirect:/offres";
        }
    }

    // FORMULAIRE CRÉATION D'OFFRE — Recruteur uniquement
    @GetMapping({"/nouvelle","/offre-form"})
    public String afficherFormulaireCreation(Model model) {
        model.addAttribute("offreDto", new OffreDto());
        return "offres/offre-form";
    }
    // ENREGISTRER UNE OFFRE — Recruteur uniquement
    @PostMapping("/enregistrer")
    public String enregistrerOffre(@Valid @ModelAttribute("offreDto") OffreDto dto,
                                   BindingResult result,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {

        // Erreurs de validation (@NotBlank, etc.)
        if (result.hasErrors()) {
            return "offres/offre-form";
        }

        try {
            offreService.publierOffre(dto, principal.getName());
            redirectAttributes.addFlashAttribute("succes",
                    "Votre offre a été soumise et est en attente de validation.");
            return "redirect:/offres/mes-offres";

        } catch (IllegalArgumentException e) {
            result.rejectValue("erreur", e.getMessage());
            return "offres/offre-form";
        }
    }

    // MES OFFRES — Recruteur uniquement
    @GetMapping("/mes-offres")
    public String voirMesOffres(Model model, Principal principal) {
        try {
            model.addAttribute("offres",
                    offreService.getMesOffres(principal.getName()));
            return "offres/mes-offres";

        } catch (Exception e) {
            return "redirect:/offres";
        }
    }

    // POSTULER À UNE OFFRE — Etudiant ou Freelance uniquement
    @PostMapping("/{id}/postuler")
    public String postulerAOffre(@PathVariable Long id,
                                 @Valid @ModelAttribute("candidatureDto") CandidatureDto dto,
                                 BindingResult result,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Le message de motivation est obligatoire (50 caractères minimum).");
            return "redirect:/offres/" + id;
        }

        try {
            candidatureService.postuler(id, dto, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("succes",
                    "Votre candidature a bien été envoyée !");

        } catch (IllegalStateException e) {
            // Doublon, offre non disponible, etc.
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }

        return "redirect:/offres/" + id;
    }

    // MES CANDIDATURES — Etudiant ou Freelance uniquement
    @GetMapping("/mes-candidatures")
    public String afficherMesCandidatures(Model model,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("listeCandidatures",
                candidatureService.getMesCandidatures(userDetails.getUsername()));
        return "offres/mes-candidatures";
    }

    // CANDIDATS D'UNE OFFRE — Recruteur uniquement

    @GetMapping("/{id}/candidatures")
    public String voirCandidatsParOffre(@PathVariable Long id,
                                        Model model,
                                        Principal principal) {
        try {
            model.addAttribute("offre", offreService.getOffreParId(id));
            model.addAttribute("candidatures",
                    candidatureService.getCandidaturesDuneOffre(id, principal.getName()));
            return "recruteur/liste-candidats";

        } catch (IllegalStateException e) {
            return "redirect:/offres/mes-offres";
        }
    }
    // ACCEPTER / REFUSER UNE CANDIDATURE — Recruteur uniquement
    @PostMapping("/candidatures/{id}/accepter")
    public String accepterCandidature(@PathVariable Long id,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            candidatureService.accepterCandidature(id, principal.getName());
            redirectAttributes.addFlashAttribute("succes", "Candidature acceptée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/offres/mes-offres";
    }

    @PostMapping("/candidatures/{id}/refuser")
    public String refuserCandidature(@PathVariable Long id,
                                     @RequestParam(required = false) String commentaire,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            candidatureService.refuserCandidature(id, principal.getName(), commentaire);
            redirectAttributes.addFlashAttribute("succes", "Candidature refusée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/offres/mes-offres";
    }

    // ARCHIVER UNE OFFRE — Recruteur ou Admin
    @PostMapping("/{id}/archiver")
    public String archiverOffre(@PathVariable Long id,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            offreService.archiverOffre(id, principal.getName());
            redirectAttributes.addFlashAttribute("succes", "Offre archivée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/offres/mes-offres";
    }
}