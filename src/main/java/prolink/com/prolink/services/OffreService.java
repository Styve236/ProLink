package prolink.com.prolink.services;

import lombok.RequiredArgsConstructor;
import prolink.com.prolink.dto.request.OffreDto;
import prolink.com.prolink.entities.JobOffer;
import prolink.com.prolink.entities.Recruteur;
import prolink.com.prolink.enums.StatutOffre;
import prolink.com.prolink.repositories.JobOfferRepository;
import prolink.com.prolink.repositories.OffreRepository;
import prolink.com.prolink.repositories.RecruteurRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service des offres d'emploi.
 *
 * Couvre les cas d'utilisation du module "2. Opportunités" :
 *  - Recruteur : publier une offre
 *  - Visiteur/Utilisateur : rechercher des offres
 *  - Admin : approuver ou rejeter une offre
 *
 * Cycle de vie d'une offre :
 *  Recruteur publie → EN_ATTENTE → Admin approuve → APPROUVEE (visible)
 *                                → Admin rejette  → ARCHIVEE
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OffreService {

    private final JobOfferRepository jobOfferRepository;
    private final RecruteurRepository recruteurRepository;
    private final NotificationService notificationService;
    private final OffreRepository offreRepository;

    // PUBLICATION — Recruteur uniquement

    /**
     * Crée une nouvelle offre — statut EN_ATTENTE par défaut.
     * L'admin doit valider avant qu'elle soit visible publiquement.
     */
    @PreAuthorize("hasRole('RECRUTEUR')")
    public JobOffer publierOffre(OffreDto dto, String emailRecruteur) {

        Recruteur recruteur = recruteurRepository.findByEmail(emailRecruteur)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recruteur introuvable : " + emailRecruteur
                ));

        JobOffer offre = new JobOffer();
        offre.setTitre(dto.getTitre());
        offre.setDescription(dto.getDescription());
        offre.setTypeContrat(dto.getTypeContrat());
        offre.setLieu(dto.getLieu());
        offre.setRemuneration(dto.getRemuneration());
        offre.setCompetencesRequises(dto.getCompetencesRequises());
        offre.setExperienceRequise(dto.getExperienceRequise());
        offre.setDateLimite(dto.getDateLimite());
        offre.setRecruteur(recruteur);
        offre.setStatut(StatutOffre.EN_ATTENTE);

        return jobOfferRepository.save(offre);
    }

    /**
     * Modifie une offre existante — uniquement si elle appartient
     * au recruteur connecté et est encore EN_ATTENTE.
     */
    @PreAuthorize("hasRole('RECRUTEUR')")
    public JobOffer modifierOffre(Long offreId, OffreDto dto, String emailRecruteur) {

        JobOffer offre = getOffreParId(offreId);

        // Vérifie que l'offre appartient bien à ce recruteur
        if (!offre.getRecruteur().getEmail().equals(emailRecruteur)) {
            throw new IllegalStateException(
                    "Vous n'êtes pas autorisé à modifier cette offre."
            );
        }

        // Une offre déjà approuvée ne peut plus être modifiée
        if (offre.getStatut() == StatutOffre.APPROUVEE) {
            throw new IllegalStateException(
                    "Une offre approuvée ne peut plus être modifiée. " +
                            "Archivez-la et créez-en une nouvelle."
            );
        }

        offre.setTitre(dto.getTitre());
        offre.setDescription(dto.getDescription());
        offre.setTypeContrat(dto.getTypeContrat());
        offre.setLieu(dto.getLieu());
        offre.setRemuneration(dto.getRemuneration());
        offre.setCompetencesRequises(dto.getCompetencesRequises());
        offre.setExperienceRequise(dto.getExperienceRequise());
        offre.setDateLimite(dto.getDateLimite());

        return jobOfferRepository.save(offre);
    }

    /**
     * Archive une offre — recruteur ou admin.
     */
    public JobOffer archiverOffre(Long offreId, String emailDemandeur) {

        JobOffer offre = getOffreParId(offreId);

        boolean estProprietaire = offre.getRecruteur().getEmail().equals(emailDemandeur);
        boolean estAdmin = estAdmin(emailDemandeur);

        if (!estProprietaire && !estAdmin) {
            throw new IllegalStateException("Action non autorisée.");
        }

        offre.setStatut(StatutOffre.ARCHIVEE);
        return jobOfferRepository.save(offre);
    }

    // VALIDATION ADMIN

    /**
     * L'admin approuve une offre → elle devient visible publiquement.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public JobOffer approuverOffre(Long offreId) {

        JobOffer offre = getOffreParId(offreId);
        offre.setStatut(StatutOffre.APPROUVEE);
        jobOfferRepository.save(offre);

        // Notifie le recruteur que son offre est en ligne
        notificationService.notifierOffreApprouvee(offre);

        return offre;
    }

    /**
     * L'admin rejette une offre avec un commentaire.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public JobOffer rejeterOffre(Long offreId, String commentaire) {

        JobOffer offre = getOffreParId(offreId);
        offre.setStatut(StatutOffre.ARCHIVEE);
        offre.setCommentaireAdmin(commentaire);
        return jobOfferRepository.save(offre);
    }

    // CONSULTATION
    /**
     * Liste toutes les offres approuvées — page publique.
     */
    @Transactional(readOnly = true)
    public List<JobOffer> getOffresPubliques() {
        return jobOfferRepository.findByStatutOrderByDatePublicationDesc(
                StatutOffre.APPROUVEE
        );
    }

    /**
     * Recherche d'offres par mot-clé — barre de recherche.
     */
    @Transactional(readOnly = true)
    public List<JobOffer> rechercherOffres(String terme) {
        if (terme == null || terme.isBlank()) {
            return getOffresPubliques();
        }
        return jobOfferRepository.rechercherOffres(terme.trim());
    }

    /**
     * Offres d'un recruteur spécifique — espace recruteur.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('RECRUTEUR')")
    public List<JobOffer> getMesOffres(String emailRecruteur) {
        Recruteur recruteur = recruteurRepository.findByEmail(emailRecruteur)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recruteur introuvable : " + emailRecruteur
                ));
        return jobOfferRepository.findByRecruteurOrderByDatePublicationDesc(recruteur);
    }

    /**
     * Offres en attente de validation — dashboard admin.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<JobOffer> getOffresEnAttente() {
        return jobOfferRepository.findByStatut(StatutOffre.EN_ATTENTE);
    }

    /**
     * Détail d'une offre par son ID.
     */
    @Transactional(readOnly = true)
    public JobOffer getOffreParId(Long id) {
        return jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Offre introuvable pour l'id : " + id
                ));
    }

    // UTILITAIRE PRIVÉ
    private boolean estAdmin(String email) {
        // Vérifie si l'utilisateur connecté est admin via le SecurityContext
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public List<JobOffer> getOffresParEntreprise(Long entrepriseId){
        return offreRepository.findByRecruteurId(entrepriseId);
    }
    public long compterCandidaturesPourEntreprise(Long entrepriseId){
        return offreRepository.countCandidaturesByRecruteurId(entrepriseId);
    }

}