package prolink.com.prolink.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import prolink.com.prolink.entities.Document;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.StatutValidation;
import prolink.com.prolink.enums.TypeDocument;
import prolink.com.prolink.repositories.DocumentRepository;
import prolink.com.prolink.repositories.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des documents/justificatifs.
 *
 * Couvre le cas d'utilisation "Téléverser justificatif" du diagramme.
 * Types acceptés : carte étudiant, Kbis entreprise, CV, portfolio.
 *
 * Flux complet :
 *  1. Utilisateur upload via /profil/documents/upload
 *  2. Fichier stocké sur disque, entité Document créée (statut EN_ATTENTE)
 *  3. Admin consulte /admin/documents, valide ou rejette
 *  4. Notification envoyée à l'utilisateur (déjà géré par AdminService)
 */
@Service
@Transactional
public class DocumentService {

    private static final String UPLOAD_DIR = "uploads/documents/";
    private static final long TAILLE_MAX = 5 * 1024 * 1024; // 5 Mo
    private static final List<String> EXTENSIONS_AUTORISEES =
            List.of("pdf", "jpg", "jpeg", "png");

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    // ----------------------------------------------------------------
    // UPLOAD
    // ----------------------------------------------------------------

    /**
     * Téléverse un document justificatif pour l'utilisateur connecté.
     *
     * @param emailUtilisateur email de l'utilisateur connecté
     * @param fichier le fichier envoyé via le formulaire
     * @param type le type de document (CARTE_ETUDIANT, KBIS_ENTREPRISE, etc.)
     */
    public Document televerserDocument(String emailUtilisateur,
                                       MultipartFile fichier,
                                       TypeDocument type) throws IOException {

        if (fichier.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }

        if (fichier.getSize() > TAILLE_MAX) {
            throw new IllegalArgumentException(
                    "Le fichier dépasse la taille maximale autorisée (5 Mo).");
        }

        String extension = obtenirExtension(fichier.getOriginalFilename());
        if (!EXTENSIONS_AUTORISEES.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Format non autorisé. Formats acceptés : PDF, JPG, PNG.");
        }

        User utilisateur = userRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilisateur introuvable : " + emailUtilisateur));

        // Nom de fichier unique pour éviter les collisions et les écrasements
        String nomFichierStocke = UUID.randomUUID() + "." + extension;

        Path dossier = Paths.get(UPLOAD_DIR);
        Files.createDirectories(dossier);

        Path destination = dossier.resolve(nomFichierStocke);
        Files.copy(fichier.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        Document document = new Document();
        document.setUtilisateur(utilisateur);
        document.setNomFichier(fichier.getOriginalFilename());
        document.setCheminFichier(UPLOAD_DIR + nomFichierStocke);
        document.setTypeMime(fichier.getContentType());
        document.setTaille(fichier.getSize());
        document.setTypeDocument(type);
        document.setStatutValidation(StatutValidation.EN_ATTENTE);

        return documentRepository.save(document);
    }

    // ----------------------------------------------------------------
    // CONSULTATION
    // ----------------------------------------------------------------

    /**
     * Liste tous les documents déposés par un utilisateur — "Mes documents".
     */
    @Transactional(readOnly = true)
    public List<Document> getMesDocuments(String emailUtilisateur) {
        User utilisateur = userRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilisateur introuvable : " + emailUtilisateur));
        return documentRepository.findByUtilisateur(utilisateur);
    }

    /**
     * Vérifie si l'utilisateur a au moins un document validé.
     * Utile pour afficher un badge "Profil vérifié".
     */
    @Transactional(readOnly = true)
    public boolean possedeDocumentValide(String emailUtilisateur) {
        return getMesDocuments(emailUtilisateur).stream()
                .anyMatch(d -> d.getStatutValidation() == StatutValidation.VALIDE);
    }

    // ----------------------------------------------------------------
    // SUPPRESSION (l'utilisateur peut retirer un document en attente)
    // ----------------------------------------------------------------

    public void supprimerDocument(Long documentId, String emailUtilisateur) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Document introuvable : " + documentId));

        if (!doc.getUtilisateur().getEmail().equals(emailUtilisateur)) {
            throw new IllegalStateException(
                    "Vous n'êtes pas autorisé à supprimer ce document.");
        }

        if (doc.getStatutValidation() == StatutValidation.VALIDE) {
            throw new IllegalStateException(
                    "Un document déjà validé ne peut pas être supprimé.");
        }

        // Supprime le fichier physique
        try {
            Files.deleteIfExists(Paths.get(doc.getCheminFichier()));
        } catch (IOException e) {
            // On continue même si le fichier physique est introuvable
        }

        documentRepository.delete(doc);
    }

    // ----------------------------------------------------------------
    // UTILITAIRE PRIVÉ
    // ----------------------------------------------------------------
    private String obtenirExtension(String nomFichier) {
        if (nomFichier == null || !nomFichier.contains(".")) {
            throw new IllegalArgumentException("Nom de fichier invalide.");
        }
        return nomFichier.substring(nomFichier.lastIndexOf('.') + 1);
    }
}