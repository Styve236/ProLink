package prolink.com.prolink.controllers;

import com.lowagie.text.Document;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.RoleUtilisateur;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.services.AdminService;
import prolink.com.prolink.services.DocumentService;
import prolink.com.prolink.services.EmailService;
import prolink.com.prolink.services.NotificationService;
import prolink.com.prolink.services.OffreService;
import prolink.com.prolink.services.RapportService;


@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final OffreService offreService;
    private final NotificationService notificationService;
    private final DocumentService documentService;
    private final EmailService emailService;
    private final RapportService rapportService;
    private final TemplateEngine templateEngine;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", adminService.getStatsDashboard());
        return "admin/admin-dashboard";
    }

    @GetMapping("/users")
    public String listeUtilisateurs(Model model) {
        model.addAttribute("users", adminService.getTousLesUtilisateurs());
        return "admin/admin-users";
    }

    @GetMapping("/users/{role}")
    public String listeParRole(@PathVariable String role, Model model) {
        try {
            RoleUtilisateur r = RoleUtilisateur.valueOf(role.toUpperCase());
            model.addAttribute("users", adminService.getUtilisateursParRole(r));
            model.addAttribute("roleFiltre", role);
        } catch (IllegalArgumentException e) {
            model.addAttribute("users", adminService.getTousLesUtilisateurs());
        }
        return "admin/admin-users";
    }

    @GetMapping("/valider/{id}")
    public String afficherValidation(@PathVariable Long id, Model model) {
        model.addAttribute("utilisateur", adminService.getUtilisateurParId(id));
        return "admin/admin-validation";
    }

    @PostMapping("/valider")
    public String validerCompte(@RequestParam Long userId,
                                @RequestParam int trustScore,
                                @RequestParam(required = false) String commentaire,
                                RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> resultat = adminService.validerCompte(userId, trustScore, commentaire);
            boolean emailEnvoye = (boolean) resultat.get("emailEnvoye");
            String msg = "Compte validé et activé avec succès.";
            if (emailEnvoye) {
                msg += " Email de notification envoyé.";
            } else {
                msg += " Attention : l'email de notification n'a pas pu être envoyé (SMTP non configuré).";
            }
            redirectAttributes.addFlashAttribute("succes", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/bannir/{id}")
    public String bannirUtilisateur(@PathVariable Long id,
                                    @RequestParam(required = false) String raison,
                                    RedirectAttributes redirectAttributes) {
        try {
            adminService.changerStatutCompte(id, StatutCompte.SUSPENDU, raison);
            redirectAttributes.addFlashAttribute("succes", "Compte suspendu.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/archiver/{id}")
    public String archiverUtilisateur(@PathVariable Long id,
                                      RedirectAttributes redirectAttributes) {
        try {
            adminService.changerStatutCompte(id, StatutCompte.ARCHIVE, null);
            redirectAttributes.addFlashAttribute("succes", "Compte archivé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/reactiver/{id}")
    public String reactiverUtilisateur(@PathVariable Long id,
                                       RedirectAttributes redirectAttributes) {
        try {
            adminService.changerStatutCompte(id, StatutCompte.ACTIF, null);
            redirectAttributes.addFlashAttribute("succes", "Compte réactivé.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimerUtilisateur(@PathVariable Long id,
                                       RedirectAttributes redirectAttributes) {
        try {
            adminService.supprimerUtilisateur(id);
            redirectAttributes.addFlashAttribute("succes",
                    "Utilisateur supprimé définitivement.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/trust-score/{id}")
    public String mettreAJourTrustScore(@PathVariable Long id,
                                        @RequestParam int score,
                                        RedirectAttributes redirectAttributes) {
        try {
            adminService.mettreAJourTrustScore(id, score);
            redirectAttributes.addFlashAttribute("succes",
                    "Trust Score mis à jour : " + score + "/100");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/valider/" + id;
    }

    // TEST EMAIL
    @PostMapping("/test-email")
    public String testEmail(RedirectAttributes redirectAttributes) {
        boolean envoye = emailService.envoyerEmail(
                "testprolink919@gmail.com",
                "Test SMTP ProLink",
                "Bonjour,\n\nCeci est un email de test depuis ProLink.\n\nSi vous recevez ce message, le SMTP fonctionne !\n\nCordialement,\nL'équipe ProLink"
        );
        if (envoye) {
            redirectAttributes.addFlashAttribute("succes",
                    "Email de test envoyé à testprolink919@gmail.com. Vérifie ta boîte de réception.");
        } else {
            redirectAttributes.addFlashAttribute("erreur",
                    "ÉCHEC de l'envoi. Consulte les logs de la console pour le détail de l'erreur.");
        }
        return "redirect:/admin/dashboard";
    }

    // GESTION DES OFFRES

    @GetMapping("/offres/pending")
    public String offresEnAttente(Model model) {
        model.addAttribute("offres", offreService.getOffresEnAttente());
        return "admin/admin-offres-pending";
    }

    @PostMapping("/offres/approuver")
    public String approuverOffre(@RequestParam Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            offreService.approuverOffre(id);
            redirectAttributes.addFlashAttribute("succes", "Offre approuvée et publiée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/offres/pending";
    }

    @PostMapping("/offres/rejeter")
    public String rejeterOffre(@RequestParam Long id,
                               @RequestParam(required = false) String motif,
                               RedirectAttributes redirectAttributes) {
        try {
            offreService.rejeterOffre(id, motif);
            redirectAttributes.addFlashAttribute("succes", "Offre rejetée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/offres/pending";
    }

    // VALIDATION DES DOCUMENTS
    @GetMapping("/documents")
    public String documentsEnAttente(Model model) {
        model.addAttribute("documents", adminService.getDocumentsEnAttente());
        return "admin/admin-documents";
    }

    @PostMapping("/documents/valider")
    public String validerDocument(@RequestParam Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> resultat = adminService.validerDocument(id);
            boolean emailEnvoye = (boolean) resultat.get("emailEnvoye");
            String msg = "Document validé.";
            if (emailEnvoye) {
                msg += " Email de notification envoyé.";
            } else {
                msg += " Attention : l'email de notification n'a pas pu être envoyé (SMTP non configuré).";
            }
            redirectAttributes.addFlashAttribute("succes", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/documents";
    }

    @PostMapping("/documents/rejeter")
    public String rejeterDocument(@RequestParam Long id,
                                   @RequestParam(required = false) String commentaire,
                                   RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> resultat = adminService.rejeterDocument(id, commentaire);
            boolean emailEnvoye = (boolean) resultat.get("emailEnvoye");
            String msg = "Document rejeté.";
            if (emailEnvoye) {
                msg += " Email de notification envoyé.";
            } else {
                msg += " Attention : l'email de notification n'a pas pu être envoyé (SMTP non configuré).";
            }
            redirectAttributes.addFlashAttribute("succes", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/documents";
    }

    // TÉLÉCHARGEMENT D'UN DOCUMENT (pour visualisation par l'admin)
    @GetMapping("/documents/telecharger/{id}")
    public ResponseEntity<Resource> telechargerDocument(@PathVariable Long id) {
        try {
            prolink.com.prolink.entities.Document doc = documentService.getDocumentParId(id);
            java.nio.file.Path chemin = java.nio.file.Paths.get(doc.getCheminFichier());
            java.io.File fichier = chemin.toFile();
            if (!fichier.exists()) {
                return ResponseEntity.notFound().build();
            }
            InputStreamResource resource = new InputStreamResource(new java.io.FileInputStream(fichier));
            String contentType = doc.getTypeMime() != null ? doc.getTypeMime() : "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getNomFichier() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // RAPPORT D'ACTIVITÉS

    @GetMapping("/rapport")
    public String rapport(Model model,
                          @RequestParam(required = false) String debut,
                          @RequestParam(required = false) String fin) {
        LocalDate[] periode = resoudrePeriode(debut, fin);
        model.addAttribute("debut", periode[0].toString());
        model.addAttribute("fin", periode[1].toString());
        model.addAttribute("rapport", rapportService.genererRapport(periode[0], periode[1]));
        return "admin/admin-rapport";
    }

    @GetMapping("/rapport/export")
    public ResponseEntity<byte[]> exporterRapport(@RequestParam String format,
                                                  @RequestParam(required = false) String debut,
                                                  @RequestParam(required = false) String fin) {
        LocalDate[] periode = resoudrePeriode(debut, fin);
        Map<String, Object> rapport = rapportService.genererRapport(periode[0], periode[1]);
        String nomFichier = "rapport-activites-prolink_" + periode[0] + "_" + periode[1];

        try {
            if ("csv".equalsIgnoreCase(format)) {
                byte[] contenu = rapportService.genererCsv(rapport).getBytes("UTF-8");
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + nomFichier + ".csv\"")
                        .body(contenu);
            }

            if ("pdf".equalsIgnoreCase(format)) {
                Context ctx = new Context(Locale.FRENCH);
                ctx.setVariable("rapport", rapport);
                String html = templateEngine.process("admin/rapport-pdf", ctx);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Document pdfDoc = new Document();
                PdfWriter writer = PdfWriter.getInstance(pdfDoc, out);
                pdfDoc.open();
                HTMLWorker htmlWorker = new HTMLWorker(pdfDoc);
                htmlWorker.parse(new StringReader(html));
                pdfDoc.close();
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + nomFichier + ".pdf\"")
                        .body(out.toByteArray());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ("Erreur lors de l'export : " + e.getMessage()).getBytes());
        }
        return ResponseEntity.badRequest().build();
    }

    private LocalDate[] resoudrePeriode(String debut, String fin) {
        LocalDate d;
        LocalDate f;
        try {
            d = (debut == null || debut.isBlank())
                    ? LocalDate.now().withDayOfMonth(1) : LocalDate.parse(debut);
        } catch (DateTimeParseException e) {
            d = LocalDate.now().withDayOfMonth(1);
        }
        try {
            f = (fin == null || fin.isBlank())
                    ? LocalDate.now() : LocalDate.parse(fin);
        } catch (DateTimeParseException e) {
            f = LocalDate.now();
        }
        if (f.isBefore(d)) {
            LocalDate tmp = d;
            d = f;
            f = tmp;
        }
        return new LocalDate[]{d, f};
    }
}