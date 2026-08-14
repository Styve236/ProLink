package prolink.com.prolink.services;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.enums.StatutCandidature;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.enums.StatutOffre;
import prolink.com.prolink.enums.StatutValidation;
import prolink.com.prolink.repositories.CandidatureRepository;
import prolink.com.prolink.repositories.ChatMessageRepository;
import prolink.com.prolink.repositories.DocumentRepository;
import prolink.com.prolink.repositories.JobOfferRepository;
import prolink.com.prolink.repositories.LinkActionRepository;
import prolink.com.prolink.repositories.MessageRepository;
import prolink.com.prolink.repositories.NotificationRepository;
import prolink.com.prolink.repositories.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Génère le rapport d'activités complet de la plateforme pour une période donnée.
 *
 * Le rapport contient :
 *  - une vue d'ensemble cumulative (toutes les données depuis le début)
 *  - l'activité sur la période choisie (inscriptions, offres, candidatures,
 *    documents, messages, connexions, notifications)
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class RapportService {

    private final UserRepository userRepository;
    private final JobOfferRepository jobOfferRepository;
    private final CandidatureRepository candidatureRepository;
    private final DocumentRepository documentRepository;
    private final MessageRepository messageRepository;
    private final LinkActionRepository linkActionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationRepository notificationRepository;

    public RapportService(UserRepository userRepository,
                          JobOfferRepository jobOfferRepository,
                          CandidatureRepository candidatureRepository,
                          DocumentRepository documentRepository,
                          MessageRepository messageRepository,
                          LinkActionRepository linkActionRepository,
                          ChatMessageRepository chatMessageRepository,
                          NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.candidatureRepository = candidatureRepository;
        this.documentRepository = documentRepository;
        this.messageRepository = messageRepository;
        this.linkActionRepository = linkActionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Construit la fiche de rapport pour la période [debut, fin].
     */
    public Map<String, Object> genererRapport(LocalDate debut, LocalDate fin) {
        if (fin.isBefore(debut)) {
            LocalDate tmp = debut;
            debut = fin;
            fin = tmp;
        }

        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT = fin.atTime(LocalTime.MAX);

        Map<String, Object> rapport = new LinkedHashMap<>();

        // ── Informations du rapport ──
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        rapport.put("debut", debut.format(fmt));
        rapport.put("fin", fin.format(fmt));
        rapport.put("genereLe", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // ── Vue d'ensemble (cumul depuis le début) ──
        rapport.put("totalUtilisateurs", userRepository.count());
        rapport.put("totalEtudiants", userRepository.countByRole(RoleUtilisateur.ETUDIANT));
        rapport.put("totalFreelances", userRepository.countByRole(RoleUtilisateur.FREELANCE));
        rapport.put("totalRecruteurs", userRepository.countByRole(RoleUtilisateur.RECRUTEUR));
        rapport.put("totalOffres", jobOfferRepository.count());
        rapport.put("totalCandidatures", candidatureRepository.count());
        rapport.put("totalDocuments", documentRepository.count());
        rapport.put("totalMessages", messageRepository.count());

        // ── Activité sur la période : utilisateurs ──
        rapport.put("inscriptionsPeriode",
                userRepository.countByDateInscriptionBetween(debutDT, finDT));
        rapport.put("inscriptionsEtudiants",
                userRepository.countByRoleAndDateInscriptionBetween(
                        RoleUtilisateur.ETUDIANT, debutDT, finDT));
        rapport.put("inscriptionsFreelances",
                userRepository.countByRoleAndDateInscriptionBetween(
                        RoleUtilisateur.FREELANCE, debutDT, finDT));
        rapport.put("inscriptionsRecruteurs",
                userRepository.countByRoleAndDateInscriptionBetween(
                        RoleUtilisateur.RECRUTEUR, debutDT, finDT));
        rapport.put("comptesEnAttente",
                userRepository.countByStatutAndDateInscriptionBetween(
                        StatutCompte.EN_ATTENTE, debutDT, finDT));

        // ── Activité sur la période : offres ──
        rapport.put("offresPeriode",
                jobOfferRepository.countByDatePublicationBetween(debutDT, finDT));
        rapport.put("offresApprouvees",
                jobOfferRepository.countByStatutAndDatePublicationBetween(
                        StatutOffre.APPROUVEE, debutDT, finDT));
        rapport.put("offresEnAttente",
                jobOfferRepository.countByStatutAndDatePublicationBetween(
                        StatutOffre.EN_ATTENTE, debutDT, finDT));

        // ── Activité sur la période : candidatures ──
        rapport.put("candidaturesPeriode",
                candidatureRepository.countByDateCandidatureBetween(debutDT, finDT));
        rapport.put("candidaturesAcceptees",
                candidatureRepository.countByStatutAndDateCandidatureBetween(
                        StatutCandidature.ACCEPTEE, debutDT, finDT));
        rapport.put("candidaturesRefusees",
                candidatureRepository.countByStatutAndDateCandidatureBetween(
                        StatutCandidature.REFUSEE, debutDT, finDT));

        // ── Activité sur la période : documents ──
        rapport.put("documentsPeriode",
                documentRepository.countByDateDepotBetween(debutDT, finDT));
        rapport.put("documentsValides",
                documentRepository.countByStatutValidationAndDateDepotBetween(
                        StatutValidation.VALIDE, debutDT, finDT));

        // ── Activité sur la période : échanges ──
        rapport.put("messagesPeriode",
                messageRepository.countByDateEnvoiBetween(debutDT, finDT));
        rapport.put("chatPeriode",
                chatMessageRepository.countByHorodatageBetween(debutDT, finDT));
        rapport.put("connexionsPeriode",
                linkActionRepository.countByDateDemandeBetween(debutDT, finDT));
        rapport.put("notificationsPeriode",
                notificationRepository.countByDateCreationBetween(debutDT, finDT));

        // ── Top recruteurs sur la période (5 max) ──
        List<Object[]> top = jobOfferRepository.topRecruteursParOffres(debutDT, finDT);
        rapport.put("topRecruteurs", top.size() > 5 ? top.subList(0, 5) : top);

        return rapport;
    }

    /**
     * Version CSV (séparateur ; pour Excel FR) de la fiche.
     */
    public String genererCsv(Map<String, Object> rapport) {
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM UTF-8 pour Excel
        sb.append("Rapport d'activités ProLink du ")
          .append(rapport.get("debut"))
          .append(" au ")
          .append(rapport.get("fin"))
          .append("\r\n\r\n");
        sb.append("Indicateur;Valeur\r\n");

        // Vue d'ensemble
        ajouterLigneCsv(sb, "Total utilisateurs", rapport.get("totalUtilisateurs"));
        ajouterLigneCsv(sb, "  dont étudiants", rapport.get("totalEtudiants"));
        ajouterLigneCsv(sb, "  dont freelances", rapport.get("totalFreelances"));
        ajouterLigneCsv(sb, "  dont recruteurs", rapport.get("totalRecruteurs"));
        ajouterLigneCsv(sb, "Total offres", rapport.get("totalOffres"));
        ajouterLigneCsv(sb, "Total candidatures", rapport.get("totalCandidatures"));
        ajouterLigneCsv(sb, "Total documents", rapport.get("totalDocuments"));
        ajouterLigneCsv(sb, "Total messages", rapport.get("totalMessages"));

        sb.append("\r\nActivité sur la période\r\n");
        ajouterLigneCsv(sb, "Inscriptions", rapport.get("inscriptionsPeriode"));
        ajouterLigneCsv(sb, "  dont étudiants", rapport.get("inscriptionsEtudiants"));
        ajouterLigneCsv(sb, "  dont freelances", rapport.get("inscriptionsFreelances"));
        ajouterLigneCsv(sb, "  dont recruteurs", rapport.get("inscriptionsRecruteurs"));
        ajouterLigneCsv(sb, "Comptes en attente de validation", rapport.get("comptesEnAttente"));
        ajouterLigneCsv(sb, "Offres publiées", rapport.get("offresPeriode"));
        ajouterLigneCsv(sb, "  dont approuvées", rapport.get("offresApprouvees"));
        ajouterLigneCsv(sb, "  dont en attente", rapport.get("offresEnAttente"));
        ajouterLigneCsv(sb, "Candidatures", rapport.get("candidaturesPeriode"));
        ajouterLigneCsv(sb, "  dont acceptées", rapport.get("candidaturesAcceptees"));
        ajouterLigneCsv(sb, "  dont refusées", rapport.get("candidaturesRefusees"));
        ajouterLigneCsv(sb, "Documents déposés", rapport.get("documentsPeriode"));
        ajouterLigneCsv(sb, "  dont validés", rapport.get("documentsValides"));
        ajouterLigneCsv(sb, "Messages privés", rapport.get("messagesPeriode"));
        ajouterLigneCsv(sb, "Messages chat temps réel", rapport.get("chatPeriode"));
        ajouterLigneCsv(sb, "Demandes de connexion", rapport.get("connexionsPeriode"));
        ajouterLigneCsv(sb, "Notifications", rapport.get("notificationsPeriode"));
        return sb.toString();
    }

    private void ajouterLigneCsv(StringBuilder sb, String libelle, Object valeur) {
        sb.append(libelle).append(';').append(valeur).append("\r\n");
    }
}
