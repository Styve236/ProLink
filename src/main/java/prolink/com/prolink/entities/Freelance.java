package prolink.com.prolink.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entité Freelance — hérite de User.
 *
 * Table JPA générée : "freelances"
 * Colonnes : id (FK → users.id), specialite, portfolio_url,
 *            tjm, competences, annees_experience, disponibilite
 */
@Entity
@Table(name = "freelances")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class Freelance extends User {

    // Domaine principal : Développement Web, Design, Marketing, etc.
    @Column(length = 100)
    private String specialite;

    // Lien vers le portfolio en ligne
    @Column(name = "portfolio_url")
    private String portfolioUrl;

    // Taux Journalier Moyen en FCFA
    @Column(length = 20)
    private String tjm;

    // Compétences techniques (ex: "React, Node.js, Figma")
    @Column(columnDefinition = "TEXT")
    private String competences;

    @Column(name = "annees_experience")
    private Integer anneesExperience;

    @Column(length = 100)
    private String disponibilite;

    // Lien LinkedIn ou autre réseau professionnel
    @Column(name = "lien_professionnel")
    private String lienProfessionnel;

    // Un freelance peut postuler à des missions
    @OneToMany(mappedBy = "candidat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Candidature> candidatures = new ArrayList<>();
}