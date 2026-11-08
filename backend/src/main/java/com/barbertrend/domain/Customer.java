package com.barbertrend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Cliente que reserva un turno.
 *
 * <p>Se guarda lo mínimo indispensable para poder avisarle y prestarle el
 * servicio: nombre y teléfono. No se piden documentos, ni dirección, ni
 * fecha de nacimiento, ni email.
 *
 * <p><b>Privacidad:</b> el teléfono se guarda SOLO con dígitos, para poder
 * compararlo sin depender de cómo lo escribió el cliente. Cuando se
 * muestra en un listado va enmascarado (ver {@code MaskingUtils}).
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** Solo dígitos, con código de país. Ej: 5491122334455 */
    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Customer() {
        // Constructor para JPA.
    }

    public Customer(String name, String phone, LocalDateTime createdAt) {
        this.name = name;
        this.phone = phone;
        this.createdAt = createdAt;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
