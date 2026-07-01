package prolink.com.prolink.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entité Etudiant — hérite de User.
 *
 * Table JPA générée : "etudiants"
 * Colonnes : id (FK → users.id), universite, filiere,
 *            niveau_etude, cv_url, disponibilite
 *
 * Quand tu fais userRepository.findById(id), JPA fait automatiquement
 * un JOIN entre "users" et "etudiants" pour retourner l'objet complet.
 */
@Entity
@Table(name = "etudiants")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class Etudiant extends User {

    @Column(length = 100)
    private String universite;

    @Column(length = 100)
    private String filiere;
    @Column(name = "niveau_etude", length = 50)
    private String niveauEtude;

    // Chemin vers le CV uploadé
    @Column(name = "cv_url")
    private String cvUrl;

    @Column(length = 100)
    private String disponibilite;

    @Column(columnDefinition = "TEXT")
    private String competences;

    // Un étudiant peut postuler à plusieurs offres
    @OneToMany(mappedBy = "candidat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Candidature> candidatures = new ArrayList<>();
}