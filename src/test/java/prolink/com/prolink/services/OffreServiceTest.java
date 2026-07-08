package prolink.com.prolink.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import prolink.com.prolink.dto.request.OffreDto;
import prolink.com.prolink.entities.JobOffer;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.StatutOffre;
import prolink.com.prolink.repositories.JobOfferRepository;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OffreServiceTest {

    @Autowired
    private OffreService offreService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @MockBean
    private NotificationService notificationService;

    private User recruteur;

    @BeforeEach
    void setUp() {
        var dtoRecruteur = new prolink.com.prolink.dto.request.InscriptionDto();
        dtoRecruteur.setPrenom("Charles");
        dtoRecruteur.setNom("Nkwi");
        dtoRecruteur.setEmail("charles.nkwi@gmail.com");
        dtoRecruteur.setPassword("Pass@1234");
        dtoRecruteur.setConfirmPassword("Pass@1234");
        dtoRecruteur.setRole(prolink.com.prolink.enums.RoleUtilisateur.RECRUTEUR);
        dtoRecruteur.setNomEntreprise("Nkwi Tech");
        recruteur = authService.inscrire(dtoRecruteur);
    }

    private OffreDto creerOffreDto(String titre, String typeContrat) {
        OffreDto dto = new OffreDto();
        dto.setTitre(titre);
        dto.setDescription("Description de " + titre);
        dto.setTypeContrat(typeContrat);
        dto.setLieu("Douala");
        dto.setRemuneration("300 000 FCFA");
        return dto;
    }

    private JobOffer creerOffre(String titre, String typeContrat, StatutOffre statut) {
        JobOffer offre = offreService.publierOffre(creerOffreDto(titre, typeContrat), "charles.nkwi@gmail.com");
        offre.setStatut(statut);
        return jobOfferRepository.save(offre);
    }

    @Test
    void publierOffre_avecSuccès() {
        JobOffer offre = offreService.publierOffre(creerOffreDto("Développeur", "CDI"), "charles.nkwi@gmail.com");
        assertNotNull(offre.getId());
        assertEquals("Développeur", offre.getTitre());
        assertEquals(StatutOffre.EN_ATTENTE, offre.getStatut());
    }

    @Test
    void getOffresPubliques_neRetourneQueApprouvees() {
        creerOffre("Offre 1", "CDI", StatutOffre.APPROUVEE);
        creerOffre("Offre 2", "Stage", StatutOffre.EN_ATTENTE);
        creerOffre("Offre 3", "Freelance", StatutOffre.APPROUVEE);
        List<JobOffer> publiques = offreService.getOffresPubliques();
        assertEquals(2, publiques.size());
        assertTrue(publiques.stream().allMatch(o -> o.getStatut() == StatutOffre.APPROUVEE));
    }

    @Test
    void getOffreParId_existante() {
        JobOffer offre = creerOffre("Dev", "CDI", StatutOffre.APPROUVEE);
        JobOffer trouvee = offreService.getOffreParId(offre.getId());
        assertEquals(offre.getId(), trouvee.getId());
    }

    @Test
    void getOffreParId_introuvable_lanceException() {
        assertThrows(IllegalArgumentException.class, () -> offreService.getOffreParId(999L));
    }

    @Test
    void rechercherOffres_parMotCle() {
        creerOffre("Développeur Java", "CDI", StatutOffre.APPROUVEE);
        creerOffre("Développeur Python", "CDI", StatutOffre.APPROUVEE);
        creerOffre("Designer UX", "Freelance", StatutOffre.APPROUVEE);
        List<JobOffer> resultats = offreService.rechercherOffres("Java");
        assertEquals(1, resultats.size());
        assertTrue(resultats.get(0).getTitre().contains("Java"));
    }

    @Test
    void getOffresPaginees_page0_taille2() {
        creerOffre("Offre A", "CDI", StatutOffre.APPROUVEE);
        creerOffre("Offre B", "Stage", StatutOffre.APPROUVEE);
        creerOffre("Offre C", "Freelance", StatutOffre.APPROUVEE);
        Page<JobOffer> page = offreService.getOffresPaginees(null, null, 0, 2);
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getNumberOfElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void getOffresPaginees_filtreTypeContrat() {
        creerOffre("Offre CDI 1", "CDI", StatutOffre.APPROUVEE);
        creerOffre("Offre CDI 2", "CDI", StatutOffre.APPROUVEE);
        creerOffre("Offre Stage", "Stage", StatutOffre.APPROUVEE);
        Page<JobOffer> page = offreService.getOffresPaginees(null, "CDI", 0, 10);
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(o -> "CDI".equalsIgnoreCase(o.getTypeContrat())));
    }

    @Test
    void getOffresPaginees_rechercheEtFiltre() {
        creerOffre("Développeur Java", "CDI", StatutOffre.APPROUVEE);
        creerOffre("Développeur Java Senior", "CDI", StatutOffre.APPROUVEE);
        creerOffre("Développeur PHP", "Freelance", StatutOffre.APPROUVEE);
        Page<JobOffer> page = offreService.getOffresPaginees("Java", "CDI", 0, 10);
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void archiverOffre_parProprietaire() {
        JobOffer offre = creerOffre("À archiver", "CDI", StatutOffre.APPROUVEE);
        offreService.archiverOffre(offre.getId(), "charles.nkwi@gmail.com");
        JobOffer archivee = offreService.getOffreParId(offre.getId());
        assertEquals(StatutOffre.ARCHIVEE, archivee.getStatut());
    }

    @Test
    void modifierOffre_nonAutorise_lanceException() {
        JobOffer offre = creerOffre("Original", "CDI", StatutOffre.EN_ATTENTE);
        assertThrows(IllegalStateException.class,
                () -> offreService.modifierOffre(offre.getId(), creerOffreDto("Modifié", "Stage"), "autre@email.com"));
    }

    @Test
    void getMesOffres() {
        creerOffre("Offre 1", "CDI", StatutOffre.APPROUVEE);
        creerOffre("Offre 2", "CDD", StatutOffre.EN_ATTENTE);
        List<JobOffer> mesOffres = offreService.getMesOffres("charles.nkwi@gmail.com");
        assertEquals(2, mesOffres.size());
    }

    @Test
    void getTypesContrat() {
        List<String> types = offreService.getTypesContrat();
        assertEquals(5, types.size());
        assertTrue(types.contains("CDI"));
        assertTrue(types.contains("Stage"));
        assertTrue(types.contains("Freelance"));
    }
}