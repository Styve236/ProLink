package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.BlogPost;
import prolink.com.prolink.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    // Incrément atomique du compteur de vues (concurrent-safe)
    @Modifying
    @Query("UPDATE BlogPost p SET p.nbVues = p.nbVues + 1 WHERE p.id = :id")
    void incrementerNbVues(@Param("id") Long id);

    // Vérifie si l'utilisateur (email) est l'auteur de ce post
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END " +
            "FROM BlogPost p WHERE p.id = :id AND p.auteur.email = :email")
    Boolean estAuteur(@Param("id") Long id, @Param("email") String email);

    // Tous les posts, du plus récent au plus ancien — page publique
    List<BlogPost> findAllByOrderByDatePublicationDesc();

    // Pagination — page publique (compteurs chargés séparément dans le service)
    Page<BlogPost> findAllByOrderByDatePublicationDesc(Pageable pageable);

    // Recherche par mot-clé dans le titre ou le contenu (paginée) + compteurs
    @Query("SELECT p FROM BlogPost p WHERE " +
            "LOWER(p.titre) LIKE LOWER(CONCAT('%', :terme, '%')) OR " +
            "LOWER(p.contenu) LIKE LOWER(CONCAT('%', :terme, '%')) ORDER BY p.datePublication DESC")
    Page<BlogPost> rechercher(@Param("terme") String terme, Pageable pageable);

    // Nombre de commentaires par post (évite de charger la collection)
    @Query("SELECT c.post.id, COUNT(c) FROM Commentaire c GROUP BY c.post.id")
    List<Object[]> countCommentairesParPost();

    // Nombre de réactions par post (évite de charger la collection)
    @Query("SELECT r.post.id, COUNT(r) FROM Reaction r GROUP BY r.post.id")
    List<Object[]> countReactionsParPost();

    // Posts d'un auteur — espace personnel
    List<BlogPost> findByAuteur_IdOrderByDatePublicationDesc(Long auteurId);

    // Charge le post avec auteur + commentaires (évite LazyInitializationException)
    @Query("SELECT DISTINCT p FROM BlogPost p " +
            "LEFT JOIN FETCH p.auteur " +
            "LEFT JOIN FETCH p.commentaires c LEFT JOIN FETCH c.auteur " +
            "WHERE p.id = :id")
    java.util.Optional<BlogPost> findDetailById(@Param("id") Long id);

    // Nombre de réactions (likes) d'un post précis — évite de charger la collection
    @Query("SELECT COUNT(r) FROM Reaction r WHERE r.post.id = :id")
    long countReactionsByPostId(@Param("id") Long id);

    // Comptage par période — rapport admin
    long countByDatePublicationBetween(LocalDateTime debut, LocalDateTime fin);

    // Suppression des posts d'un auteur (nettoyage lors de la suppression d'un utilisateur)
    void deleteByAuteur(User auteur);
}
