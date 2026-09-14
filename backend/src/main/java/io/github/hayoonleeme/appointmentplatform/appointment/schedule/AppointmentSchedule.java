package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import io.github.hayoonleeme.appointmentplatform.appointment.type.AppointmentType;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Entity
@Getter
public class AppointmentSchedule {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, unique = true)
  private AppointmentType appointmentType;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  @OneToMany(mappedBy = "appointmentSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AppointmentScheduleWeeklyTimeRange> weeklyTimeRanges = new ArrayList<>();

  @OneToMany(mappedBy = "appointmentSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AppointmentScheduleException> scheduleExceptions = new ArrayList<>();

  protected AppointmentSchedule() {}

  public AppointmentSchedule(
      AppointmentType appointmentType, LocalDate startDate, LocalDate endDate) {
    if (appointmentType == null) {
      throw new IllegalArgumentException("appointmentType must not be null");
    }
    if (startDate == null) {
      throw new IllegalArgumentException("startDate must not be null");
    }
    if (endDate == null) {
      throw new IllegalArgumentException("endDate must not be null");
    }
    if (startDate.isAfter(endDate)) {
      throw new IllegalArgumentException("startDate must not be after endDate");
    }
    this.appointmentType = appointmentType;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public void addWeeklyTimeRange(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    for (AppointmentScheduleWeeklyTimeRange timeRange : weeklyTimeRanges) {
      if (timeRange.getDayOfWeek().equals(dayOfWeek)
          && timeRange.getStartTime().isBefore(endTime)
          && timeRange.getEndTime().isAfter(startTime)) {
        throw new IllegalArgumentException(
            "weekly time ranges must not overlap on the same dayOfWeek");
      }
    }
    weeklyTimeRanges.add(
        new AppointmentScheduleWeeklyTimeRange(this, dayOfWeek, startTime, endTime));
  }

  public AppointmentScheduleException addScheduleException(
      LocalDate date, ScheduleExceptionType type) {
    AppointmentScheduleException scheduleException =
        new AppointmentScheduleException(this, date, type);
    if (scheduleException.getDate().isBefore(startDate)
        || scheduleException.getDate().isAfter(endDate)) {
      throw new IllegalArgumentException("exception date must be within the schedule period");
    }
    if (scheduleExceptions.stream()
        .anyMatch(exception -> exception.getDate().isEqual(scheduleException.getDate()))) {
      throw new IllegalArgumentException("exception date must be unique");
    }
    scheduleExceptions.add(scheduleException);
    return scheduleException;
  }

  public void replace(AppointmentScheduleParams params) {
    if (params.startDate() == null) {
      throw new IllegalArgumentException("startDate must not be null");
    }
    if (params.endDate() == null) {
      throw new IllegalArgumentException("endDate must not be null");
    }
    if (params.startDate().isAfter(params.endDate())) {
      throw new IllegalArgumentException("startDate must not be after endDate");
    }
    this.startDate = params.startDate();
    this.endDate = params.endDate();

    this.weeklyTimeRanges.clear();
    this.scheduleExceptions.clear();

    params
        .weeklyTimeRanges()
        .forEach(
            weeklyTimeRange ->
                addWeeklyTimeRange(
                    weeklyTimeRange.dayOfWeek(),
                    weeklyTimeRange.timeRange().startTime(),
                    weeklyTimeRange.timeRange().endTime()));

    for (AppointmentScheduleParams.ScheduleException paramScheduleException :
        params.scheduleExceptions()) {
      AppointmentScheduleException scheduleException =
          addScheduleException(paramScheduleException.date(), paramScheduleException.type());
      paramScheduleException
          .timeRanges()
          .forEach(
              timeRange ->
                  scheduleException.addExceptionTimeRange(
                      timeRange.startTime(), timeRange.endTime()));
    }

    validate();
  }

  public void validate() {
    if (weeklyTimeRanges.isEmpty()) {
      throw new IllegalArgumentException("weekly time ranges must not be empty");
    }
    scheduleExceptions.forEach(AppointmentScheduleException::validate);
  }
}
