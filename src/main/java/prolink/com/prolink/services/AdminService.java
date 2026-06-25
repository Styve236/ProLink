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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final MessageRepository messageRepository;
    private final CandidatureRepository candidatureRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public AdminService(UserRepository userRepository,
                        DocumentRepository documentRepository,
                        MessageRepository messageRepository,
                        CandidatureRepository candidatureRepository,
                        NotificationService notificationService,
                        EmailService emailService) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.messageRepository = messageRepository;
        this.candidatureRepository = candidatureRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
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
    public Map<String, Object> validerCompte(Long userId, int trustScore, String commentaire) {
        User user = getUtilisateurParId(userId);
        user.setStatut(StatutCompte.ACTIF);
        user.setTrustScore(Math.max(0, Math.min(100, trustScore)));
        userRepository.save(user);
        boolean emailEnvoye = emailService.notifierCompteValide(user.getEmail(), user.getPrenom());
        if (!emailEnvoye) {
            log.warn("Email de validation de compte non envoyé à {} (SMTP non configuré ?)", user.getEmail());
        }
        Map<String, Object> resultat = new HashMap<>();
        resultat.put("user", user);
        resultat.put("emailEnvoye", emailEnvoye);
        return resultat;
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

    public Map<String, Object> validerDocument(Long documentId) {
        Document doc = getDocumentParId(documentId);
        doc.setStatutValidation(StatutValidation.VALIDE);
        doc.setDateValidation(LocalDateTime.now());
        documentRepository.save(doc);
        boolean emailEnvoye = notificationService.notifierDocumentValide(
                doc.getUtilisateur(), doc.getNomFichier());
        if (!emailEnvoye) {
            log.warn("Email de validation de document non envoyé à {} (SMTP non configuré ?)", doc.getUtilisateur().getEmail());
        }
        Map<String, Object> resultat = new HashMap<>();
        resultat.put("document", doc);
        resultat.put("emailEnvoye", emailEnvoye);
        return resultat;
    }

    public Map<String, Object> rejeterDocument(Long documentId, String commentaire) {
        Document doc = getDocumentParId(documentId);
        doc.setStatutValidation(StatutValidation.REJETE);
        doc.setCommentaireAdmin(commentaire);
        doc.setDateValidation(LocalDateTime.now());
        documentRepository.save(doc);
        boolean emailEnvoye = notificationService.notifierDocumentRejete(
                doc.getUtilisateur(), doc.getNomFichier(), commentaire);
        if (!emailEnvoye) {
            log.warn("Email de rejet de document non envoyé à {} (SMTP non configuré ?)", doc.getUtilisateur().getEmail());
        }
        Map<String, Object> resultat = new HashMap<>();
        resultat.put("document", doc);
        resultat.put("emailEnvoye", emailEnvoye);
        return resultat;
    }

    // UTILITAIRE PRIVÉ
    private Document getDocumentParId(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Document introuvable : " + id));
    }
}