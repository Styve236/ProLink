package prolink.com.prolink.entities;

import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.enums.StatutCompte;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité parent de tous les utilisateurs ProLink.
 *
 * Stratégie JOINED :
 *  → Table "users"      : colonnes communes à tous les utilisateurs
 *  → Table "etudiants"  : colonnes spécifiques à Etudiant (FK → users.id)
 *  → Table "freelances" : colonnes spécifiques à Freelance (FK → users.id)
 *  → Table "recruteurs" : colonnes spécifiques à Recruteur (FK → users.id)
 *
 * Un utilisateur ADMIN n'a pas de sous-classe — il est stocké
 * uniquement dans la table "users" avec role = 'ADMIN'.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nom;

    @Column(nullable = false, length = 50)
    private String prenom;

    @Column(length = 20)
    private String telephone;

    @Column(length = 100)
    private String ville;

    // Photo de profil — chemin vers le fichier uploadé
    @Column(name = "photo_url")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleUtilisateur role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutCompte statut = StatutCompte.EN_ATTENTE;

    // Score de confiance attribué par l'admin (0 à 100)
    @Column(name = "trust_score")
    private int trustScore = 0;

    @Column(name = "date_inscription", nullable = false, updatable = false)
    private LocalDateTime dateInscription;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @OneToMany(mappedBy = "expediteur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messagesEnvoyes = new ArrayList<>();

    @OneToMany(mappedBy = "destinataire", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messagesRecus = new ArrayList<>();

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.dateInscription = LocalDateTime.now();
        // Un compte admin est actif directement
        if (this.role == RoleUtilisateur.ADMIN) {
            this.statut = StatutCompte.ACTIF;
            this.trustScore = 100;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Hook disponible pour audit futur
    }

    // Méthode utilitaire pour Thymeleaf et Spring Security
    public String getNomComplet() {
        return prenom + " " + nom;
    }
}