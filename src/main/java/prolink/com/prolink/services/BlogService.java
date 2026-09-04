package prolink.com.prolink.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prolink.com.prolink.dto.request.BlogPostDto;
import prolink.com.prolink.entities.BlogPost;
import prolink.com.prolink.entities.Commentaire;
import prolink.com.prolink.entities.Reaction;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.repositories.BlogPostRepository;
import prolink.com.prolink.repositories.CommentaireRepository;
import prolink.com.prolink.repositories.ReactionRepository;
import prolink.com.prolink.repositories.UserRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class BlogService {

    private final BlogPostRepository blogPostRepository;
    private final CommentaireRepository commentaireRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // PUBLIER UN POST — tout utilisateur actif
    public BlogPost publierPost(BlogPostDto dto, String email) {
        User auteur = getUser(email);
        BlogPost post = new BlogPost();
        post.setTitre(dto.getTitre().trim());
        post.setContenu(dto.getContenu().trim());
        post.setAuteur(auteur);
        return blogPostRepository.save(post);
    }

    // LISTE PUBLIQUE — paginée
    @Transactional(readOnly = true)
    public Page<BlogPost> getPostsPublies(int page, int taille) {
        Pageable pageable = PageRequest.of(page, taille, Sort.by(Sort.Direction.DESC, "datePublication"));
        return blogPostRepository.findAllByOrderByDatePublicationDesc(pageable);
    }

    // RECHERCHE — paginée
    @Transactional(readOnly = true)
    public Page<BlogPost> rechercher(String terme, int page, int taille) {
        Pageable pageable = PageRequest.of(page, taille, Sort.by(Sort.Direction.DESC, "datePublication"));
        return blogPostRepository.rechercher(terme.trim(), pageable);
    }

    // COMPTEURS — évite de charger les collections (commentaires / réactions)
    @Transactional(readOnly = true)
    public java.util.Map<Long, Long> compterCommentaires() {
        java.util.Map<Long, Long> map = new java.util.HashMap<>();
        for (Object[] row : blogPostRepository.countCommentairesParPost()) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, Long> compterLikes() {
        java.util.Map<Long, Long> map = new java.util.HashMap<>();
        for (Object[] row : blogPostRepository.countReactionsParPost()) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public long compterLikesPost(Long postId) {
        return blogPostRepository.countReactionsByPostId(postId);
    }

    // DÉTAIL — incrémente les vues
    @Transactional(readOnly = true)
    public BlogPost getPostDetail(Long id) {
        return blogPostRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post introuvable : " + id));
    }

    public void incrementerVues(Long id) {
        blogPostRepository.findById(id).ifPresent(post -> {
            post.setNbVues(post.getNbVues() + 1);
            blogPostRepository.save(post);
        });
    }

    // MES POSTS
    @Transactional(readOnly = true)
    public java.util.List<BlogPost> getMesPosts(String email) {
        User auteur = getUser(email);
        return blogPostRepository.findByAuteur_IdOrderByDatePublicationDesc(auteur.getId());
    }

    // COMMENTER
    public Commentaire commenter(Long postId, String contenu, String email) {
        User auteur = getUser(email);
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post introuvable : " + postId));

        Commentaire commentaire = new Commentaire();
        commentaire.setContenu(contenu.trim());
        commentaire.setAuteur(auteur);
        commentaire.setPost(post);
        commentaireRepository.save(commentaire);

        // Notifier l'auteur du post si ce n'est pas lui-même
        if (!post.getAuteur().getId().equals(auteur.getId())) {
            notificationService.notifierNouveauCommentaire(
                    post.getAuteur(), auteur, postId, post.getTitre());
        }
        return commentaire;
    }

    // LIKE / UNLIKE (toggle)
    public boolean toggleLike(Long postId, String email) {
        User utilisateur = getUser(email);
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post introuvable : " + postId));

        java.util.Optional<Reaction> existant = reactionRepository
                .findByPostAndUtilisateur(post, utilisateur);

        if (existant.isPresent()) {
            reactionRepository.delete(existant.get());
            return false; // a retiré son like
        }

        Reaction reaction = new Reaction();
        reaction.setPost(post);
        reaction.setUtilisateur(utilisateur);
        reactionRepository.save(reaction);

        // Notifier l'auteur si ce n'est pas lui-même
        if (!post.getAuteur().getId().equals(utilisateur.getId())) {
            notificationService.notifierNouveauLike(
                    post.getAuteur(), utilisateur, postId, post.getTitre());
        }
        return true; // a aimé
    }

    // L'utilisateur connecté a-t-il déjà liké ce post ?
    @Transactional(readOnly = true)
    public boolean aDejaLiker(Long postId, String email) {
        if (email == null) return false;
        User utilisateur = getUser(email);
        BlogPost post = blogPostRepository.getReferenceById(postId);
        return reactionRepository.existsByPostAndUtilisateur(post, utilisateur);
    }

    // SUPPRIMER — l'auteur OU l'admin
    public void supprimerPost(Long postId, String emailDemandeur) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post introuvable : " + postId));

        boolean estAuteur = post.getAuteur().getEmail().equals(emailDemandeur);
        boolean estAdmin = estAdmin(emailDemandeur);

        if (!estAuteur && !estAdmin) {
            throw new IllegalStateException("Action non autorisée.");
        }

        // Notifier l'auteur quand c'est l'admin qui retire le post
        if (estAdmin && !estAuteur) {
            notificationService.creerNotificationPostRetire(
                    post.getAuteur(), post.getTitre(), postId);
        }

        blogPostRepository.delete(post);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilisateur introuvable : " + email));
    }

    private boolean estAdmin(String email) {
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
