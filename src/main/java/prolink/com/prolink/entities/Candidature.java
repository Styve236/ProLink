package prolink.com.prolink.entities;


import lombok.Data;
import prolink.com.prolink.enums.StatutCandidature;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import prolink.com.prolink.enums.StatutCandidature;

import java.time.LocalDateTime;

/**
 * Entité Candidature — lien entre un postulant (Etudiant ou Freelance)
 * et une JobOffer.
 *
 * Pas d'héritage — entité indépendante.
 *
 * Note : Le candidat est de type User (pas Etudiant ou Freelance)
 * pour simplifier la relation JPA. Le rôle réel est accessible
 * via candidat.getRole().
 */
@Entity
@Table(
        name = "candidatures",
        // Contrainte d'unicité : un utilisateur ne postule qu'une fois par offre
        uniqueConstraints = @UniqueConstraint(
                name = "uk_candidature_user_offre",
                columnNames = {"candidat_id", "offre_id"}
        )
)
@Getter
@Setter
@Data
@NoArgsConstructor
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_motivation", columnDefinition = "TEXT")
    private String messageMotivation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutCandidature statut = StatutCandidature.EN_ATTENTE;

    @Column(name = "date_candidature", nullable = false, updatable = false)
    private LocalDateTime dateCandidature;

    // Réponse du recruteur en cas de refus
    @Column(name = "commentaire_recruteur", columnDefinition = "TEXT")
    private String commentaireRecruteur;


    // Le postulant — Etudiant ou Freelance (stocké comme User en BDD)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidat_id", nullable = false)
    private User candidat;

    // L'offre ciblée
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_id", nullable = false)
    private JobOffer offre;

    @PrePersist
    protected void onCreate() {
        this.dateCandidature = LocalDateTime.now();
    }
}