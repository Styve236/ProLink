package prolink.com.prolink.services;

import prolink.com.prolink.entities.User;
import prolink.com.prolink.enums.StatutCompte;
import prolink.com.prolink.repositories.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service chargé par Spring Security à chaque tentative de connexion.
 * IMPORTANT — logique de "enabled" :
 *   EN_ATTENTE → peut se connecter (enabled = true), mais ses ACTIONS
 *                sont bloquées au niveau Service via @RequiertCompteValide.
 *   ACTIF      → peut se connecter, toutes les actions débloquées.
 *   SUSPENDU   → ne peut PAS se connecter (enabled = false).
 *   ARCHIVE    → ne peut PAS se connecter (enabled = false).
 */
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun compte trouvé avec l'email : " + email
                ));

        boolean compteAutorise = user.getStatut() != StatutCompte.SUSPENDU
                && user.getStatut() != StatutCompte.ARCHIVE;

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                compteAutorise,  // enabled
                true,   // accountNonExpired
                true,   // credentialsNonExpired
                true,   // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}