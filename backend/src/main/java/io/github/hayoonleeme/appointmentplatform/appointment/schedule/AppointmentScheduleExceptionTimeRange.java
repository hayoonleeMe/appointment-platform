package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.Getter;

@Entity
public class AppointmentScheduleExceptionTimeRange {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private AppointmentScheduleException appointmentScheduleException;

  @Column(nullable = false)
  @Getter
  private LocalTime startTime;

  @Column(nullable = false)
  @Getter
  private LocalTime endTime;

  protected AppointmentScheduleExceptionTimeRange() {}

  public AppointmentScheduleExceptionTimeRange(
      AppointmentScheduleException appointmentScheduleException,
      LocalTime startTime,
      LocalTime endTime) {
    if (appointmentScheduleException == null) {
      throw new IllegalArgumentException("appointmentScheduleException must not be null");
    }
    if (startTime == null) {
      throw new IllegalArgumentException("startTime must not be null");
    }
    if (endTime == null) {
      throw new IllegalArgumentException("endTime must not be null");
    }
    if (!startTime.isBefore(endTime)) {
      throw new IllegalArgumentException("startTime must be before endTime");
    }
    this.appointmentScheduleException = appointmentScheduleException;
    this.startTime = startTime;
    this.endTime = endTime;
  }
}
