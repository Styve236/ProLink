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
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/favicon.ico"
                        ).permitAll()

                        .requestMatchers(
                                "/",
                                "/index",
                                "/auth/inscription",
                                "/auth/connexion",
                                "/auth/deconnexion",
                                "/compte/en-attente",
                                "/offres",
                                "/offres/{id}"
                        ).permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .requestMatchers(
                                "/offres/nouvelle",
                                "/offres/*/modifier",
                                "/offres/*/supprimer",
                                "/candidatures/*/traiter"
                        ).hasRole("RECRUTEUR")

                        .requestMatchers(
                                "/candidatures/postuler/**",
                                "/documents/upload"
                        ).hasAnyRole("ETUDIANT", "FREELANCE")

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
                        .defaultSuccessUrl("/profil/dashboard", true)
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
                        .accessDeniedPage("/erreur/acces-refuse")
                )

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**", "/app/**", "/topic/**")
                );

        return http.build();
    }
}