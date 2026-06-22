package prolink.com.prolink.repositories;

import prolink.com.prolink.entities.Freelance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FreelanceRepository extends JpaRepository<Freelance, Long> {
    Optional<Freelance> findByEmail(String email);
    boolean existsByEmail(String email);
}