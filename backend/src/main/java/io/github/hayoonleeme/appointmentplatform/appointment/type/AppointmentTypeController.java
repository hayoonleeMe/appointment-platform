package io.github.hayoonleeme.appointmentplatform.appointment.type;

import io.github.hayoonleeme.appointmentplatform.appointment.schedule.AppointmentScheduleParams;
import io.github.hayoonleeme.appointmentplatform.appointment.schedule.ScheduleExceptionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operators/{operatorId}/appointment-types")
@RequiredArgsConstructor
public class AppointmentTypeController {
  private final AppointmentTypeService appointmentTypeService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(
      @PathVariable Long operatorId, @RequestBody @Valid RegisterRequest request) {
    AppointmentType appointmentType =
        appointmentTypeService.register(
            operatorId,
            request.name,
            request.appointmentMethod,
            request.durationMinutes,
            request.preparationMinutes,
            request.startIntervalMinutes,
            request.active,
            request.schedule.toScheduleParams());
    return new RegisterResponse(appointmentType.getId());
  }

  @PatchMapping("/{appointmentTypeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(
      @PathVariable Long operatorId,
      @PathVariable Long appointmentTypeId,
      @RequestBody @Valid UpdateRequest request) {
    appointmentTypeService.updateActive(operatorId, appointmentTypeId, request.active);
  }

  public record RegisterRequest(
      @NotBlank(message = "name must not be blank")
          @Length(max = 50, message = "name must not exceed 50 characters")
          String name,
      @NotNull(message = "appointmentMethod must not be null") AppointmentMethod appointmentMethod,
      @NotNull(message = "durationMinutes must not be null")
          @Positive(message = "durationMinutes must be positive")
          Short durationMinutes,
      @NotNull(message = "preparationMinutes must not be null")
          @PositiveOrZero(message = "preparationMinutes must be zero or positive")
          Short preparationMinutes,
      @Positive(message = "startIntervalMinutes must be positive") Short startIntervalMinutes,
      @NotNull(message = "active must not be null") Boolean active,
      @Valid @NotNull(message = "schedule must not be null") ScheduleRequest schedule) {

    @AssertTrue(
        message = "startIntervalMinutes must be present for ONE_ON_ONE and absent for GROUP")
    public boolean isStartIntervalMinutesValidForAppointmentMethod() {
      if (appointmentMethod == null) {
        return true;
      }
      if (appointmentMethod == AppointmentMethod.ONE_ON_ONE && startIntervalMinutes == null) {
        return false;
      }
      if (appointmentMethod == AppointmentMethod.GROUP && startIntervalMinutes != null) {
        return false;
      }
      return true;
    }
  }

  public record ScheduleRequest(
      @NotNull(message = "startDate must not be null") LocalDate startDate,
      @NotNull(message = "endDate must not be null") LocalDate endDate,
      @NotEmpty(message = "weeklyTimeRanges must not be empty")
          List<
                  @NotNull(message = "weeklyTimeRanges must not contain null") @Valid
                  WeeklyTimeRangeRequest>
              weeklyTimeRanges,
      @NotNull(message = "exceptions must not be null")
          List<
                  @NotNull(message = "exceptions must not contain null") @Valid
                  ScheduleExceptionRequest>
              exceptions) {
    @AssertTrue(message = "startDate must not be after endDate")
    public boolean isDateRangeValid() {
      if (startDate == null || endDate == null) {
        return true;
      }
      return !startDate.isAfter(endDate);
    }

    @AssertTrue(message = "weeklyTimeRanges must not overlap on the same dayOfWeek")
    public boolean isWeeklyTimeRangesNonOverlapping() {
      if (weeklyTimeRanges == null
          || weeklyTimeRanges.stream()
              .anyMatch(
                  timeRange ->
                      timeRange == null
                          || timeRange.dayOfWeek == null
                          || timeRange.startTime == null
                          || timeRange.endTime == null)) {
        return true;
      }
      for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
        List<WeeklyTimeRangeRequest> filtered =
            weeklyTimeRanges.stream()
                .filter(weeklyTimeRange -> weeklyTimeRange.dayOfWeek == dayOfWeek)
                .sorted(
                    (weeklyTimeRangeA, weeklyTimeRangeB) ->
                        weeklyTimeRangeA.startTime.compareTo(weeklyTimeRangeB.startTime))
                .toList();
        for (int i = 0; i < filtered.size() - 1; i++) {
          if (filtered.get(i).endTime.isAfter(filtered.get(i + 1).startTime)) {
            return false;
          }
        }
      }
      return true;
    }

    public record WeeklyTimeRangeRequest(
        @NotNull(message = "dayOfWeek must not be null") DayOfWeek dayOfWeek,
        @NotNull(message = "startTime must not be null") LocalTime startTime,
        @NotNull(message = "endTime must not be null") LocalTime endTime) {
      @AssertTrue(message = "startTime must be before endTime")
      public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
          return true;
        }
        return startTime.isBefore(endTime);
      }
    }

    public record ScheduleExceptionRequest(
        @NotNull(message = "date must not be null") LocalDate date,
        @NotNull(message = "type must not be null") ScheduleExceptionType type,
        @NotNull(message = "timeRanges must not be null")
            List<@NotNull(message = "timeRanges must not contain null") @Valid TimeRangeRequest>
                timeRanges) {
      @AssertTrue(message = "timeRanges must be empty for CLOSED and non-empty for CUSTOM_TIME")
      public boolean isTimeRangesValidForExceptionType() {
        if (type == null || timeRanges == null) {
          return true;
        }
        if (type == ScheduleExceptionType.CLOSED && !timeRanges.isEmpty()) {
          return false;
        }
        if (type == ScheduleExceptionType.CUSTOM_TIME && timeRanges.isEmpty()) {
          return false;
        }
        return true;
      }

      @AssertTrue(message = "timeRanges must not overlap")
      public boolean isTimeRangesNonOverlapping() {
        if (timeRanges == null
            || timeRanges.stream()
                .anyMatch(
                    timeRange ->
                        timeRange == null
                            || timeRange.startTime == null
                            || timeRange.endTime == null)) {
          return true;
        }
        List<TimeRangeRequest> sortedTimeRanges =
            timeRanges.stream()
                .sorted(
                    (timeRangeA, timeRangeB) ->
                        timeRangeA.startTime.compareTo(timeRangeB.startTime))
                .toList();
        for (int i = 0; i < sortedTimeRanges.size() - 1; i++) {
          if (sortedTimeRanges.get(i).endTime.isAfter(sortedTimeRanges.get(i + 1).startTime)) {
            return false;
          }
        }
        return true;
      }
    }

    @AssertTrue(message = "exception dates must be within the schedule period")
    public boolean isExceptionDatesWithinSchedulePeriod() {
      if (startDate == null
          || endDate == null
          || exceptions == null
          || exceptions.stream()
              .anyMatch(exception -> exception == null || exception.date == null)) {
        return true;
      }
      for (ScheduleExceptionRequest exceptionRequest : exceptions) {
        if (exceptionRequest.date.isBefore(startDate) || exceptionRequest.date.isAfter(endDate)) {
          return false;
        }
      }
      return true;
    }

    @AssertTrue(message = "exception dates must be unique")
    public boolean isExceptionDatesUnique() {
      if (exceptions == null
          || exceptions.stream()
              .anyMatch(exception -> exception == null || exception.date == null)) {
        return true;
      }
      return exceptions.stream()
              .map(ScheduleExceptionRequest::date)
              .collect(Collectors.toSet())
              .size()
          == exceptions.size();
    }

    public record TimeRangeRequest(
        @NotNull(message = "startTime must not be null") LocalTime startTime,
        @NotNull(message = "endTime must not be null") LocalTime endTime) {
      @AssertTrue(message = "startTime must be before endTime")
      public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
          return true;
        }
        return startTime.isBefore(endTime);
      }
    }

    public AppointmentScheduleParams toScheduleParams() {
      return new AppointmentScheduleParams(
          startDate,
          endDate,
          weeklyTimeRanges.stream()
              .map(
                  range ->
                      new AppointmentScheduleParams.WeeklyTimeRange(
                          range.dayOfWeek,
                          new AppointmentScheduleParams.TimeRange(range.startTime, range.endTime)))
              .toList(),
          exceptions.stream()
              .map(
                  exception ->
                      new AppointmentScheduleParams.ScheduleException(
                          exception.date,
                          exception.type,
                          exception.timeRanges.stream()
                              .map(
                                  timeRange ->
                                      new AppointmentScheduleParams.TimeRange(
                                          timeRange.startTime, timeRange.endTime))
                              .toList()))
              .toList());
    }
  }

  public record RegisterResponse(Long id) {}

  public record UpdateRequest(@NotNull(message = "active must not be null") Boolean active) {}
}
