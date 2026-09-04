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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BlogService {

    private final BlogPostRepository blogPostRepository;
    private final CommentaireRepository commentaireRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final long MEDIA_MAX = 50 * 1024 * 1024; // 50 Mo
    private static final List<String> IMAGES_AUTORISEES = List.of("jpg", "jpeg", "png", "webp", "gif");
    private static final List<String> VIDEOS_AUTORISEES = List.of("mp4", "webm", "ogg");

    @org.springframework.beans.factory.annotation.Value("${app.upload-dir:uploads/}")
    private String uploadDir;

    // PUBLIER UN POST — tout utilisateur actif
    public BlogPost publierPost(BlogPostDto dto, String email, MultipartFile media) {
        User auteur = getUser(email);
        BlogPost post = new BlogPost();
        post.setTitre(dto.getTitre().trim());
        post.setContenu(dto.getContenu().trim());
        post.setAuteur(auteur);

        if (media != null && !media.isEmpty()) {
            String[] mediaInfos = sauvegarderMedia(media);
            post.setMediaUrl(mediaInfos[0]);
            post.setMediaType(mediaInfos[1]);
        }

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

    /**
     * Incrémente le compteur de vues d'un post.
     * - Ignore la vue si le visiteur connecté est l'auteur du post.
     * - Utilise un incrément atomique (UPDATE ... SET nb_vues = nb_vues + 1)
     *   pour éviter de perdre des vues en cas de requêtes concurrentes.
     */
    public void incrementerVues(Long id, String emailVisiteur) {
        // L'auteur ne compte pas sa propre consultation
        if (emailVisiteur != null) {
            Boolean estAuteur = blogPostRepository.estAuteur(id, emailVisiteur);
            if (Boolean.TRUE.equals(estAuteur)) {
                return;
            }
        }
        blogPostRepository.incrementerNbVues(id);
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

    /**
     * Sauvegarde le média (image ou vidéo) sur disque et renvoie
     * [url servable via /uploads/**, type] (IMAGE ou VIDEO).
     * Le fichier est conservé même si le post est supprimé (décision produit).
     */
    private String[] sauvegarderMedia(MultipartFile fichier) {
        if (fichier.getSize() > MEDIA_MAX) {
            throw new IllegalArgumentException(
                    "Le média dépasse la taille maximale autorisée (50 Mo).");
        }

        String extension = obtenirExtension(fichier.getOriginalFilename());
        String type;

        if (IMAGES_AUTORISEES.contains(extension.toLowerCase())) {
            type = "IMAGE";
        } else if (VIDEOS_AUTORISEES.contains(extension.toLowerCase())) {
            type = "VIDEO";
        } else {
            throw new IllegalArgumentException(
                    "Format non autorisé. Formats acceptés : " +
                            "JPG, PNG, WEBP, GIF et MP4, WEBM, OGG.");
        }

        String base = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        String dossierBlog = base + "blog/";

        try {
            String nomFichierStocke;
            Path dossier = Paths.get(dossierBlog);
            Files.createDirectories(dossier);
            Path destination;

            if ("IMAGE".equals(type)) {
                // Redimensionne + compresse l'image pour limiter son poids
                byte[] imageOptimisee = optimiserImage(fichier.getBytes());
                nomFichierStocke = UUID.randomUUID() + ".jpg";
                destination = dossier.resolve(nomFichierStocke);
                Files.write(destination, imageOptimisee);
            } else {
                nomFichierStocke = UUID.randomUUID() + "." + extension.toLowerCase();
                destination = dossier.resolve(nomFichierStocke);
                Files.copy(fichier.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            }

            return new String[]{"/uploads/blog/" + nomFichierStocke, type};
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Erreur lors de l'enregistrement du média : " + e.getMessage());
        }
    }

    /**
     * Redimensionne (largeur max 1200px) et compresse l'image en JPEG
     * pour réduire fortement le poids au stockage. Les PNG avec transparence
     * sont convertis vers un fond blanc.
     */
    private byte[] optimiserImage(byte[] donnees) throws IOException {
        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(donnees)) {
            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(bais);
            if (original == null) {
                throw new IllegalArgumentException("Image illisible ou corrompue.");
            }

            final int LARGEUR_MAX = 1200;
            int largeur = original.getWidth();
            int hauteur = original.getHeight();
            double ratio = (double) largeur / hauteur;

            if (largeur > LARGEUR_MAX) {
                largeur = LARGEUR_MAX;
                hauteur = (int) (largeur / ratio);
            }

            java.awt.image.BufferedImage sortie = new java.awt.image.BufferedImage(
                    largeur, hauteur, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = sortie.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, largeur, hauteur);
            g.drawImage(original, 0, 0, largeur, hauteur, null);
            g.dispose();

            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                javax.imageio.ImageWriter writer =
                        javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
                javax.imageio.stream.ImageOutputStream ios =
                        javax.imageio.ImageIO.createImageOutputStream(baos);
                writer.setOutput(ios);
                javax.imageio.plugins.jpeg.JPEGImageWriteParam param =
                        new javax.imageio.plugins.jpeg.JPEGImageWriteParam(null);
                param.setCompressionMode(javax.imageio.plugins.jpeg.JPEGImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.8f);
                writer.write(null, new javax.imageio.IIOImage(sortie, null, null), param);
                writer.dispose();
                ios.close();
                return baos.toByteArray();
            }
        }
    }

    private String obtenirExtension(String nomFichier) {
        if (nomFichier == null || !nomFichier.contains(".")) {
            throw new IllegalArgumentException("Nom de fichier invalide.");
        }
        return nomFichier.substring(nomFichier.lastIndexOf('.') + 1);
    }

    private boolean estAdmin(String email) {
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
