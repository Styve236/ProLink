package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.enums.StatutCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Utilisé par CustomUserDetailsService pour Spring Security
    Optional<User> findByEmail(String email);

    // Utilisé par AdminInitializer au démarrage
    boolean existsByEmailAndRole(String email, RoleUtilisateur role);

    // Vérifier si un email est déjà pris (inscription)
    boolean existsByEmail(String email);

    // Lister tous les utilisateurs par rôle (dashboard admin)
    List<User> findAllByRole(RoleUtilisateur role);

    // Lister les utilisateurs par statut (modération admin)
    List<User> findAllByStatut(StatutCompte statut);

    // Compter par rôle — statistiques dashboard admin
    long countByRole(RoleUtilisateur role);

    // Recherche par nom ou prénom (barre de recherche)
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.nom) LIKE LOWER(CONCAT('%', :terme, '%')) OR " +
            "LOWER(u.prenom) LIKE LOWER(CONCAT('%', :terme, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :terme, '%'))")
    List<User> rechercherParNomOuEmail(@Param("terme") String terme);

    long countByStatut(StatutCompte statut);

    // ── Chargement eager des collections pour éviter LazyInitializationException ──
    // On fait 2 requêtes séparées car Hibernate interdit 2 JOIN FETCH sur 2 List en même temps

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.documents WHERE u.email = :email")
    Optional<User> findByEmailAvecDocuments(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.messagesRecus WHERE u.email = :email")
    Optional<User> findByEmailAvecMessages(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.documents WHERE u.id = :id")
    Optional<User> findByIdAvecDocuments(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.messagesRecus WHERE u.id = :id")
    Optional<User> findByIdAvecMessages(@Param("id") Long id);
}