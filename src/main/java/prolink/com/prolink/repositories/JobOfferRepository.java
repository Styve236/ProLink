package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.JobOffer;
import prolink.com.prolink.entities.Recruteur;
import prolink.com.prolink.enums.StatutOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    // Toutes les offres approuvées — page publique
    List<JobOffer> findByStatutOrderByDatePublicationDesc(StatutOffre statut);

    // Offres d'un recruteur spécifique — espace recruteur
    List<JobOffer> findByRecruteurOrderByDatePublicationDesc(Recruteur recruteur);

    // Offres en attente de validation — dashboard admin
    List<JobOffer> findByStatut(StatutOffre statut);

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
}