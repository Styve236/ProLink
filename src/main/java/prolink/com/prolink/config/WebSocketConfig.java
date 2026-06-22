package prolink.com.prolink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket avec le protocole STOMP pour le chat temps réel.
 *
 * Flux d'un message de chat :
 *  1. Client JS se connecte à /ws (endpoint SockJS)
 *  2. Client envoie un message vers /app/chat.envoyer
 *  3. ChatController reçoit avec @MessageMapping("/chat.envoyer")
 *  4. ChatController diffuse vers /topic/chat.{roomId}
 *  5. Tous les abonnés à ce topic reçoivent le message en temps réel
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure le broker de messages.
     *
     * /topic  → diffusion 1-vers-plusieurs (chat de groupe, notifications globales)
     * /queue  → messages privés 1-vers-1 (messagerie directe entre deux utilisateurs)
     * /app    → préfixe pour les méthodes @MessageMapping des controllers
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker simple en mémoire pour /topic et /queue
        // En production avancée, on remplacerait par RabbitMQ ou ActiveMQ
        registry.enableSimpleBroker("/topic", "/queue");

        // Préfixe des destinations gérées par les @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");

        // Préfixe pour les messages privés (ex: /user/{username}/queue/messages)
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Enregistre l'endpoint WebSocket avec fallback SockJS.
     *
     * SockJS assure la compatibilité avec les navigateurs
     * qui ne supportent pas WebSocket nativement.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}