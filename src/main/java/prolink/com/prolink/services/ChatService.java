package prolink.com.prolink.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import prolink.com.prolink.entities.ChatMessage;
import prolink.com.prolink.repositories.ChatMessageRepository;

@Service
public class ChatService {
    @Autowired
    private ChatMessageRepository repository;

    public void sauvegarderMessage(ChatMessage message) {
        repository.save(message);
    }
}