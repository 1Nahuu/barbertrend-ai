package com.barbertrend.repository;

import com.barbertrend.domain.BusinessHours;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessHoursRepository extends JpaRepository<BusinessHours, Long> {

    List<BusinessHours> findAllByOrderByDayOfWeekAsc();

    Optional<BusinessHours> findByDayOfWeek(int dayOfWeek);
}
