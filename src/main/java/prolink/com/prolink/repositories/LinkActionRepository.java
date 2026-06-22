package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.LinkAction;
import prolink.com.prolink.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository des demandes de connexion entre utilisateurs.
 *
 * IMPORTANT : LinkAction ne gère PAS les candidatures.
 * Les candidatures sont dans CandidatureRepository.
 *
 * LinkAction gère uniquement :
 *  - Demande de connexion entre deux utilisateurs
 *  - Statuts : EN_ATTENTE, ACCEPTEE, REFUSEE
 *
 * Propriétés disponibles dans LinkAction :
 *  - demandeur (User)
 *  - cible     (User)
 *  - statut    (String)
 *  - dateDemande
 *  - dateReponse
 */
@Repository
public interface LinkActionRepository extends JpaRepository<LinkAction, Long> {

    // Demandes envoyées par un utilisateur
    List<LinkAction> findByDemandeurOrderByDateDemandeDesc(User demandeur);

    // Demandes reçues par un utilisateur
    List<LinkAction> findByCibleOrderByDateDemandeDesc(User cible);

    // Demandes en attente reçues — badge de notification
    List<LinkAction> findByCibleAndStatut(User cible, String statut);

    // Vérifier si une demande existe déjà entre deux utilisateurs
    boolean existsByDemandeurAndCible(User demandeur, User cible);

    // Trouver la demande entre deux utilisateurs spécifiques
    Optional<LinkAction> findByDemandeurAndCible(User demandeur, User cible);

    // Trouver toutes les connexions acceptées d'un utilisateur
    @Query("SELECT l FROM LinkAction l WHERE " +
            "(l.demandeur.id = :userId OR l.cible.id = :userId) " +
            "AND l.statut = 'ACCEPTEE'")
    List<LinkAction> findConnexionsAcceptees(@Param("userId") Long userId);

    // Compter les demandes en attente reçues — badge navbar
    long countByCibleAndStatut(User cible, String statut);

    // Vérifier si deux utilisateurs sont connectés (dans un sens ou l'autre)
    @Query("SELECT COUNT(l) > 0 FROM LinkAction l WHERE " +
            "((l.demandeur.id = :idA AND l.cible.id = :idB) OR " +
            "(l.demandeur.id = :idB AND l.cible.id = :idA)) " +
            "AND l.statut = 'ACCEPTEE'")
    boolean sontConnectes(@Param("idA") Long idA, @Param("idB") Long idB);
}