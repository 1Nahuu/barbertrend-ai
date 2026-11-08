package com.barbertrend.repository;

import com.barbertrend.domain.Barber;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarberRepository extends JpaRepository<Barber, Long> {

    List<Barber> findByActiveTrueOrderBySortOrderAsc();

    Optional<Barber> findFirstByActiveTrue();
}
