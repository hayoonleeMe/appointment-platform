package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;

@Entity
public class AppointmentScheduleWeeklyTimeRange {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private AppointmentSchedule appointmentSchedule;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Getter
  private DayOfWeek dayOfWeek;

  @Column(nullable = false)
  @Getter
  private LocalTime startTime;

  @Column(nullable = false)
  @Getter
  private LocalTime endTime;

  protected AppointmentScheduleWeeklyTimeRange() {}

  public AppointmentScheduleWeeklyTimeRange(
      AppointmentSchedule appointmentSchedule,
      DayOfWeek dayOfWeek,
      LocalTime startTime,
      LocalTime endTime) {
    if (appointmentSchedule == null) {
      throw new IllegalArgumentException("appointmentSchedule must not be null");
    }
    if (dayOfWeek == null) {
      throw new IllegalArgumentException("dayOfWeek must not be null");
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
    this.appointmentSchedule = appointmentSchedule;
    this.dayOfWeek = dayOfWeek;
    this.startTime = startTime;
    this.endTime = endTime;
  }
}
