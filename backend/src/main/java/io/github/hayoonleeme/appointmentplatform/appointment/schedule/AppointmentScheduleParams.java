package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AppointmentScheduleParams(
    LocalDate startDate,
    LocalDate endDate,
    List<WeeklyTimeRange> weeklyTimeRanges,
    List<ScheduleException> scheduleExceptions) {
  public record WeeklyTimeRange(DayOfWeek dayOfWeek, TimeRange timeRange) {}

  public record ScheduleException(
      LocalDate date, ScheduleExceptionType type, List<TimeRange> timeRanges) {}

  public record TimeRange(LocalTime startTime, LocalTime endTime) {}
}
