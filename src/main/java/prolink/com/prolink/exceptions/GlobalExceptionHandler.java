package prolink.com.prolink.exceptions;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestion globale des exceptions applicatives.
 *
 * CompteNonValideException : levée par l'aspect @RequiertCompteValide
 * quand un utilisateur dont le compte n'est pas ACTIF tente une action
 * d'écriture. On redirige vers le dashboard avec un message clair,
 * au lieu d'une erreur 500.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CompteNonValideException.class)
    public String gererCompteNonValide(CompteNonValideException e,
                                       RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        return "redirect:/profil/dashboard";
    }
}