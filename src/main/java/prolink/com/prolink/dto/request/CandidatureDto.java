package prolink.com.prolink.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO reçu depuis le formulaire de candidature.
 * Utilisé par Etudiant et Freelance.
 */
@Getter
@Setter
public class CandidatureDto {

    @NotBlank(message = "Le message de motivation est obligatoire")
    @Size(min = 50, message = "Le message doit faire au moins 50 caractères")
    private String messageMotivation;
}