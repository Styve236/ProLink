package prolink.com.prolink.config;

import prolink.com.prolink.services.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ----------------------------------------------------------------
    // Spring Boot 4 — DaoAuthenticationProvider prend maintenant
    // le UserDetailsService directement dans le constructeur.
    // L'ancien new DaoAuthenticationProvider() sans argument n'existe plus.
    // ----------------------------------------------------------------
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String redirect = request.getParameter("redirect");
            if (redirect != null && !redirect.isBlank()
                    && (redirect.startsWith("/") && !redirect.startsWith("//"))) {
                response.sendRedirect(redirect);
            } else {
                response.sendRedirect("/profil/dashboard");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // 1. Ressources statiques — toujours en premier
                        .requestMatchers(
                                "/css/**", "/js/**", "/img/**", "/lib/**",
                                "/webjars/**", "/favicon.ico"
                        ).permitAll()

                        // 2. Pages publiques générales
                        .requestMatchers(
                                "/",
                                "/index",
                                "/auth/inscription",
                                "/auth/connexion",
                                "/auth/deconnexion",
                                "/compte/en-attente",
                                "/erreur/**",
                                "/offres",
                                "/a-propos",
                                "/contact"
                        ).permitAll()

                        // 3. Admin uniquement
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 4. Routes Recruteur — AVANT /offres/{id} pour éviter le conflit de pattern
                        .requestMatchers(
                                "/offres/offre-form",
                                "/offres/nouvelle",
                                "/offres/enregistrer",
                                "/offres/mes-offres",
                                "/offres/*/modifier",
                                "/offres/*/supprimer",
                                "/offres/*/archiver",
                                "/offres/*/candidatures",
                                "/offres/candidatures/*/accepter",
                                "/offres/candidatures/*/refuser"
                        ).hasRole("RECRUTEUR")

                        // 5. Détail d'une offre publique — APRÈS les routes spécifiques
                        .requestMatchers("/offres/*").permitAll()

                        // 6. Routes Étudiant / Freelance
                        .requestMatchers(
                                "/candidatures/postuler/**",
                                "/documents/upload"
                        ).hasAnyRole("ETUDIANT", "FREELANCE")

                        // 7. Routes authentifiées
                        .requestMatchers(
                                "/profil/**",
                                "/messages/**",
                                "/chat/**",
                                "/notifications/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/auth/connexion")
                        .loginProcessingUrl("/auth/connexion")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler())
                        .failureUrl("/auth/connexion?erreur=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/auth/deconnexion")
                        .logoutSuccessUrl("/auth/connexion?deconnecte=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/erreur/acces-refuse")                )

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**", "/app/**", "/topic/**")
                );

        return http.build();
    }
}