package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.Message;
import prolink.com.prolink.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByDestinataireOrderByDateEnvoiDesc(User destinataire);

    List<Message> findByExpediteurOrderByDateEnvoiDesc(User expediteur);

    // Conversation entre deux utilisateurs — dans les deux sens
    @Query("SELECT m FROM Message m WHERE " +
            "(m.expediteur.id = :idA AND m.destinataire.id = :idB) OR " +
            "(m.expediteur.id = :idB AND m.destinataire.id = :idA) " +
            "ORDER BY m.dateEnvoi ASC")
    List<Message> findConversation(@Param("idA") Long idA, @Param("idB") Long idB);

    long countByDestinataireAndLuFalse(User destinataire);

    // Utilisé par AdminService pour suppression propre
    void deleteByExpediteur(User expediteur);
    void deleteByDestinataire(User destinataire);
}