package com.admin.adminlfarma_mini.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asistencias")
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "hora_entrada", nullable = false)
    private LocalDateTime horaEntrada;

    @Column(name = "hora_salida")
    private LocalDateTime horaSalida;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha; // Para queries por día sin calcular

    @Column(length = 255)
    private String observaciones;

    @PrePersist
    protected void prePersist() {
        if (this.fecha == null && this.horaEntrada != null) {
            this.fecha = this.horaEntrada.toLocalDate();
        }
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalDateTime horaEntrada) {
        this.horaEntrada = horaEntrada;
        if (horaEntrada != null) this.fecha = horaEntrada.toLocalDate();
    }

    public LocalDateTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalDateTime horaSalida) { this.horaSalida = horaSalida; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    /** Calcula horas trabajadas si hay entrada y salida */
    public String getHorasTrabajadas() {
        if (horaEntrada == null || horaSalida == null) return "En curso";
        long minutos = java.time.Duration.between(horaEntrada, horaSalida).toMinutes();
        return String.format("%dh %02dmin", minutos / 60, minutos % 60);
    }
}
