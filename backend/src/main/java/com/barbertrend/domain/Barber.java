package com.barbertrend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Barbero que atiende los turnos.
 *
 * <p>Hoy hay uno solo, pero la tabla existe igual: la agenda y la garantía de
 * no superposición funcionan por barbero, así que agregar un segundo más
 * adelante no obliga a migrar los datos que ya hay guardados.
 */
@Entity
@Table(name = "barber")
public class Barber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    protected Barber() {
        // Constructor para JPA.
    }

    public Barber(String name, int sortOrder) {
        this.name = name;
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
