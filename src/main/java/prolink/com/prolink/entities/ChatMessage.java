package prolink.com.prolink.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité ChatMessage — messages du chat temps réel WebSocket.
 *
 * Différence avec Message :
 *  - Message     : messagerie privée classique (boîte de réception)
 *  - ChatMessage : chat temps réel dans une room (WebSocket STOMP)
 *
 * Une "room" est identifiée par roomId = "user_{idA}_user_{idB}"
 * (les deux IDs triés pour garantir l'unicité).
 *
 * Pas d'héritage — entité indépendante.
 */
@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifiant de la room de chat (ex: "user_3_user_7")
    @Column(name = "room_id", nullable = false, length = 50)
    private String roomId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(nullable = false, updatable = false)
    private LocalDateTime horodatage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediteur_id", nullable = false)
    private User expediteur;

    @PrePersist
    protected void onCreate() {
        this.horodatage = LocalDateTime.now();
    }

    public void setType(String notification) {
    }
}