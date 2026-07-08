package prolink.com.prolink.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import prolink.com.prolink.dto.request.InscriptionDto;
import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.repositories.PasswordResetTokenRepository;
import prolink.com.prolink.repositories.UserRepository;

import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @MockBean
    private PasswordResetTokenRepository tokenRepository;

    private InscriptionDto dtoEtudiant;

    @BeforeEach
    void setUp() {
        dtoEtudiant = new InscriptionDto();
        dtoEtudiant.setPrenom("Jean");
        dtoEtudiant.setNom("Dupont");
        dtoEtudiant.setEmail("jean.dupont@gmail.com");
        dtoEtudiant.setPassword("Test@1234");
        dtoEtudiant.setConfirmPassword("Test@1234");
        dtoEtudiant.setRole(RoleUtilisateur.ETUDIANT);
        dtoEtudiant.setTelephone("612345678");
        dtoEtudiant.setVille("Douala");
        dtoEtudiant.setUniversite("Université de Douala");
        dtoEtudiant.setFiliere("Informatique");
        dtoEtudiant.setNiveauEtude("Master 1");
    }

    @Test
    void inscrire_avecSuccès() {
        User user = authService.inscrire(dtoEtudiant);
        assertNotNull(user.getId());
        assertEquals("jean.dupont@gmail.com", user.getEmail());
        assertEquals(RoleUtilisateur.ETUDIANT, user.getRole());
        assertTrue(passwordEncoder.matches("Test@1234", user.getPassword()));
    }

    @Test
    void inscrire_emailExistant_lanceException() {
        authService.inscrire(dtoEtudiant);
        assertThrows(IllegalArgumentException.class, () -> authService.inscrire(dtoEtudiant));
    }

    @Test
    void inscrire_emailNonGmail_lanceException() {
        dtoEtudiant.setEmail("jean@yahoo.fr");
        assertThrows(IllegalArgumentException.class, () -> authService.inscrire(dtoEtudiant));
    }

    @Test
    void inscrire_passwordMismatch_lanceException() {
        dtoEtudiant.setConfirmPassword("WrongPass@1");
        assertThrows(IllegalArgumentException.class, () -> authService.inscrire(dtoEtudiant));
    }

    @Test
    void inscrire_roleAdmin_lanceException() {
        dtoEtudiant.setRole(RoleUtilisateur.ADMIN);
        assertThrows(IllegalArgumentException.class, () -> authService.inscrire(dtoEtudiant));
    }

    @Test
    void inscrire_recruteur_sansEntreprise_lanceException() {
        dtoEtudiant.setRole(RoleUtilisateur.RECRUTEUR);
        dtoEtudiant.setNomEntreprise(null);
        assertThrows(IllegalArgumentException.class, () -> authService.inscrire(dtoEtudiant));
    }

    @Test
    void inscrire_recruteur_avecSuccès() {
        dtoEtudiant.setRole(RoleUtilisateur.RECRUTEUR);
        dtoEtudiant.setNomEntreprise("Tech Corp");
        dtoEtudiant.setSecteurActivite("IT");
        User user = authService.inscrire(dtoEtudiant);
        assertNotNull(user.getId());
        assertEquals(RoleUtilisateur.RECRUTEUR, user.getRole());
    }

    @Test
    void getUtilisateurConnecte_existant() {
        authService.inscrire(dtoEtudiant);
        User user = authService.getUtilisateurConnecte("jean.dupont@gmail.com");
        assertNotNull(user);
        assertEquals("jean.dupont@gmail.com", user.getEmail());
    }

    @Test
    void getUtilisateurConnecte_introuvable_lanceException() {
        assertThrows(IllegalStateException.class,
                () -> authService.getUtilisateurConnecte("inconnu@gmail.com"));
    }
}