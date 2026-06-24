package prolink.com.prolink.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO reçu depuis le formulaire de création/modification d'offre.
 * Utilisé par le Recruteur uniquement.
 */
@Getter
@Setter
public class OffreDto {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotBlank(message = "Le type de contrat est obligatoire")
    private String typeContrat;  // CDI, CDD, Stage, Freelance, Alternance

    private String lieu;
    private String remuneration;
    private String competencesRequises;
    private String experienceRequise;
    private LocalDateTime dateLimite;
}