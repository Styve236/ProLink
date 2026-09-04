package prolink.com.prolink.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité Reaction — "like" (pouce) d'un utilisateur sur un post de blog.
 * Contrainte d'unicité : un utilisateur ne peut liker qu'une seule fois
 * un même post.
 */
@Entity
@Table(
        name = "reactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reaction_user_post",
                columnNames = {"utilisateur_id", "post_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_reaction", nullable = false, updatable = false)
    private LocalDateTime dateReaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private BlogPost post;

    @PrePersist
    protected void onCreate() {
        this.dateReaction = LocalDateTime.now();
    }
}
