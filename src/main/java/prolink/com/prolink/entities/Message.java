package prolink.com.prolink.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité Message — messagerie privée asynchrone entre deux utilisateurs.
 * Différent de ChatMessage qui est le chat temps réel WebSocket.
 *
 * Pas d'héritage — entité indépendante.
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "date_envoi", nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;

    @Column(nullable = false)
    private boolean lu = false;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediteur_id", nullable = false)
    private User expediteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private User destinataire;
    @PrePersist
    protected void onCreate() {
        this.dateEnvoi = LocalDateTime.now();
    }
}