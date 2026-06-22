package prolink.com.prolink.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Recruteur — hérite de User.
 *
 * Table JPA générée : "recruteurs"
 * Colonnes : id (FK → users.id), nom_entreprise, secteur_activite,
 *            site_web, description_entreprise, ville_entreprise
 */
@Entity
@Table(name = "recruteurs")
@PrimaryKeyJoinColumn(name = "id")
@Data
@NoArgsConstructor
public class Recruteur extends User {

    @Column(name = "nom_entreprise", nullable = false, length = 100)
    private String nomEntreprise;

    @Column(name = "secteur_activite", length = 100)
    private String secteurActivite;

    // Site web de l'entreprise
    @Column(name = "site_web")
    private String siteWeb;

    // Présentation de l'entreprise pour le profil public
    @Column(name = "description_entreprise", columnDefinition = "TEXT")
    private String descriptionEntreprise;

    // Ville du siège (peut différer de la ville du recruteur)
    @Column(name = "ville_entreprise", length = 100)
    private String villeEntreprise;

    // Taille de l'entreprise : "1-10", "11-50", "51-200", "200+"
    @Column(name = "taille_entreprise", length = 20)
    private String tailleEntreprise;

    // Un recruteur peut publier plusieurs offres
    @OneToMany(mappedBy = "recruteur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobOffer> offres = new ArrayList<>();
}