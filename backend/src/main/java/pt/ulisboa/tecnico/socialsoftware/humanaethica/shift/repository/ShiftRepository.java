package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;

@Repository
@Transactional
public interface ShiftRepository extends JpaRepository<Shift, Integer> {
}
