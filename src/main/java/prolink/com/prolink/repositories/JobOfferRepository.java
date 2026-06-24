package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.JobOffer;
import prolink.com.prolink.entities.Recruteur;
import prolink.com.prolink.enums.StatutOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import prolink.com.prolink.entities.Candidature;


import java.util.List;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    // Toutes les offres approuvées — page publique
    List<JobOffer> findByStatutOrderByDatePublicationDesc(StatutOffre statut);

    // Offres d'un recruteur spécifique — espace recruteur
    List<JobOffer> findByRecruteurOrderByDatePublicationDesc(Recruteur recruteur);

    // APRÈS ✅ — charge le recruteur en même temps
    @Query("SELECT o FROM JobOffer o JOIN FETCH o.recruteur WHERE o.statut = :statut")
    List<JobOffer> findByStatut(@Param("statut") StatutOffre statut);

    // Recherche par mot-clé dans titre ou description
    @Query("SELECT o FROM JobOffer o WHERE o.statut = 'APPROUVEE' AND (" +
            "LOWER(o.titre) LIKE LOWER(CONCAT('%', :terme, '%')) OR " +
            "LOWER(o.description) LIKE LOWER(CONCAT('%', :terme, '%')) OR " +
            "LOWER(o.lieu) LIKE LOWER(CONCAT('%', :terme, '%')))")
    List<JobOffer> rechercherOffres(@Param("terme") String terme);

    // Recherche par type de contrat
    List<JobOffer> findByStatutAndTypeContratIgnoreCase(StatutOffre statut, String typeContrat);

    // Nombre d'offres en attente — badge admin
    long countByStatut(StatutOffre statut);

    @Query("SELECT DISTINCT o FROM JobOffer o LEFT JOIN FETCH o.candidatures WHERE o.recruteur.id = :recruteurId")
    List<JobOffer> findByRecruteurId(@Param("recruteurId") Long recruteurId);

    // Compter le total des candidatures reçues pour ce recruteur
    @Query("SELECT COUNT(c) FROM Candidature c WHERE c.offre.recruteur.id = :recruteurId")
    long countCandidaturesByRecruteurId(@Param("recruteurId") Long recruteurId);

    // Nombre de candidatures par offre — évite le LazyInitializationException
    @Query("SELECT o.id, COUNT(c) FROM JobOffer o LEFT JOIN o.candidatures c WHERE o.recruteur.id = :recruteurId GROUP BY o.id")
    List<Object[]> countCandidaturesParOffre(@Param("recruteurId") Long recruteurId);
}