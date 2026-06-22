package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.Notification;
import prolink.com.prolink.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUtilisateurOrderByDateCreationDesc(User utilisateur);

    List<Notification> findByUtilisateurAndLueFalse(User utilisateur);

    long countByUtilisateurAndLueFalse(User utilisateur);
}