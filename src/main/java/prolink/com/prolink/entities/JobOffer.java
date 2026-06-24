package prolink.com.prolink.entities;

import lombok.Data;
import prolink.com.prolink.enums.StatutOffre;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité JobOffer — offre publiée par un Recruteur.
 */
@Entity
@Table(name = "offres")
@Data
@NoArgsConstructor
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titre;

    // CORRECTION : Ajout du champ entreprise qui manquait !
    @Column(length = 100)
    private String entreprise;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "type_contrat", length = 50)
    private String typeContrat;

    @Column(length = 100)
    private String lieu;

    // Rémunération (ex: "150 000 FCFA/mois")
    @Column(length = 100)
    private String remuneration;

    @Column(name = "competences_requises", columnDefinition = "TEXT")
    private String competencesRequises;

    @Column(name = "experience_requise", length = 50)
    private String experienceRequise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutOffre statut = StatutOffre.EN_ATTENTE;

    @Column(name = "date_publication", nullable = false, updatable = false)
    private LocalDateTime datePublication;

    @Column(name = "date_limite")
    private LocalDateTime dateLimite;

    @Column(name = "commentaire_admin", columnDefinition = "TEXT")
    private String commentaireAdmin;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruteur_id", nullable = false)
    private Recruteur recruteur;

    @OneToMany(mappedBy = "offre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Candidature> candidatures = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.datePublication = LocalDateTime.now();
    }

    public int getNombreCandidatures(){
        return candidatures != null ? candidatures.size() : 0;
    }
}