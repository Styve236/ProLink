package prolink.com.prolink.services;

import prolink.com.prolink.entities.Etudiant;
import prolink.com.prolink.entities.Freelance;
import prolink.com.prolink.entities.Recruteur;
import prolink.com.prolink.entities.User;
//import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.repositories.EtudiantRepository;
import prolink.com.prolink.repositories.FreelanceRepository;
import prolink.com.prolink.repositories.RecruteurRepository;
import prolink.com.prolink.repositories.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des profils utilisateurs.
 *
 * Couvre le cas d'utilisation "Gérer son profil" du diagramme :
 *  - Mise à jour des informations personnelles
 *  - Upload de photo de profil
 *  - Consultation de profil public
 *  - Recherche d'utilisateurs / profils
 */
@Service
@Transactional
public class ProfilService {

    private static final String UPLOAD_DIR = "uploads/photos/";

    private final UserRepository userRepository;
    private final EtudiantRepository etudiantRepository;
    private final FreelanceRepository freelanceRepository;
    private final RecruteurRepository recruteurRepository;

    public ProfilService(UserRepository userRepository,
                         EtudiantRepository etudiantRepository,
                         FreelanceRepository freelanceRepository,
                         RecruteurRepository recruteurRepository) {
        this.userRepository = userRepository;
        this.etudiantRepository = etudiantRepository;
        this.freelanceRepository = freelanceRepository;
        this.recruteurRepository = recruteurRepository;
    }

    // CONSULTATION
    /**
     * Récupère un utilisateur par son ID.
     * Utilisé pour afficher le profil public d'un autre utilisateur.
     */
    @Transactional(readOnly = true)
    public User getProfilParId(Long id) {
        User user = userRepository.findByIdAvecDocuments(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profil introuvable pour l'id : " + id));
        userRepository.findByIdAvecMessages(id);
        return user;
    }

    /**
     * Récupère le profil complet selon le rôle.
     * Retourne le bon type (Etudiant, Freelance, Recruteur) avec
     * toutes les colonnes de sa table spécifique.
     */
    @Transactional(readOnly = true)
    public User getProfilComplet(String email) {
        User user = userRepository.findByEmailAvecDocuments(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilisateur introuvable : " + email));
        userRepository.findByEmailAvecMessages(email);
        return user;
    }

    // MISE A JOUR — champs communs
    /**
     * Met à jour les informations communes à tous les rôles.
     */
    public User mettreAJourInfosCommunes(String email,
                                         String prenom,
                                         String nom,
                                         String telephone,
                                         String ville) {
        User user = getProfilComplet(email);
        user.setPrenom(prenom);
        user.setNom(nom);
        user.setTelephone(telephone);
        user.setVille(ville);
        return userRepository.save(user);
    }

    // MISE A JOUR — champs spécifiques par rôle
    /**
     * Met à jour les infos spécifiques d'un étudiant.
     * @PreAuthorize garantit qu'un freelance ne peut pas appeler cette méthode.
     */
    @PreAuthorize("hasRole('ETUDIANT')")
    public Etudiant mettreAJourProfilEtudiant(String email,
                                              String universite,
                                              String filiere,
                                              String niveauEtude,
                                              String competences,
                                              String disponibilite) {
        Etudiant etudiant = etudiantRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Etudiant introuvable : " + email
                ));
        etudiant.setUniversite(universite);
        etudiant.setFiliere(filiere);
        etudiant.setNiveauEtude(niveauEtude);
        etudiant.setCompetences(competences);
        etudiant.setDisponibilite(disponibilite);
        return etudiantRepository.save(etudiant);
    }

    @PreAuthorize("hasRole('FREELANCE')")
    public Freelance mettreAJourProfilFreelance(String email,
                                                String specialite,
                                                String portfolioUrl,
                                                String tjm,
                                                String competences,
                                                String disponibilite) {
        Freelance freelance = freelanceRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Freelance introuvable : " + email
                ));
        freelance.setSpecialite(specialite);
        freelance.setPortfolioUrl(portfolioUrl);
        freelance.setTjm(tjm);
        freelance.setCompetences(competences);
        freelance.setDisponibilite(disponibilite);
        return freelanceRepository.save(freelance);
    }

    @PreAuthorize("hasRole('RECRUTEUR')")
    public Recruteur mettreAJourProfilRecruteur(String email,
                                                String nomEntreprise,
                                                String secteurActivite,
                                                String siteWeb,
                                                String descriptionEntreprise) {
        Recruteur recruteur = recruteurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recruteur introuvable : " + email
                ));
        recruteur.setNomEntreprise(nomEntreprise);
        recruteur.setSecteurActivite(secteurActivite);
        recruteur.setSiteWeb(siteWeb);
        recruteur.setDescriptionEntreprise(descriptionEntreprise);
        return recruteurRepository.save(recruteur);
    }

    // UPLOAD PHOTO DE PROFIL

    /**
     * Sauvegarde la photo de profil sur le disque et met à jour l'URL en base.
     * Le fichier est renommé avec un UUID pour éviter les collisions.
     */
    public String uploadPhoto(String email, MultipartFile photo) throws IOException {

        if (photo.isEmpty()) {
            throw new IllegalArgumentException("Le fichier photo est vide.");
        }

        // Vérification type MIME — photos uniquement
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "Format invalide. Seules les images sont acceptées."
            );
        }

        // Nom unique pour éviter les collisions
        String extension = obtenirExtension(photo.getOriginalFilename());
        String nomFichier = UUID.randomUUID() + "." + extension;

        // Création du dossier si nécessaire
        Path dossier = Paths.get(UPLOAD_DIR);
        Files.createDirectories(dossier);

        // Sauvegarde du fichier
        Path destination = dossier.resolve(nomFichier);
        Files.copy(photo.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        // Mise à jour de l'URL en base
        String urlPhoto = "/" + UPLOAD_DIR + nomFichier;
        User user = getProfilComplet(email);
        user.setPhotoUrl(urlPhoto);
        userRepository.save(user);

        return urlPhoto;
    }

    // RECHERCHE
    /**
     * Recherche des profils par nom, prénom ou email.
     * Utilisé pour la barre de recherche "Rechercher des offres/profils"
     * du diagramme de cas d'utilisation.
     */
    @Transactional(readOnly = true)
    public List<User> rechercherProfils(String terme) {
        if (terme == null || terme.isBlank()) {
            return List.of();
        }
        return userRepository.rechercherParNomOuEmail(terme.trim());
    }

    /**
     * Mise à jour de la dernière connexion — appelée après login réussi.
     */
    public void mettreAJourDerniereConnexion(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setDerniereConnexion(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    // UTILITAIRE PRIVÉ
    private String obtenirExtension(String nomFichier) {
        if (nomFichier == null || !nomFichier.contains(".")) return "jpg";
        return nomFichier.substring(nomFichier.lastIndexOf('.') + 1).toLowerCase();
    }
}