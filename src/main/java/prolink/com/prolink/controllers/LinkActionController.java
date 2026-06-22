package prolink.com.prolink.controllers;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import prolink.com.prolink.services.LinkActionService;

@Controller
@RequestMapping("/connexions")
@RequiredArgsConstructor
public class LinkActionController {

    private final LinkActionService linkActionService;

    // Liste des demandes reçues — page dédiée
    @GetMapping("/recues")
    public String demandesRecues(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("demandes", linkActionService.getDemandesRecues(userDetails.getUsername()));
        return "connexions/demandes-recues";
    }

    // Liste des demandes envoyées
    @GetMapping("/envoyees")
    public String demandesEnvoyees(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("demandes", linkActionService.getDemandesEnvoyees(userDetails.getUsername()));
        return "connexions/demandes-envoyees";
    }

    // Envoyer une demande à un utilisateur (depuis son profil public)
    @PostMapping("/demander/{cibleId}")
    public String envoyerDemande(@PathVariable Long cibleId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            linkActionService.envoyerDemande(userDetails.getUsername(), cibleId);
            redirectAttributes.addFlashAttribute("succes", "Demande de connexion envoyée.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/profil/" + cibleId;
    }

    // Accepter une demande reçue
    @PostMapping("/{id}/accepter")
    public String accepter(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        try {
            linkActionService.repondreDemande(id, userDetails.getUsername(), true);
            redirectAttributes.addFlashAttribute("succes", "Demande acceptée. Vous pouvez maintenant discuter.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/connexions/recues";
    }

    // Refuser une demande reçue
    @PostMapping("/{id}/refuser")
    public String refuser(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            linkActionService.repondreDemande(id, userDetails.getUsername(), false);
            redirectAttributes.addFlashAttribute("succes", "Demande refusée.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/connexions/recues";
    }
}