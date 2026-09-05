package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.ChatMessage;
import prolink.com.prolink.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Historique d'une room de chat — trié chronologiquement
    List<ChatMessage> findByRoomIdOrderByHorodatageAsc(String roomId);

    // Derniers N messages d'une room — pour pagination future
    List<ChatMessage> findTop50ByRoomIdOrderByHorodatageDesc(String roomId);

    // ── Comptage par période — rapport d'activités admin ──
    long countByHorodatageBetween(LocalDateTime debut, LocalDateTime fin);

    // Suppression des messages de chat d'un utilisateur (nettoyage lors de la suppression d'un utilisateur)
    void deleteByExpediteur(User expediteur);
}