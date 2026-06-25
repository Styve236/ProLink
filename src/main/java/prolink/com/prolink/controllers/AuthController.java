package prolink.com.prolink.controllers;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import prolink.com.prolink.dto.request.InscriptionDto;
import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.services.AuthService;
import prolink.com.prolink.services.OffreService;

@Controller
public class AuthController {

    private final AuthService authService;
    private final OffreService offreService;

    public AuthController(AuthService authService, OffreService offreService) {
        this.authService = authService;
        this.offreService = offreService;
    }

    @GetMapping({"/", "/index"})
    public String accueil(@AuthenticationPrincipal UserDetails userDetails, Model model)
    {
        if (userDetails != null){
            return "redirect:/profil/dashboard";
        }

        model.addAttribute("dernieresOffres", offreService.getOffresPubliques());
        return "index";
    }
    @GetMapping("/auth/connexion")
    public String afficherConnexion(
            @RequestParam(required = false) String erreur,
            @RequestParam(required = false) String deconnecte,
            @RequestParam(required = false) String redirect,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails != null) {
            return "redirect:/profil/dashboard";
        }

        if (erreur != null) {
            model.addAttribute("erreur", "Email ou mot de passe incorrect.");
        }
        if (deconnecte != null) {
            model.addAttribute("succes", "Vous avez été déconnecté avec succès.");
        }
        if (redirect != null && !redirect.isBlank()) {
            model.addAttribute("redirect", redirect);
        }

        return "auth/connexion";
    }

    @GetMapping("/auth/inscription")
    public String afficherInscription(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails != null) {
            return "redirect:/profil/dashboard";
        }

        model.addAttribute("inscriptionDto", new InscriptionDto());
        model.addAttribute("roles", RoleUtilisateur.values());
        return "auth/inscription";
    }

    @PostMapping("/auth/inscription")
    public String traiterInscription(
            @Valid @ModelAttribute("inscriptionDto") InscriptionDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("roles", RoleUtilisateur.values());
            return "auth/inscription";
        }

        try {
            authService.inscrire(dto);
            redirectAttributes.addFlashAttribute("succes",
                    "Compte créé avec succès ! Votre compte est en attente de validation. " +
                            "Vous pouvez vous connecter dès validation par l'administrateur.");
            return "redirect:/auth/connexion";

        } catch (IllegalArgumentException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("roles", RoleUtilisateur.values());
            return "auth/inscription";
        }
    }
}