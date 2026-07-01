package  prolink.com.prolink.config;

import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.RoleUtilisateur;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    private static final String ADMIN_EMAIL    = "admin@prolink.cm";
    private static final String ADMIN_NOM      = "Administrateur";
    private static final String ADMIN_PRENOM   = "ProLink";

    @Value("${admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {

            boolean adminExiste = userRepository
                    .existsByEmailAndRole(ADMIN_EMAIL, RoleUtilisateur.ADMIN);

            if (!adminExiste) {
                if ("Admin@ProLink2025!".equals(adminPassword)) {
                    System.out.println();
                    System.out.println("╔══════════════════════════════════════════════════╗");
                    System.out.println("║  ⚠  ADMIN_PASSWORD non définie !               ║");
                    System.out.println("║  Utilisation du mot de passe par défaut.       ║");
                    System.out.println("║  Définissez la variable d'environnement        ║");
                    System.out.println("║  ADMIN_PASSWORD pour sécuriser le compte.      ║");
                    System.out.println("╚══════════════════════════════════════════════════╝");
                    System.out.println();
                }
                User admin = new User();
                admin.setEmail(ADMIN_EMAIL);
                admin.setPassword(passwordEncoder.encode(adminPassword));
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
                System.out.println("║  Password : " + String.format("%-25s", adminPassword) + "║");
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