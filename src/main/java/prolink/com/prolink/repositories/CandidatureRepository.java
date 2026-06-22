package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.Candidature;
import prolink.com.prolink.entities.JobOffer;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.StatutCandidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    // Toutes les candidatures d'un utilisateur — "Mes candidatures"
    List<Candidature> findByCandidatOrderByDateCandidatureDesc(User candidat);

    // Toutes les candidatures pour une offre — vue recruteur
    List<Candidature> findByOffreOrderByDateCandidatureDesc(JobOffer offre);

    // Vérifier si un utilisateur a déjà postulé à une offre
    boolean existsByCandidatAndOffre(User candidat, JobOffer offre);

    // Trouver une candidature spécifique (candidat + offre)
    Optional<Candidature> findByCandidatAndOffre(User candidat, JobOffer offre);

    // Candidatures par statut pour un recruteur
    List<Candidature> findByOffreAndStatut(JobOffer offre, StatutCandidature statut);

    // Nombre de candidatures reçues pour une offre
    long countByOffre(JobOffer offre);
}