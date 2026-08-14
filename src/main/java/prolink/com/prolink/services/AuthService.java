package prolink.com.prolink.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prolink.com.prolink.dto.request.InscriptionDto;
import prolink.com.prolink.entities.Etudiant;
import prolink.com.prolink.entities.Freelance;
import prolink.com.prolink.entities.PasswordResetToken;
import prolink.com.prolink.entities.Recruteur;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.repositories.EtudiantRepository;
import prolink.com.prolink.repositories.FreelanceRepository;
import prolink.com.prolink.repositories.PasswordResetTokenRepository;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Préfixe d'URL public des photos (mappé par WebMvcConfig vers app.upload-dir)
    private static final String URL_PREFIX = "/uploads/";

    private final String photosDir;
    private final UserRepository userRepository;
    private final EtudiantRepository etudiantRepository;
    private final FreelanceRepository freelanceRepository;
    private final RecruteurRepository recruteurRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(@org.springframework.beans.factory.annotation.Value("${app.upload-dir:uploads/}") String uploadDir,
                       UserRepository userRepository,
                       EtudiantRepository etudiantRepository,
                       FreelanceRepository freelanceRepository,
                       RecruteurRepository recruteurRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        String dir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        this.photosDir = dir + "photos/";
        this.userRepository = userRepository;
        this.etudiantRepository = etudiantRepository;
        this.freelanceRepository = freelanceRepository;
        this.recruteurRepository = recruteurRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
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
                Path dossier = Paths.get(photosDir);
                Files.createDirectories(dossier);
                Path destination = dossier.resolve(nomFichier);
                Files.copy(photo.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
                user.setPhotoUrl(URL_PREFIX + "photos/" + nomFichier);
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

    // MOT DE PASSE OUBLIÉ

    public String creerTokenReinitialisation(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun compte trouvé avec cette adresse email."
                ));

        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user, 60);
        passwordResetTokenRepository.save(resetToken);

        String lien = "http://localhost:8080/auth/reinitialiser-mot-de-passe?token=" + token;
        String contenu = "Bonjour,\n\n" +
                "Vous avez demandé la réinitialisation de votre mot de passe ProLink.\n\n" +
                "Cliquez sur le lien ci-dessous (valable 60 minutes) :\n" +
                lien + "\n\n" +
                "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n" +
                "Cordialement,\nL'équipe ProLink";

        log.info("🔗 LIEN DE RÉINITIALISATION (non envoyé si SMTP non configuré) : {}", lien);
        emailService.envoyerEmail(email, "Réinitialisation mot de passe - ProLink", contenu);
        return token;
    }

    public void reinitialiserMotDePasse(String token, String nouveauMotDePasse, String confirmation) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide."));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Ce token a expiré ou a déjà été utilisé.");
        }

        if (!nouveauMotDePasse.equals(confirmation)) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(nouveauMotDePasse));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
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