package com.softfruver.inventario.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "proveedor", schema = "softfruver")
public class Proveedor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nombre;

  private String telefono;

  @Column(name = "archived_at")
  private OffsetDateTime archivedAt;

  @Column(name = "creado_en")
  private OffsetDateTime creadoEn;

  @Column(name = "actualizado_en")
  private OffsetDateTime actualizadoEn;

  // ===== Getters/Setters =====
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }

  public String getTelefono() { return telefono; }
  public void setTelefono(String telefono) { this.telefono = telefono; }

  public OffsetDateTime getArchivedAt() { return archivedAt; }
  public void setArchivedAt(OffsetDateTime archivedAt) { this.archivedAt = archivedAt; }

  public OffsetDateTime getCreadoEn() { return creadoEn; }
  public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }

  public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
  public void setActualizadoEn(OffsetDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
