package prolink.com.prolink.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO reçu depuis le formulaire de création d'un post de blog.
 * Tout utilisateur connecté et actif peut publier.
 */
@Getter
@Setter
public class BlogPostDto {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
    private String titre;

    @NotBlank(message = "Le contenu est obligatoire")
    @Size(min = 20, message = "Le contenu doit contenir au moins 20 caractères")
    private String contenu;
}
