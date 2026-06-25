package prolink.com.prolink.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prolink.com.prolink.entities.LinkAction;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.repositories.LinkActionRepository;
import prolink.com.prolink.repositories.UserRepository;
import prolink.com.prolink.security.RequiertCompteValide;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LinkActionService {

    private final LinkActionRepository linkActionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;


    // ENVOYER UNE DEMANDE DE CONNEXION
    @RequiertCompteValide
    public LinkAction envoyerDemande(String emailDemandeur, Long cibleId) {

        User demandeur = userRepository.findByEmail(emailDemandeur)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        if (demandeur.getId().equals(cibleId)) {
            throw new IllegalArgumentException("Impossible de s'envoyer une demande à soi-même.");
        }

        User cible = userRepository.findById(cibleId)
                .orElseThrow(() -> new IllegalArgumentException("Destinataire introuvable."));

        // Vérifie s'il existe déjà une demande dans un sens ou l'autre
        boolean dejaConnectes = linkActionRepository.sontConnectes(demandeur.getId(), cibleId);
        if (dejaConnectes) {
            throw new IllegalStateException("Vous êtes déjà en contact avec cet utilisateur.");
        }

        linkActionRepository.findByDemandeurAndCible(demandeur, cible).ifPresent(l -> {
            throw new IllegalStateException("Une demande est déjà en attente.");
        });

        LinkAction demande = new LinkAction();
        demande.setDemandeur(demandeur);
        demande.setCible(cible);
        demande.setStatut("EN_ATTENTE");

        LinkAction saved = linkActionRepository.save(demande);

        notificationService.notifierNouvelleDemandeConnexion(cible, demandeur);

        return saved;
    }

    // RÉPONDRE À UNE DEMANDE (accepter ou refuser)
    public LinkAction repondreDemande(Long demandeId, String emailCible, boolean accepter) {

        LinkAction demande = linkActionRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable."));

        // Sécurité : seul le destinataire de la demande peut y répondre
        if (!demande.getCible().getEmail().equals(emailCible)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à répondre à cette demande.");
        }

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }

        demande.setStatut(accepter ? "ACCEPTEE" : "REFUSEE");
        demande.setDateReponse(LocalDateTime.now());

        return linkActionRepository.save(demande);
    }

    // CONSULTATION
    @Transactional(readOnly = true)
    public List<LinkAction> getDemandesRecues(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return linkActionRepository.findByCibleOrderByDateDemandeDesc(user);
    }

    @Transactional(readOnly = true)
    public List<LinkAction> getDemandesEnvoyees(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return linkActionRepository.findByDemandeurOrderByDateDemandeDesc(user);
    }

    @Transactional(readOnly = true)
    public long compterDemandesEnAttente(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return linkActionRepository.countByCibleAndStatut(user, "EN_ATTENTE");
    }

    @Transactional(readOnly = true)
    public List<User> getConnexionsAcceptees(User utilisateur) {
        List<LinkAction> actions = linkActionRepository.findConnexionsAcceptees(utilisateur.getId());
        return actions.stream().map(action -> {
            if (action.getDemandeur().getId().equals(utilisateur.getId())) {
                return action.getCible();
            } else {
                return action.getDemandeur();
            }
        }).collect(Collectors.toList());
    }

    /**
     * Vérifie si deux utilisateurs peuvent communiquer (chat/messages).
     * Utilisé par ChatController et MessageController.
     */
    @Transactional(readOnly = true)
    public boolean peuventCommuniquer(Long idA, Long idB) {
        return linkActionRepository.sontConnectes(idA, idB);
    }
}