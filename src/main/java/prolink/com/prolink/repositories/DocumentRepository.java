package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.Document;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.StatutValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUtilisateur(User utilisateur);

    List<Document> findByStatutValidation(StatutValidation statut);

    long countByStatutValidation(StatutValidation statut);
}