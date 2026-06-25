package prolink.com.prolink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor())
                .withSockJS();
    }

    private HandshakeInterceptor handshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(
                    org.springframework.http.server.ServerHttpRequest request,
                    org.springframework.http.server.ServerHttpResponse response,
                    org.springframework.web.socket.WebSocketHandler wsHandler,
                    Map<String, Object> attributes) {

                Principal principal = request.getPrincipal();
                if (principal != null) {
                    attributes.put("userEmail", principal.getName());
                }
                return true;
            }

            @Override
            public void afterHandshake(
                    org.springframework.http.server.ServerHttpRequest request,
                    org.springframework.http.server.ServerHttpResponse response,
                    org.springframework.web.socket.WebSocketHandler wsHandler,
                    Exception exception) {
            }
        };
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
                    String email = (String) accessor.getSessionAttributes().get("userEmail");
                    if (email != null) {
                        accessor.setUser(() -> email);
                    }
                }
                return message;
            }
        });
    }
}
