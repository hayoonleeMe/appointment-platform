package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AppointmentScheduleDetails(
    LocalDate startDate,
    LocalDate endDate,
    List<WeeklyTimeRange> weeklyTimeRanges,
    List<ScheduleException> exceptions) {
  public record WeeklyTimeRange(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {}

  public record ScheduleException(
      LocalDate date, ScheduleExceptionType type, List<TimeRange> timeRanges) {
    public record TimeRange(LocalTime startTime, LocalTime endTime) {}
  }

  public static AppointmentScheduleDetails from(AppointmentSchedule schedule) {
    return new AppointmentScheduleDetails(
        schedule.getStartDate(),
        schedule.getEndDate(),
        schedule.getWeeklyTimeRanges().stream()
            .map(
                weeklyTimeRange ->
                    new WeeklyTimeRange(
                        weeklyTimeRange.getDayOfWeek(),
                        weeklyTimeRange.getStartTime(),
                        weeklyTimeRange.getEndTime()))
            .toList(),
        schedule.getScheduleExceptions().stream()
            .map(
                scheduleException ->
                    new ScheduleException(
                        scheduleException.getDate(),
                        scheduleException.getType(),
                        scheduleException.getExceptionTimeRanges().stream()
                            .map(
                                timeRange ->
                                    new ScheduleException.TimeRange(
                                        timeRange.getStartTime(), timeRange.getEndTime()))
                            .toList()))
            .toList());
  }
}
