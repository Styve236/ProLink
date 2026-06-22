package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Historique d'une room de chat — trié chronologiquement
    List<ChatMessage> findByRoomIdOrderByHorodatageAsc(String roomId);

    // Derniers N messages d'une room — pour pagination future
    List<ChatMessage> findTop50ByRoomIdOrderByHorodatageDesc(String roomId);
}