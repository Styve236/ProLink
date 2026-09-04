package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.BlogPost;
import prolink.com.prolink.entities.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    List<Commentaire> findByPostOrderByDateCreationDesc(BlogPost post);

    long countByPost(BlogPost post);
}
