package prolink.com.prolink.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité BlogPost — publication rédigée par un utilisateur (entreprise,
 * freelance, étudiant ou admin) pour partager une actualité.
 *
 * Publication directe (pas de modération), mais l'admin peut retirer
 * un post jugé incohérent ou blessant (suppression définitive).
 */
@Entity
@Table(name = "blog_posts")
@Getter
@Setter
@NoArgsConstructor
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "date_publication", nullable = false, updatable = false)
    private LocalDateTime datePublication;

    @Column(name = "nb_vues")
    private int nbVues = 0;

    // Média du post (optionnel) : image ou vidéo téléversée
    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "media_type")
    private String mediaType;

    // L'auteur — tout utilisateur actif peut publier (User stocké en BDD)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    private User auteur;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Commentaire> commentaires = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reaction> reactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.datePublication = LocalDateTime.now();
    }

    public int getNombreCommentaires() {
        return commentaires != null ? commentaires.size() : 0;
    }

    public int getNombreLikes() {
        return reactions != null ? reactions.size() : 0;
    }

    public boolean getAUnMedia() {
        return mediaUrl != null && !mediaUrl.isBlank();
    }

    public boolean getEstVideo() {
        return "VIDEO".equalsIgnoreCase(mediaType);
    }

    public boolean getEstImage() {
        return "IMAGE".equalsIgnoreCase(mediaType);
    }
}
