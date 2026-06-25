package prolink.com.prolink.dto.request;

import prolink.com.prolink.enums.RoleUtilisateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO reçu depuis le formulaire d'inscription Thymeleaf.
 * Ce que le formulaire envoie — jamais une entité JPA directement.
 */
@Getter
@Setter
public class InscriptionDto {

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères")
    private String password;

    @NotBlank(message = "Veuillez confirmer le mot de passe")
    private String confirmPassword;

    @NotNull(message = "Veuillez choisir un rôle")
    private RoleUtilisateur role;   // ETUDIANT, FREELANCE ou RECRUTEUR

    private String telephone;
    private String ville;

    // Champs spécifiques ETUDIANT
    private String universite;
    private String filiere;
    private String niveauEtude;

    // Champs spécifiques FREELANCE
    private String specialite;
    private String tjm;

    // Champs spécifiques RECRUTEUR
    private String nomEntreprise;
    private String secteurActivite;

    // Photo de profil (optionnelle)
    private MultipartFile photo;
}