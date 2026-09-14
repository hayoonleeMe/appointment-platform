package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Entity
@Getter
public class AppointmentScheduleException {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private AppointmentSchedule appointmentSchedule;

  @Column(name = "exception_date", nullable = false)
  private LocalDate date;

  @Enumerated(EnumType.STRING)
  @Column(name = "exception_type", nullable = false)
  private ScheduleExceptionType type;

  @OneToMany(
      mappedBy = "appointmentScheduleException",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<AppointmentScheduleExceptionTimeRange> exceptionTimeRanges = new ArrayList<>();

  protected AppointmentScheduleException() {}

  public AppointmentScheduleException(
      AppointmentSchedule appointmentSchedule, LocalDate date, ScheduleExceptionType type) {
    if (appointmentSchedule == null) {
      throw new IllegalArgumentException("appointmentSchedule must not be null");
    }
    if (date == null) {
      throw new IllegalArgumentException("date must not be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }
    this.appointmentSchedule = appointmentSchedule;
    this.date = date;
    this.type = type;
  }

  public void addExceptionTimeRange(LocalTime startTime, LocalTime endTime) {
    if (type == ScheduleExceptionType.CLOSED) {
      throw new IllegalArgumentException("timeRanges must be empty for CLOSED");
    }
    for (AppointmentScheduleExceptionTimeRange timeRange : exceptionTimeRanges) {
      if (timeRange.getStartTime().isBefore(endTime) && timeRange.getEndTime().isAfter(startTime)) {
        throw new IllegalArgumentException("exception time ranges must not overlap");
      }
    }
    exceptionTimeRanges.add(new AppointmentScheduleExceptionTimeRange(this, startTime, endTime));
  }

  void validate() {
    if (type == ScheduleExceptionType.CUSTOM_TIME && exceptionTimeRanges.isEmpty()) {
      throw new IllegalArgumentException("timeRanges must not be empty for CUSTOM_TIME");
    }
  }
}
