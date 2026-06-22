package prolink.com.prolink.services;

import prolink.com.prolink.entities.Candidature;
import prolink.com.prolink.entities.JobOffer;
import prolink.com.prolink.entities.Notification;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.TypeNotification;
import prolink.com.prolink.repositories.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // MÉTHODES PUBLIQUES — appelées par les autres services
    public void notifierNouvelleCandidature(Candidature candidature) {
        creer(
                candidature.getOffre().getRecruteur(),
                "Nouvelle candidature reçue pour : " + candidature.getOffre().getTitre(),
                TypeNotification.CANDIDATURE_RECUE,
                "/candidatures/offre/" + candidature.getOffre().getId()
        );
    }

    public void notifierCandidatureAcceptee(Candidature candidature) {
        creer(
                candidature.getCandidat(),
                "Félicitations ! Votre candidature pour \""
                        + candidature.getOffre().getTitre() + "\" a été acceptée.",
                TypeNotification.CANDIDATURE_ACCEPTEE,
                "/offres/candidatures"
        );
    }

    public void notifierCandidatureRefusee(Candidature candidature) {
        creer(
                candidature.getCandidat(),
                "Votre candidature pour \""
                        + candidature.getOffre().getTitre() + "\" n'a pas été retenue.",
                TypeNotification.CANDIDATURE_REFUSEE,
                "/offres/candidatures"
        );
    }

    public void notifierOffreApprouvee(JobOffer offre) {
        creer(
                offre.getRecruteur(),
                "Votre offre \"" + offre.getTitre() + "\" est maintenant en ligne.",
                TypeNotification.OFFRE_APPROUVEE,
                "/offres/" + offre.getId()
        );
    }

    public void notifierDocumentValide(User utilisateur, String nomDocument) {
        creer(
                utilisateur,
                "Votre document \"" + nomDocument + "\" a été validé.",
                TypeNotification.DOCUMENT_VALIDE,
                "/profil/mes-documents"
        );
    }

    public void notifierDocumentRejete(User utilisateur, String nomDocument) {
        creer(
                utilisateur,
                "Votre document \"" + nomDocument + "\" a été rejeté. " +
                        "Consultez le commentaire de l'administrateur.",
                TypeNotification.DOCUMENT_REJETE,
                "/profil/mes-documents"
        );
    }

    // ← MÉTHODE AJOUTÉE — appelée par AdminService
    public void notifierTrustScoreMisAJour(User utilisateur, int score) {
        creer(
                utilisateur,
                "Votre Trust Score a été mis à jour : " + score + "/100.",
                TypeNotification.TRUST_SCORE_MIS_A_JOUR,
                "/profil/mon-profil"
        );
    }

    // CONSULTATION
    @Transactional(readOnly = true)
    public List<Notification> getMesNotifications(User utilisateur) {
        return notificationRepository.findByUtilisateurOrderByDateCreationDesc(utilisateur);
    }

    @Transactional(readOnly = true)
    public long compterNonLues(User utilisateur) {
        return notificationRepository.countByUtilisateurAndLueFalse(utilisateur);
    }

    public void marquerToutesLues(User utilisateur) {
        List<Notification> nonLues = notificationRepository
                .findByUtilisateurAndLueFalse(utilisateur);
        nonLues.forEach(n -> n.setLue(true));
        notificationRepository.saveAll(nonLues);
    }

    // MÉTHODE UTILITAIRE PRIVÉE
    private void creer(User destinataire, String contenu,
                       TypeNotification type, String lienAction) {
        Notification notification = new Notification();
        notification.setUtilisateur(destinataire);
        notification.setContenu(contenu);
        notification.setType(type);
        notification.setLienAction(lienAction);
        notification.setLue(false);
        notificationRepository.save(notification);
    }

    //notification pour une nouvelle connexion

    public void notifierNouvelleDemandeConnexion(User cible, User demandeur) {
        creer(
                cible,
                demandeur.getNomComplet() + " souhaite se connecter avec vous.",
                TypeNotification.DEMANDE_CONNEXION,
                "/connexions/recues"
        );
    }}