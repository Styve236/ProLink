package prolink.com.prolink.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void verifierConfiguration() {
        if (username == null || username.isBlank()) {
            log.warn("═══════════════════════════════════════════════════");
            log.warn("  SMTP NON CONFIGURÉ — Les emails ne partiront pas");
            log.warn("  Définis les variables d'environnement :");
            log.warn("    MAIL_USERNAME = ton.adresse@gmail.com");
            log.warn("    MAIL_PASSWORD = mot-de-passe-d-application");
            log.warn("  (mot de passe d'application Gmail, pas le tien)");
            log.warn("═══════════════════════════════════════════════════");
        } else {
            log.info("SMTP configuré avec {}", username);
        }
    }

    public boolean envoyerEmail(String to, String sujet, String contenu) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(sujet);
            message.setText(contenu);
            mailSender.send(message);
            log.info("EMAIL envoyé avec succès à {}", masquerEmail(to));
            return true;
        } catch (Exception e) {
            log.error("ERREUR ENVOI EMAIL à {} : {} (type: {})", masquerEmail(to), e.getMessage(), e.getClass().getSimpleName());
            if (e.getCause() != null) {
                log.error("Cause: {} - {}", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            return false;
        }
    }

    public boolean notifierDocumentValide(String email, String nomDocument) {
        return envoyerEmail(email,
                "Document validé - ProLink",
                "Bonjour,\n\nVotre document \"" + nomDocument + "\" a été validé par l'administrateur.\n\nVotre profil est maintenant complet.\n\nCordialement,\nL'équipe ProLink"
        );
    }

    public boolean notifierDocumentRejete(String email, String nomDocument, String commentaire) {
        String motif = commentaire != null && !commentaire.isBlank()
                ? "\nMotif : " + commentaire
                : "";
        return envoyerEmail(email,
                "Document rejeté - ProLink",
                "Bonjour,\n\nVotre document \"" + nomDocument + "\" a été rejeté." + motif + "\n\nVeuillez téléverser un nouveau document conforme.\n\nCordialement,\nL'équipe ProLink"
        );
    }

    public boolean notifierCompteValide(String email, String prenom) {
        return envoyerEmail(email,
                "Compte activé - ProLink",
                "Bonjour " + prenom + ",\n\nVotre compte ProLink a été activé avec succès ! Vous pouvez dès maintenant :\n- Consulter et postuler aux offres\n- Réseauter avec d'autres professionnels\n- Recevoir des notifications en temps réel\n\nCordialement,\nL'équipe ProLink"
        );
    }

    public boolean notifierOffreApprouvee(String email, String titreOffre) {
        return envoyerEmail(email,
                "Offre approuvée - ProLink",
                "Bonjour,\n\nVotre offre \"" + titreOffre + "\" a été approuvée et est maintenant visible publiquement.\n\nCordialement,\nL'équipe ProLink"
        );
    }

    public boolean notifierCandidatureStatut(String email, String offreTitre, String statut) {
        return envoyerEmail(email,
                "Candidature " + statut + " - ProLink",
                "Bonjour,\n\nVotre candidature pour l'offre \"" + offreTitre + "\" a été " + statut + ".\n\nCordialement,\nL'équipe ProLink"
        );
    }

    private String masquerEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return local.charAt(0) + "***@" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    public boolean notifierNouveauMessage(String email, String expediteurNom) {
        return envoyerEmail(email,
                "Nouveau message - ProLink",
                "Bonjour,\n\nVous avez reçu un nouveau message de " + expediteurNom + " sur ProLink.\n\nConnectez-vous pour le consulter.\n\nCordialement,\nL'équipe ProLink"
        );
    }
}
