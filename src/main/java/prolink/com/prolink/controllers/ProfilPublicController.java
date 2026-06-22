package prolink.com.prolink.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.repositories.UserRepository;

@Controller
@RequiredArgsConstructor
public class ProfilPublicController {
    private final UserRepository userRepository;

    // Lister les membres
    @GetMapping("/membres")
    public String listerMembres(Model model) {
        model.addAttribute("membres", userRepository.findAll());
        return "liste-membres";
    }
/*
    // Voir profil d'un autre
    @GetMapping("/profil/voir/{id}")
    public String voirProfilPublic(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id).orElseThrow();
        model.addAttribute("utilisateur", user);
        return "profil/profil-public";
    }*/
}