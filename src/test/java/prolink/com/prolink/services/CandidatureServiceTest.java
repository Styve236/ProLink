package prolink.com.prolink.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import prolink.com.prolink.dto.request.CandidatureDto;
import prolink.com.prolink.dto.request.OffreDto;
import prolink.com.prolink.entities.Candidature;
import prolink.com.prolink.entities.JobOffer;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.StatutOffre;
import prolink.com.prolink.repositories.CandidatureRepository;
import prolink.com.prolink.repositories.JobOfferRepository;
import prolink.com.prolink.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CandidatureServiceTest {

    @Autowired
    private CandidatureService candidatureService;

    @Autowired
    private AuthService authService;

    @Autowired
    private OffreService offreService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidatureRepository candidatureRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @MockBean
    private EmailService emailService;

    @MockBean
    private NotificationService notificationService;

    private User candidat;
    private User recruteur;
    private JobOffer offre;

    @BeforeEach
    void setUp() {
        // Utilisateur authentifié avec le rôle ETUDIANT (nécessaire pour @PreAuthorize)
        authentifierComme("ETUDIANT");
        var dtoCandidat = new prolink.com.prolink.dto.request.InscriptionDto();
        dtoCandidat.setPrenom("Alice");
        dtoCandidat.setNom("Martin");
        dtoCandidat.setEmail("alice.martin@gmail.com");
        dtoCandidat.setPassword("Pass@1234");
        dtoCandidat.setConfirmPassword("Pass@1234");
        dtoCandidat.setRole(prolink.com.prolink.enums.RoleUtilisateur.ETUDIANT);
        dtoCandidat.setUniversite("Université de Yaoundé");
        dtoCandidat.setFiliere("Informatique");
        dtoCandidat.setNiveauEtude("Licence 3");
        candidat = authService.inscrire(dtoCandidat);

        var dtoRecruteur = new prolink.com.prolink.dto.request.InscriptionDto();
        dtoRecruteur.setPrenom("Bob");
        dtoRecruteur.setNom("Kamga");
        dtoRecruteur.setEmail("bob.kamga@gmail.com");
        dtoRecruteur.setPassword("Pass@5678");
        dtoRecruteur.setConfirmPassword("Pass@5678");
        dtoRecruteur.setRole(prolink.com.prolink.enums.RoleUtilisateur.RECRUTEUR);
        dtoRecruteur.setNomEntreprise("ProLink Inc.");
        recruteur = authService.inscrire(dtoRecruteur);

        OffreDto offreDto = new OffreDto();
        offreDto.setTitre("Développeur Java");
        offreDto.setDescription("Description du poste");
        offreDto.setTypeContrat("CDI");
        offreDto.setLieu("Douala");
        offreDto.setRemuneration("500 000 FCFA");
        offre = offreService.publierOffre(offreDto, "bob.kamga@gmail.com");
        offre.setStatut(StatutOffre.APPROUVEE);
        jobOfferRepository.save(offre);
    }

    @Test
    void postuler_avecSuccès() {
        CandidatureDto dto = new CandidatureDto();
        dto.setMessageMotivation("Je suis très motivé !");
        Candidature c = candidatureService.postuler(offre.getId(), dto, "alice.martin@gmail.com");
        assertNotNull(c.getId());
        assertEquals(prolink.com.prolink.enums.StatutCandidature.EN_ATTENTE, c.getStatut());
        assertEquals(candidat.getId(), c.getCandidat().getId());
    }

    @Test
    void postuler_offreNonApprouvee_lanceException() {
        offre.setStatut(StatutOffre.EN_ATTENTE);
        jobOfferRepository.save(offre);
        CandidatureDto dto = new CandidatureDto();
        dto.setMessageMotivation("Motivé");
        assertThrows(IllegalStateException.class,
                () -> candidatureService.postuler(offre.getId(), dto, "alice.martin@gmail.com"));
    }

    @Test
    void postuler_deuxFois_lanceException() {
        CandidatureDto dto = new CandidatureDto();
        dto.setMessageMotivation("Motivé");
        candidatureService.postuler(offre.getId(), dto, "alice.martin@gmail.com");
        assertThrows(IllegalStateException.class,
                () -> candidatureService.postuler(offre.getId(), dto, "alice.martin@gmail.com"));
    }

    @Test
    void postuler_recruteurSurSaPropreOffre_lanceException() {
        CandidatureDto dto = new CandidatureDto();
        dto.setMessageMotivation("Motivé");
        assertThrows(IllegalStateException.class,
                () -> candidatureService.postuler(offre.getId(), dto, "bob.kamga@gmail.com"));
    }

    @Test
    void getMesCandidatures() {
        CandidatureDto dto = new CandidatureDto();
        dto.setMessageMotivation("Motivé");
        candidatureService.postuler(offre.getId(), dto, "alice.martin@gmail.com");
        List<Candidature> mesCandidatures = candidatureService.getMesCandidatures("alice.martin@gmail.com");
        assertEquals(1, mesCandidatures.size());
    }

    @Test
    void getCandidaturesDuneOffre_nonAutorise_lanceException() {
        CandidatureDto dto = new CandidatureDto();
        dto.setMessageMotivation("Motivé");
        candidatureService.postuler(offre.getId(), dto, "alice.martin@gmail.com");
        // Le recruteur n'est pas autorisé à voir les candidatures de cet offre qu'il ne possède pas
        authentifierComme("RECRUTEUR");
        assertThrows(IllegalStateException.class,
                () -> candidatureService.getCandidaturesDuneOffre(offre.getId(), "alice.martin@gmail.com"));
    }

    @Test
    void aDejaPostule_true() {
        CandidatureDto dto = new CandidatureDto();
        dto.setMessageMotivation("Motivé");
        candidatureService.postuler(offre.getId(), dto, "alice.martin@gmail.com");
        assertTrue(candidatureService.aDejaPostule(offre.getId(), "alice.martin@gmail.com"));
    }

    @Test
    void aDejaPostule_false() {
        assertFalse(candidatureService.aDejaPostule(offre.getId(), "alice.martin@gmail.com"));
    }

    private void authentifierComme(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test-user", "test-password",
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}