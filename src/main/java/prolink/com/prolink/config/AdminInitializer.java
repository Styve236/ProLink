package  prolink.com.prolink.config;

import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    private static final String ADMIN_EMAIL    = "admin@prolink.cm";
    private static final String ADMIN_PASSWORD = "Admin@ProLink2025!";
    private static final String ADMIN_NOM      = "Administrateur";
    private static final String ADMIN_PRENOM   = "ProLink";

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {

            boolean adminExiste = userRepository
                    .existsByEmailAndRole(ADMIN_EMAIL, RoleUtilisateur.ADMIN);

            if (!adminExiste) {
                User admin = new User();
                admin.setEmail(ADMIN_EMAIL);
                admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
                admin.setNom(ADMIN_NOM);
                admin.setPrenom(ADMIN_PRENOM);
                admin.setRole(RoleUtilisateur.ADMIN);
                admin.setStatut(StatutCompte.ACTIF);
                admin.setTrustScore(100);

                userRepository.save(admin);

                System.out.println();
                System.out.println("╔══════════════════════════════════════════════════╗");
                System.out.println("║           PROLINK — COMPTE ADMIN CRÉÉ            ║");
                System.out.println("╠══════════════════════════════════════════════════╣");
                System.out.println("║  Email    : admin@prolink.cm                     ║");
                System.out.println("║  Password : Admin@ProLink2025!                   ║");
                System.out.println("║  Rôle     : ADMIN                                ║");
                System.out.println("╠══════════════════════════════════════════════════╣");
                System.out.println("║  ⚠  Changez ces identifiants avant la démo !    ║");
                System.out.println("╚══════════════════════════════════════════════════╝");
                System.out.println();

            } else {
                System.out.println();
                System.out.println("╔══════════════════════════════════════════════════╗");
                System.out.println("║         PROLINK — ADMIN DÉJÀ EXISTANT            ║");
                System.out.println("╠══════════════════════════════════════════════════╣");
                System.out.println("║  Email    : admin@prolink.cm                     ║");
                System.out.println("║  Statut   : Compte actif en base de données      ║");
                System.out.println("╚══════════════════════════════════════════════════╝");
                System.out.println();
            }
        };
    }
}