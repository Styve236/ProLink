package prolink.com.prolink.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.exceptions.CompteNonValideException;
import prolink.com.prolink.repositories.UserRepository;

@Aspect
@Component
public class CompteValideAspect {

    private final UserRepository userRepository;

    public CompteValideAspect(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Before("@annotation(prolink.com.prolink.security.RequiertCompteValide)")
    public void verifierStatutCompte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new CompteNonValideException("Vous devez être connecté.");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CompteNonValideException("Utilisateur introuvable."));

        if (user.getStatut() != StatutCompte.ACTIF) {
            throw new CompteNonValideException(
                    "Votre compte est en attente de validation par un administrateur. " +
                            "Cette action sera disponible une fois votre compte validé."
            );
        }
    }
}