package prolink.com.prolink.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque une méthode de Service comme nécessitant un compte ACTIF.
 *
 * Un utilisateur EN_ATTENTE peut se connecter et naviguer,
 * mais ne peut PAS exécuter les actions annotées ici
 * (postuler, publier une offre, envoyer un message, etc.)
 *
 * Lève CompteNonValideException si le compte n'est pas ACTIF.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiertCompteValide {
}