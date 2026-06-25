package prolink.com.prolink.services;

import prolink.com.prolink.dto.request.CandidatureDto;
import prolink.com.prolink.entities.Candidature;
import prolink.com.prolink.entities.JobOffer;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.StatutCandidature;
import prolink.com.prolink.enums.StatutOffre;
import prolink.com.prolink.repositories.CandidatureRepository;
import prolink.com.prolink.repositories.JobOfferRepository;
import prolink.com.prolink.repositories.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service des candidatures.
 *
 * Couvre les cas d'utilisation du module "2. Opportunités" :
 *  - Etudiant / Freelance : postuler à une offre
 *  - Recruteur : voir les candidats, accepter ou refuser
 *
 * Règles métier :
 *  - On ne peut postuler qu'à une offre APPROUVEE
 *  - On ne peut postuler qu'une seule fois par offre
 *  - Seul le recruteur propriétaire de l'offre peut traiter les candidatures
 */
@Service
@Transactional
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public CandidatureService(CandidatureRepository candidatureRepository,
                              JobOfferRepository jobOfferRepository,
                              UserRepository userRepository,
                              NotificationService notificationService,
                              EmailService emailService) {
        this.candidatureRepository = candidatureRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    // POSTULER — Etudiant ou Freelance uniquement

    /**
     * Soumet une candidature à une offre.
     *
     * @param offreId   ID de l'offre ciblée
     * @param dto       Message de motivation
     * @param emailCandidat Email du postulant connecté
     */
    @PreAuthorize("hasAnyRole('ETUDIANT', 'FREELANCE')")
    public Candidature postuler(Long offreId, CandidatureDto dto, String emailCandidat) {

        User candidat = userRepository.findByEmail(emailCandidat)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Candidat introuvable : " + emailCandidat
                ));

        JobOffer offre = jobOfferRepository.findById(offreId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Offre introuvable : " + offreId
                ));

        // Règle 1 — L'offre doit être approuvée
        if (offre.getStatut() != StatutOffre.APPROUVEE) {
            throw new IllegalStateException(
                    "Cette offre n'est plus disponible."
            );
        }

        // Règle 2 — Pas de doublon de candidature
        if (candidatureRepository.existsByCandidatAndOffre(candidat, offre)) {
            throw new IllegalStateException(
                    "Vous avez déjà postulé à cette offre."
            );
        }

        // Règle 3 — Un recruteur ne peut pas postuler à sa propre offre
        if (offre.getRecruteur().getEmail().equals(emailCandidat)) {
            throw new IllegalStateException(
                    "Un recruteur ne peut pas postuler à ses propres offres."
            );
        }

        Candidature candidature = new Candidature();
        candidature.setCandidat(candidat);
        candidature.setOffre(offre);
        candidature.setMessageMotivation(dto.getMessageMotivation());
        candidature.setStatut(StatutCandidature.EN_ATTENTE);

        candidatureRepository.save(candidature);

        // Notifie le recruteur qu'il a reçu une nouvelle candidature
        notificationService.notifierNouvelleCandidature(candidature);

        return candidature;
    }

    // TRAITEMENT — Recruteur uniquement

    /**
     * Le recruteur accepte une candidature.
     */
    @PreAuthorize("hasRole('RECRUTEUR')")
    public Candidature accepterCandidature(Long candidatureId, String emailRecruteur) {

        Candidature candidature = getCandidatureParId(candidatureId);
        verifierProprietaireOffre(candidature, emailRecruteur);

        candidature.setStatut(StatutCandidature.ACCEPTEE);
        candidatureRepository.save(candidature);

        notificationService.notifierCandidatureAcceptee(candidature);
        emailService.notifierCandidatureStatut(
                candidature.getCandidat().getEmail(),
                candidature.getOffre().getTitre(),
                "acceptée"
        );

        return candidature;
    }

    @PreAuthorize("hasRole('RECRUTEUR')")
    public Candidature refuserCandidature(Long candidatureId,
                                          String emailRecruteur,
                                          String commentaire) {

        Candidature candidature = getCandidatureParId(candidatureId);
        verifierProprietaireOffre(candidature, emailRecruteur);

        candidature.setStatut(StatutCandidature.REFUSEE);
        candidature.setCommentaireRecruteur(commentaire);
        candidatureRepository.save(candidature);

        notificationService.notifierCandidatureRefusee(candidature);
        emailService.notifierCandidatureStatut(
                candidature.getCandidat().getEmail(),
                candidature.getOffre().getTitre(),
                "refusée"
        );

        return candidature;
    }

    // CONSULTATION

    /**
     * Mes candidatures — espace étudiant/freelance.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ETUDIANT', 'FREELANCE')")
    public List<Candidature> getMesCandidatures(String emailCandidat) {
        User candidat = userRepository.findByEmail(emailCandidat)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Candidat introuvable : " + emailCandidat
                ));
        return candidatureRepository.findByCandidatOrderByDateCandidatureDesc(candidat);
    }

    /**
     * Candidatures reçues pour une offre — espace recruteur.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('RECRUTEUR')")
    public List<Candidature> getCandidaturesDuneOffre(Long offreId,
                                                      String emailRecruteur) {
        JobOffer offre = jobOfferRepository.findById(offreId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Offre introuvable : " + offreId
                ));

        // Vérifie que l'offre appartient au recruteur
        if (!offre.getRecruteur().getEmail().equals(emailRecruteur)) {
            throw new IllegalStateException("Accès non autorisé à ces candidatures.");
        }

        return candidatureRepository.findByOffreOrderByDateCandidatureDesc(offre);
    }

    /**
     * Vérifie si l'utilisateur a déjà postulé — utile dans les templates
     * Thymeleaf pour afficher/masquer le bouton "Postuler".
     */
    @Transactional(readOnly = true)
    public boolean aDejaPostule(Long offreId, String emailCandidat) {
        return userRepository.findByEmail(emailCandidat)
                .map(candidat -> jobOfferRepository.findById(offreId)
                        .map(offre -> candidatureRepository
                                .existsByCandidatAndOffre(candidat, offre))
                        .orElse(false))
                .orElse(false);
    }

    // UTILITAIRES PRIVÉS
    private Candidature getCandidatureParId(Long id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Candidature introuvable : " + id
                ));
    }

    private void verifierProprietaireOffre(Candidature candidature,
                                           String emailRecruteur) {
        if (!candidature.getOffre().getRecruteur().getEmail().equals(emailRecruteur)) {
            throw new IllegalStateException(
                    "Vous n'êtes pas autorisé à traiter cette candidature."
            );
        }
    }
}