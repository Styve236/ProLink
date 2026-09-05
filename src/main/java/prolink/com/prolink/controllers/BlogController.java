package prolink.com.prolink.controllers;

import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import prolink.com.prolink.entities.BlogPost;
import prolink.com.prolink.services.BlogService;

import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    // LISTE PUBLIQUE DES POSTS
    @GetMapping
    public String listerPosts(Model model,
                              @RequestParam(required = false) String recherche,
                              @RequestParam(defaultValue = "0") int page,
                              @AuthenticationPrincipal UserDetails userDetails) {

        Page<BlogPost> pagePosts;
        if (recherche != null && !recherche.isBlank()) {
            pagePosts = blogService.rechercher(recherche, page, 9);
            model.addAttribute("recherche", recherche);
        } else {
            pagePosts = blogService.getPostsPublies(page, 9);
        }

        model.addAttribute("listePosts", pagePosts.getContent());
        model.addAttribute("comptesCommentaires", blogService.compterCommentaires());
        model.addAttribute("comptesLikes", blogService.compterLikes());
        model.addAttribute("pageActuelle", page);
        model.addAttribute("totalPages", pagePosts.getTotalPages());
        model.addAttribute("totalElements", pagePosts.getTotalElements());

        if (userDetails != null) {
            model.addAttribute("emailConnecte", userDetails.getUsername());
        }

        return "blog/blog-liste";
    }

    // FORMULAIRE CRÉATION — tout utilisateur connecté
    @GetMapping("/nouvelle")
    public String afficherFormulaireCreation(Model model) {
        model.addAttribute("blogPostDto",
                new prolink.com.prolink.dto.request.BlogPostDto());
        return "blog/blog-form";
    }

    // PUBLIER UN POST — tout utilisateur connecté
    @PostMapping("/enregistrer")
    public String enregistrerPost(
            @Valid @ModelAttribute("blogPostDto") prolink.com.prolink.dto.request.BlogPostDto dto,
            BindingResult result,
            @RequestParam(value = "media", required = false) org.springframework.web.multipart.MultipartFile media,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "blog/blog-form";
        }

        try {
            BlogPost post = blogService.publierPost(dto, principal.getName(), media);
            redirectAttributes.addFlashAttribute("succes",
                    "Votre article a été publié avec succès !");
            return "redirect:/blog/" + post.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/blog/nouvelle";
        }
    }

    // MES POSTS — tout utilisateur connecté (l'admin voit TOUS les posts + suppression)
    @GetMapping("/mes-posts")
    public String voirMesPosts(Model model, Principal principal) {
        String email = principal.getName();
        model.addAttribute("mesPosts",
                blogService.getMesPosts(email));
        model.addAttribute("comptesCommentaires", blogService.compterCommentaires());
        model.addAttribute("comptesLikes", blogService.compterLikes());
        model.addAttribute("estAdmin", blogService.estAdmin(email));
        return "blog/mes-posts";
    }

    // DÉTAIL D'UN POST
    private static final String COOKIE_VUES = "prolink_articles_vus";

    @GetMapping("/{id:[0-9]+}")
    public String detailPost(@PathVariable Long id,
                             Model model,
                             @CookieValue(value = COOKIE_VUES, required = false) String articlesVusCookie,
                             HttpServletResponse response,
                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            BlogPost post = blogService.getPostDetail(id);

            // Comptage des vues avec déduplication par visiteur (cookie)
            boolean dejaVu = articlesVusCookie != null
                    && Arrays.asList(articlesVusCookie.split(",")).contains(String.valueOf(id));
            if (!dejaVu) {
                blogService.incrementerVues(id,
                        userDetails != null ? userDetails.getUsername() : null);
                // Met à jour le cookie (limité à 200 articles max pour rester léger)
                Set<String> vus = new LinkedHashSet<>();
                if (articlesVusCookie != null && !articlesVusCookie.isBlank()) {
                    vus.addAll(Arrays.asList(articlesVusCookie.split(",")));
                }
                vus.add(String.valueOf(id));
                if (vus.size() > 200) {
                    vus.remove(vus.iterator().next());
                }
                Cookie cookie = new Cookie(COOKIE_VUES,
                        String.join(",", vus));
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24 * 365); // 1 an
                response.addCookie(cookie);
            }

            model.addAttribute("post", post);
            model.addAttribute("nombreLikesPost", blogService.compterLikesPost(id));

            if (userDetails != null) {
                model.addAttribute("emailConnecte", userDetails.getUsername());
                model.addAttribute("aDejaLike",
                        blogService.aDejaLiker(id, userDetails.getUsername()));
                model.addAttribute("estAuteur",
                        post.getAuteur().getEmail().equals(userDetails.getUsername()));
            }

            return "blog/blog-detail";

        } catch (IllegalArgumentException e) {
            return "redirect:/blog";
        }
    }

    // COMMENTER UN POST — tout utilisateur connecté
    @PostMapping("/{id}/commenter")
    public String commenterPost(@PathVariable Long id,
                                @RequestParam("contenu") String contenu,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {

        if (contenu == null || contenu.isBlank()) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Le commentaire ne peut pas être vide.");
            return "redirect:/blog/" + id;
        }

        try {
            blogService.commenter(id, contenu, principal.getName());
            redirectAttributes.addFlashAttribute("succes", "Commentaire ajouté !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/blog/" + id;
    }

    // LIKE / UNLIKE — tout utilisateur connecté
    @PostMapping("/{id}/like")
    public String toggleLike(@PathVariable Long id,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            blogService.toggleLike(id, principal.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/blog/" + id;
    }

    // SUPPRIMER UN POST — auteur ou admin
    @PostMapping("/{id}/supprimer")
    public String supprimerPost(@PathVariable Long id,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            blogService.supprimerPost(id, principal.getName());
            redirectAttributes.addFlashAttribute("succes",
                    "Le post a été supprimé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        // Retour vers la liste de gestion (admin) ou la page d'accueil du blog
        if (blogService.estAdmin(principal.getName())) {
            return "redirect:/blog/mes-posts";
        }
        return "redirect:/blog";
    }
}
