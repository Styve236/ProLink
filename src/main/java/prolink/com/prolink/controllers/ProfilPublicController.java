package prolink.com.prolink.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.repositories.UserRepository;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProfilPublicController {
    private final UserRepository userRepository;

    @GetMapping("/membres")
    public String listerMembres(Model model) {
        List<User> tous = userRepository.findAll();
        List<User> actifs = tous.stream()
                .filter(u -> u.getStatut() == StatutCompte.ACTIF)
                .toList();
        model.addAttribute("membres", actifs);
        return "liste-membres";
    }
}
