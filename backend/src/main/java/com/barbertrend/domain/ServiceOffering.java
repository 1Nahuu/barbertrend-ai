package com.barbertrend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Servicio del catálogo: nombre, precio y duración.
 *
 * <p>Se desactiva con {@code active = false} en lugar de borrarse: los
 * turnos ya reservados siguen apuntando acá, y el historial no se rompe.
 */
@Entity
@Table(name = "service")
public class ServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 80, unique = true)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /** Precio en la moneda de la barbería (ARS). */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Duración NOMINAL en minutos.
     *
     * <p>Un corte no dura siempre lo mismo: depende del pelo y de lo que
     * pida el cliente. Este valor es el que la agenda usa por defecto para
     * saber cuántos bloques ocupa el turno. El dueño puede extender un turno
     * desde el panel cuando haga falta.
     */
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    protected ServiceOffering() {
        // Constructor para JPA. Las entidades no se crean con "new".
    }

    public ServiceOffering(String name, String description, BigDecimal price,
                           int durationMinutes, int sortOrder) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
        this.sortOrder = sortOrder;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
