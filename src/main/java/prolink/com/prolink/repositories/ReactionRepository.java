package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.BlogPost;
import prolink.com.prolink.entities.Reaction;
import prolink.com.prolink.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    // Existe-t-il déjà un like de cet utilisateur sur ce post ?
    boolean existsByPostAndUtilisateur(BlogPost post, User utilisateur);

    // Récupère le like pour le retirer (toggle)
    Optional<Reaction> findByPostAndUtilisateur(BlogPost post, User utilisateur);

    long countByPost(BlogPost post);

    // Suppression des réactions d'un utilisateur (nettoyage lors de la suppression d'un utilisateur)
    void deleteByUtilisateur(User utilisateur);
}
