package com.barbertrend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Turno reservado.
 *
 * <p><b>Los horarios se guardan como hora LOCAL de la barbería</b>, no en
 * UTC. Para una sede única con una sola zona horaria, guardar el momento
 * local simplifica el cálculo de "el bloque de las 10:00" y evita que H2 y
 * PostgreSQL se comporten de forma distinta.
 *
 * <p>Las entidades se cargan con {@code FetchType.LAZY} en las relaciones:
 * una lista de 50 turnos no tiene que traer 50 clientes y 50 servicios
 * siempre. Para acceder a un turno hay que estar dentro de una transacción,
 * y en la capa de servicios siempre se está.
 */
@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código corto y público (por ejemplo {@code BT-7K2M9Q}).
     *
     * <p>No es secuencial a propósito: si lo fuera, cualquiera podría
     * probarlos y ver los turnos de otros clientes.
     */
    @Column(name = "reference", nullable = false, length = 16, unique = true)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "cancellation_reason", length = 200)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Appointment() {
        // Constructor para JPA.
    }

    public Appointment(String reference, Customer customer, Barber barber,
                       ServiceOffering service, LocalDateTime startsAt,
                       LocalDateTime endsAt, LocalDateTime now) {
        this.reference = reference;
        this.customer = customer;
        this.barber = barber;
        this.service = service;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = AppointmentStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Libera el horario y guarda el motivo. */
    public void cancel(String reason, LocalDateTime now) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancellationReason = reason;
        this.updatedAt = now;
    }

    public void changeStatus(AppointmentStatus newStatus, LocalDateTime now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Barber getBarber() {
        return barber;
    }

    public ServiceOffering getService() {
        return service;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
