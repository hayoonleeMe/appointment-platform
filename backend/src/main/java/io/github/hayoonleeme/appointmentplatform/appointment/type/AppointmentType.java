package io.github.hayoonleeme.appointmentplatform.appointment.type;

import io.github.hayoonleeme.appointmentplatform.operator.Operator;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
public class AppointmentType {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter
  private Long id;

  @Column(nullable = false, length = 50)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private AppointmentMethod appointmentMethod;

  @Column(nullable = false)
  private short durationMinutes;

  @Column(nullable = false)
  private short preparationMinutes;

  private Short startIntervalMinutes;

  @Column(nullable = false)
  @Getter
  private boolean active;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "operator_id", nullable = false)
  private Operator operator;

  protected AppointmentType() {}

  public AppointmentType(
      String name,
      AppointmentMethod appointmentMethod,
      Short durationMinutes,
      Short preparationMinutes,
      Short startIntervalMinutes,
      boolean active,
      Operator operator) {
    if (name == null || name.isBlank() || name.length() > 50) {
      throw new IllegalArgumentException("name must be non-blank and at most 50 characters");
    }
    if (appointmentMethod == null) {
      throw new IllegalArgumentException("appointmentMethod must not be null");
    }
    if (durationMinutes == null || durationMinutes <= 0) {
      throw new IllegalArgumentException("durationMinutes must be positive");
    }
    if (preparationMinutes == null || preparationMinutes < 0) {
      throw new IllegalArgumentException("preparationMinutes must be zero or positive");
    }
    if (operator == null) {
      throw new IllegalArgumentException("operator must not be null");
    }
    if (appointmentMethod == AppointmentMethod.ONE_ON_ONE
        && (startIntervalMinutes == null || startIntervalMinutes <= 0)) {
      throw new IllegalArgumentException("startIntervalMinutes must be positive for ONE_ON_ONE");
    }
    if (appointmentMethod == AppointmentMethod.GROUP && startIntervalMinutes != null) {
      throw new IllegalArgumentException("startIntervalMinutes must be null for GROUP");
    }
    this.name = name;
    this.appointmentMethod = appointmentMethod;
    this.durationMinutes = durationMinutes;
    this.preparationMinutes = preparationMinutes;
    this.startIntervalMinutes = startIntervalMinutes;
    this.active = active;
    this.operator = operator;
  }

  public void updateActive(boolean active) {
    this.active = active;
  }
}
