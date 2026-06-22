package prolink.com.prolink.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.services.OffreService;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.services.ProfilService;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final OffreService offreService;
    private final ProfilService profilService;

   /* @GetMapping("/page-accueil")
    public String accueil(Model model) {
        model.addAttribute("dernieresOffres", offreService.getOffresPubliques());
        return "index";
    }*/

    @GetMapping("/a-propos")
    public String aPropos() {
        return "pages/a-propos";
    }

    @GetMapping("/contact")
    public String contact() {
        return "pages/contact";
    }

    @GetMapping("/compte/en-attente")
    public String compteEnAttente(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // Si le compte est devenu actif entretemps, on redirige vers le dashboard
        if (userDetails != null) {
            User user = profilService.getProfilComplet(userDetails.getUsername());
            if (user.getStatut() == StatutCompte.ACTIF) {
                return "redirect:/profil/dashboard";
            }
            model.addAttribute("utilisateur", user);
        }
        return "compte-en-attente";
    }
}