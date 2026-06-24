package prolink.com.prolink.services;

import lombok.RequiredArgsConstructor;
import prolink.com.prolink.dto.request.OffreDto;
import prolink.com.prolink.entities.JobOffer;
import prolink.com.prolink.entities.Recruteur;
import prolink.com.prolink.enums.StatutOffre;
import prolink.com.prolink.repositories.JobOfferRepository;
import prolink.com.prolink.repositories.RecruteurRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class OffreService {

    private final JobOfferRepository jobOfferRepository;
    private final RecruteurRepository recruteurRepository;
    private final NotificationService notificationService;

    @PreAuthorize("hasRole('RECRUTEUR')")
    public JobOffer publierOffre(OffreDto dto, String emailRecruteur) {
        Recruteur recruteur = recruteurRepository.findByEmail(emailRecruteur)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recruteur introuvable : " + emailRecruteur));

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

    @PreAuthorize("hasRole('RECRUTEUR')")
    public JobOffer modifierOffre(Long offreId, OffreDto dto, String emailRecruteur) {
        JobOffer offre = getOffreParId(offreId);

        if (!offre.getRecruteur().getEmail().equals(emailRecruteur)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à modifier cette offre.");
        }
        if (offre.getStatut() == StatutOffre.APPROUVEE) {
            throw new IllegalStateException(
                    "Une offre approuvée ne peut plus être modifiée. Archivez-la et créez-en une nouvelle.");
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

    @PreAuthorize("hasRole('ADMIN')")
    public JobOffer approuverOffre(Long offreId) {
        JobOffer offre = getOffreParId(offreId);
        offre.setStatut(StatutOffre.APPROUVEE);
        jobOfferRepository.save(offre);
        notificationService.notifierOffreApprouvee(offre);
        return offre;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public JobOffer rejeterOffre(Long offreId, String commentaire) {
        JobOffer offre = getOffreParId(offreId);
        offre.setStatut(StatutOffre.ARCHIVEE);
        offre.setCommentaireAdmin(commentaire);
        return jobOfferRepository.save(offre);
    }

    @Transactional(readOnly = true)
    public List<JobOffer> getOffresPubliques() {
        return jobOfferRepository.findByStatutOrderByDatePublicationDesc(StatutOffre.APPROUVEE);
    }

    @Transactional(readOnly = true)
    public List<JobOffer> rechercherOffres(String terme) {
        if (terme == null || terme.isBlank()) return getOffresPubliques();
        return jobOfferRepository.rechercherOffres(terme.trim());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('RECRUTEUR')")
    public List<JobOffer> getMesOffres(String emailRecruteur) {
        Recruteur recruteur = recruteurRepository.findByEmail(emailRecruteur)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recruteur introuvable : " + emailRecruteur));
        return jobOfferRepository.findByRecruteurOrderByDatePublicationDesc(recruteur);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<JobOffer> getOffresEnAttente() {
        return jobOfferRepository.findByStatut(StatutOffre.EN_ATTENTE);
    }

    @Transactional(readOnly = true)
    public JobOffer getOffreParId(Long id) {
        return jobOfferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Offre introuvable pour l'id : " + id));
    }

    // Dashboard recruteur — liste des offres
    @Transactional(readOnly = true)
    public List<JobOffer> getOffresParEntreprise(Long recruteurId) {
        return jobOfferRepository.findByRecruteurId(recruteurId);
    }

    // Dashboard recruteur — total candidatures reçues
    @Transactional(readOnly = true)
    public long compterCandidaturesPourEntreprise(Long recruteurId) {
        return jobOfferRepository.countCandidaturesByRecruteurId(recruteurId);
    }

    // Dashboard recruteur — Map<offreId, nbCandidatures> sans LazyInit
    @Transactional(readOnly = true)
    public Map<Long, Long> getCandidaturesParOffre(Long recruteurId) {
        Map<Long, Long> result = new HashMap<>();
        jobOfferRepository.countCandidaturesParOffre(recruteurId)
                .forEach(row -> result.put((Long) row[0], (Long) row[1]));
        return result;
    }

    private boolean estAdmin(String email) {
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}