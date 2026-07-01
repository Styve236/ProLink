package prolink.com.prolink.entities;

import prolink.com.prolink.enums.StatutValidation;
import prolink.com.prolink.enums.TypeDocument;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité Document — fichier uploadé par un utilisateur.
 *
 * Utilisé pour :
 *  - Carte étudiante (Etudiant)
 *  - Kbis / Registre commerce (Recruteur)
 *  - CV, Portfolio (Etudiant / Freelance)
 *
 * L'admin valide ou rejette via AdminController.
 * Pas d'héritage — entité indépendante.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    // Chemin relatif depuis le dossier uploads/ (ex: "documents/user_5_cv.pdf")
    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

    // Type MIME (ex: "application/pdf", "image/jpeg")
    @Column(name = "type_mime", length = 50)
    private String typeMime;

    // Taille en octets
    private Long taille;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 30)
    private TypeDocument typeDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", nullable = false, length = 20)
    private StatutValidation statutValidation = StatutValidation.EN_ATTENTE;

    // Note de l'admin lors de la validation ou du rejet
    @Column(name = "commentaire_admin", columnDefinition = "TEXT")
    private String commentaireAdmin;

    @Column(name = "date_depot", nullable = false, updatable = false)
    private LocalDateTime dateDepot;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    // L'utilisateur qui a déposé le document
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;
    @PrePersist
    protected void onCreate() {
        this.dateDepot = LocalDateTime.now();
    }
}