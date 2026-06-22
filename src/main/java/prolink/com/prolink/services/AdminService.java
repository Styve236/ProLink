package prolink.com.prolink.services;

import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import prolink.com.prolink.entities.Document;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.enums.StatutValidation;
import prolink.com.prolink.repositories.CandidatureRepository;
import prolink.com.prolink.repositories.DocumentRepository;
import prolink.com.prolink.repositories.MessageRepository;
import prolink.com.prolink.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final MessageRepository messageRepository;
    private final CandidatureRepository candidatureRepository;
    private final NotificationService notificationService;

    public AdminService(UserRepository userRepository,
                        DocumentRepository documentRepository,
                        MessageRepository messageRepository,
                        CandidatureRepository candidatureRepository,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.messageRepository = messageRepository;
        this.candidatureRepository = candidatureRepository;
        this.notificationService = notificationService;
    }

    // STATS DASHBOARD
    public Map<String, Long> getStatsDashboard() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUtilisateurs", userRepository.count());
        stats.put("etudiants",  userRepository.countByRole(RoleUtilisateur.ETUDIANT));
        stats.put("freelances", userRepository.countByRole(RoleUtilisateur.FREELANCE));
        stats.put("recruteurs", userRepository.countByRole(RoleUtilisateur.RECRUTEUR));
        stats.put("enAttente",  userRepository.countByStatut(StatutCompte.EN_ATTENTE));
        stats.put("documentsEnAttente",
                documentRepository.countByStatutValidation(StatutValidation.EN_ATTENTE));
        return stats;
    }

    // GESTION UTILISATEURS
    public List<User> getTousLesUtilisateurs() {
        return userRepository.findAll();
    }

    public List<User> getUtilisateursParRole(RoleUtilisateur role) {
        return userRepository.findAllByRole(role);
    }

    public User getUtilisateurParId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilisateur introuvable : " + id));
    }

    // VALIDATION DE COMPTE
    public User validerCompte(Long userId, int trustScore, String commentaire) {
        User user = getUtilisateurParId(userId);
        user.setStatut(StatutCompte.ACTIF);
        // Borne le score entre 0 et 100
        user.setTrustScore(Math.max(0, Math.min(100, trustScore)));
        return userRepository.save(user);
    }

    // CHANGER STATUT — bannir / archiver / réactiver
    public User changerStatutCompte(Long userId, StatutCompte statut, String raison) {
        User user = getUtilisateurParId(userId);

        if (user.getRole() == RoleUtilisateur.ADMIN) {
            throw new IllegalStateException(
                    "Impossible de modifier le statut d'un administrateur.");
        }

        user.setStatut(statut);
        return userRepository.save(user);
    }

    // TRUST SCORE
    public User mettreAJourTrustScore(Long userId, int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(
                    "Le Trust Score doit être entre 0 et 100.");
        }
        User user = getUtilisateurParId(userId);
        user.setTrustScore(score);
        userRepository.save(user);

        // Appel direct à notificationService — pas de méthode "creer" ici
        notificationService.notifierTrustScoreMisAJour(user, score);
        return user;
    }

    // SUPPRESSION COMPLÈTE D'UN UTILISATEUR
   public void supprimerUtilisateur(Long userId) {
        User user = getUtilisateurParId(userId);

        if (user.getRole() == RoleUtilisateur.ADMIN) {
            throw new IllegalStateException(
                    "Impossible de supprimer un administrateur.");
        }

        messageRepository.deleteByExpediteur(user);
        messageRepository.deleteByDestinataire(user);
        userRepository.delete(user);
    }

    // GESTION DES DOCUMENTS
    public List<Document> getDocumentsEnAttente() {
        return documentRepository.findByStatutValidation(StatutValidation.EN_ATTENTE);
    }

    public Document validerDocument(Long documentId) {
        Document doc = getDocumentParId(documentId);
        doc.setStatutValidation(StatutValidation.VALIDE);
        doc.setDateValidation(LocalDateTime.now());
        documentRepository.save(doc);
        // Appel via notificationService — pas creer() directement
        notificationService.notifierDocumentValide(
                doc.getUtilisateur(), doc.getNomFichier());
        return doc;
    }

    public Document rejeterDocument(Long documentId, String commentaire) {
        Document doc = getDocumentParId(documentId);
        doc.setStatutValidation(StatutValidation.REJETE);
        doc.setCommentaireAdmin(commentaire);
        doc.setDateValidation(LocalDateTime.now());
        documentRepository.save(doc);
        notificationService.notifierDocumentRejete(
                doc.getUtilisateur(), doc.getNomFichier());
        return doc;
    }

    // UTILITAIRE PRIVÉ
    private Document getDocumentParId(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Document introuvable : " + id));
    }
}