package prolink.com.prolink.services;

import prolink.com.prolink.dto.request.InscriptionDto;
import prolink.com.prolink.entities.Etudiant;
import prolink.com.prolink.entities.Freelance;
import prolink.com.prolink.entities.Recruteur;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.repositories.EtudiantRepository;
import prolink.com.prolink.repositories.FreelanceRepository;
import prolink.com.prolink.repositories.RecruteurRepository;
import prolink.com.prolink.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service central pour l'authentification.
 *
 * Responsabilités :
 *  - inscrire() : crée le bon type d'entité selon le rôle choisi
 *    → Etudiant  → sauvegardé dans table "etudiants"
 *    → Freelance → sauvegardé dans table "freelances"
 *    → Recruteur → sauvegardé dans table "recruteurs"
 *  - connecter() : authentifie via Spring Security
 */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final EtudiantRepository etudiantRepository;
    private final FreelanceRepository freelanceRepository;
    private final RecruteurRepository recruteurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       EtudiantRepository etudiantRepository,
                       FreelanceRepository freelanceRepository,
                       RecruteurRepository recruteurRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.etudiantRepository = etudiantRepository;
        this.freelanceRepository = freelanceRepository;
        this.recruteurRepository = recruteurRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    // INSCRIPTION

    /**
     * Crée un compte selon le rôle choisi dans le formulaire.
     * Chaque rôle va dans sa propre table JPA (stratégie JOINED).
     *
     * @throws IllegalArgumentException si l'email est déjà utilisé
     * @throws IllegalArgumentException si les mots de passe ne correspondent pas
     */
    public User inscrire(InscriptionDto dto) {

        // Validation email unique
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException(
                    "Un compte existe déjà avec cette adresse email."
            );
        }

        // Validation email @gmail.com uniquement
        if (dto.getEmail() == null || !dto.getEmail().toLowerCase().endsWith("@gmail.com")) {
            throw new IllegalArgumentException(
                    "Seules les adresses @gmail.com sont acceptées."
            );
        }

        // Validation confirmation mot de passe
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException(
                    "Les mots de passe ne correspondent pas."
            );
        }

        // Dispatch selon le rôle — chaque branche crée la bonne sous-entité
        return switch (dto.getRole()) {
            case ETUDIANT  -> inscrireEtudiant(dto);
            case FREELANCE -> inscrireFreelance(dto);
            case RECRUTEUR -> inscrireRecruteur(dto);
            case ADMIN     -> throw new IllegalArgumentException(
                    "Impossible de créer un compte admin via l'inscription publique."
            );
        };
    }

    // Inscription ETUDIANT → table "etudiants"
    private Etudiant inscrireEtudiant(InscriptionDto dto) {
        Etudiant etudiant = new Etudiant();
        remplirChampsCommuns(etudiant, dto);
        etudiant.setRole(RoleUtilisateur.ETUDIANT);

        // Champs spécifiques étudiant
        etudiant.setUniversite(dto.getUniversite());
        etudiant.setFiliere(dto.getFiliere());
        etudiant.setNiveauEtude(dto.getNiveauEtude());

        // Statut EN_ATTENTE — l'admin valide après vérification
        etudiant.setStatut(StatutCompte.EN_ATTENTE);

        return etudiantRepository.save(etudiant);
    }

    // Inscription FREELANCE → table "freelances"
    private Freelance inscrireFreelance(InscriptionDto dto) {
        Freelance freelance = new Freelance();
        remplirChampsCommuns(freelance, dto);
        freelance.setRole(RoleUtilisateur.FREELANCE);

        // Champs spécifiques freelance
        freelance.setSpecialite(dto.getSpecialite());
        freelance.setTjm(dto.getTjm());

        freelance.setStatut(StatutCompte.EN_ATTENTE);

        return freelanceRepository.save(freelance);
    }

    // Inscription RECRUTEUR → table "recruteurs"
    private Recruteur inscrireRecruteur(InscriptionDto dto) {
        Recruteur recruteur = new Recruteur();
        remplirChampsCommuns(recruteur, dto);
        recruteur.setRole(RoleUtilisateur.RECRUTEUR);

        // Champs spécifiques recruteur
        if (dto.getNomEntreprise() == null || dto.getNomEntreprise().isBlank()) {
            throw new IllegalArgumentException(
                    "Le nom de l'entreprise est obligatoire pour un recruteur."
            );
        }
        recruteur.setNomEntreprise(dto.getNomEntreprise());
        recruteur.setSecteurActivite(dto.getSecteurActivite());

        recruteur.setStatut(StatutCompte.EN_ATTENTE);

        return recruteurRepository.save(recruteur);
    }

    // Champs communs à tous les rôles
    private void remplirChampsCommuns(User user, InscriptionDto dto) {
        user.setPrenom(dto.getPrenom());
        user.setNom(dto.getNom());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setTelephone(dto.getTelephone());
        user.setVille(dto.getVille());

        // Photo de profil (optionnelle)
        MultipartFile photo = dto.getPhoto();
        if (photo != null && !photo.isEmpty()) {
            String contentType = photo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException(
                        "Format photo invalide. Seules les images sont acceptées."
                );
            }
            try {
                String extension = obtenirExtension(photo.getOriginalFilename());
                String nomFichier = UUID.randomUUID() + "." + extension;
                Path dossier = Paths.get("uploads/photos/");
                Files.createDirectories(dossier);
                Path destination = dossier.resolve(nomFichier);
                Files.copy(photo.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
                user.setPhotoUrl("/uploads/photos/" + nomFichier);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Erreur lors de l'enregistrement de la photo : " + e.getMessage()
                );
            }
        }
    }

    private String obtenirExtension(String nomFichier) {
        if (nomFichier == null || !nomFichier.contains(".")) return "jpg";
        return nomFichier.substring(nomFichier.lastIndexOf('.') + 1).toLowerCase();
    }

    // CONNEXION

    /**
     * Authentifie l'utilisateur et stocke l'authentification
     * dans le SecurityContext (session Spring Security).
     *
     * Spring Security gère ensuite la session automatiquement.
     */
    public Authentication connecter(String email, String password) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    // UTILITAIRES
    @Transactional(readOnly = true)
    public User getUtilisateurConnecte(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Utilisateur connecté introuvable : " + email
                ));
    }
}