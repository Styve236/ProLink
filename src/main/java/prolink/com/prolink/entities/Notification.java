package prolink.com.prolink.entities;

import prolink.com.prolink.enums.TypeNotification;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité Notification — notifications in-app pour les utilisateurs.
 *
 * Générées automatiquement par les services lors d'événements :
 *  - Nouvelle candidature reçue     → notifie le Recruteur
 *  - Candidature acceptée / refusée → notifie l'Etudiant / Freelance
 *  - Document validé / rejeté       → notifie l'utilisateur
 *  - Nouveau message reçu           → notifie le destinataire
 *
 * Pas d'héritage — entité indépendante.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(nullable = false)
    private boolean lue = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeNotification type;

    // Lien vers la page concernée (ex: "/candidatures/12", "/offres/5")
    @Column(name = "lien_action")
    private String lienAction;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}