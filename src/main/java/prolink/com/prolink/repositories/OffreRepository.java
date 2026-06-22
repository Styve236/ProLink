package prolink.com.prolink.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import prolink.com.prolink.entities.JobOffer;
import java.util.List;

public interface OffreRepository extends JpaRepository<JobOffer, Long> {

    // Récupérer les offres d'un recruteur spécifique
    List<JobOffer> findByRecruteurId(Long recruteurId);

    // Compter le total des candidatures reçues pour ce recruteur
    @Query("SELECT COUNT(c) FROM Candidature c WHERE c.offre.recruteur.id = :recruteurId")
    long countCandidaturesByRecruteurId(@Param("recruteurId") Long recruteurId);
}