package prolink.com.prolink.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité LinkAction — demandes de connexion entre utilisateurs.
 *
 * Modélise le flux "Demande de connexion" du diagramme de cas d'utilisation.
 *
 * Statuts possibles (stockés en String) :
 *  EN_ATTENTE → ACCEPTEE ou REFUSEE
 *
 * Pas d'héritage — entité indépendante.
 */
@Entity
@Table(
        name = "link_actions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_link_demandeur_cible",
                columnNames = {"demandeur_id", "cible_id"}
        )
)

@Getter
@Setter
@NoArgsConstructor
public class LinkAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Statut : EN_ATTENTE, ACCEPTEE, REFUSEE
    @Column(nullable = false, length = 20)
    private String statut = "EN_ATTENTE";

    @Column(name = "date_demande", nullable = false, updatable = false)
    private LocalDateTime dateDemande;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    // L'utilisateur qui envoie la demande de connexion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private User demandeur;

    // L'utilisateur qui reçoit la demande
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cible_id", nullable = false)
    private User cible;

    @PrePersist
    protected void onCreate() {
        this.dateDemande = LocalDateTime.now();
    }
}