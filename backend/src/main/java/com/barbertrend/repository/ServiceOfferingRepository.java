package com.barbertrend.repository;

import com.barbertrend.domain.ServiceOffering;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos del catálogo de servicios.
 *
 * <p>Los métodos los implementa Spring Data en tiempo de ejecución: el
 * nombre tiene que seguir el patrón "findBy" + condición.
 */
public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    /** Servicios que se muestran en la web, en el orden del catálogo. */
    List<ServiceOffering> findByActiveTrueOrderBySortOrderAsc();

    Optional<ServiceOffering> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
