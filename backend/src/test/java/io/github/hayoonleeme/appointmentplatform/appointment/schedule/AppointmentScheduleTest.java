package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hayoonleeme.appointmentplatform.appointment.type.AppointmentMethod;
import io.github.hayoonleeme.appointmentplatform.appointment.type.AppointmentType;
import io.github.hayoonleeme.appointmentplatform.operator.Operator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

public class AppointmentScheduleTest {
  @Test
  void rejectsReversedSchedulePeriod() {
    assertThatThrownBy(
            () ->
                new AppointmentSchedule(
                    validAppointmentType(), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsScheduleWithoutWeeklyTimeRanges() {
    assertThatThrownBy(
            () ->
                new AppointmentSchedule(
                        validAppointmentType(),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31))
                    .validate())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsOverlappingWeeklyTimeRangesOnSameDay() {
    assertThatThrownBy(
            () -> {
              AppointmentSchedule schedule =
                  new AppointmentSchedule(
                      validAppointmentType(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
              schedule.addWeeklyTimeRange(
                  DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(14, 0));
              schedule.addWeeklyTimeRange(
                  DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0));
            })
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void allowsAdjacentWeeklyTimeRangesOnSameDay() {
    assertThatCode(
            () -> {
              AppointmentSchedule schedule =
                  new AppointmentSchedule(
                      validAppointmentType(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
              schedule.addWeeklyTimeRange(
                  DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
              schedule.addWeeklyTimeRange(
                  DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));
            })
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsExceptionDateOutsideSchedulePeriod() {
    assertThatThrownBy(
            () -> {
              AppointmentSchedule schedule =
                  new AppointmentSchedule(
                      validAppointmentType(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1));
              schedule.addScheduleException(
                  LocalDate.of(2026, 10, 1), ScheduleExceptionType.CLOSED);
            })
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsDuplicateExceptionDates() {
    assertThatThrownBy(
            () -> {
              AppointmentSchedule schedule =
                  new AppointmentSchedule(
                      validAppointmentType(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1));
              schedule.addScheduleException(
                  LocalDate.of(2026, 8, 10), ScheduleExceptionType.CLOSED);
              schedule.addScheduleException(
                  LocalDate.of(2026, 8, 10), ScheduleExceptionType.CLOSED);
            })
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsTimeRangeForClosedException() {
    assertThatThrownBy(
            () -> {
              AppointmentSchedule schedule =
                  new AppointmentSchedule(
                      validAppointmentType(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1));
              schedule
                  .addScheduleException(LocalDate.of(2026, 8, 10), ScheduleExceptionType.CLOSED)
                  .addExceptionTimeRange(LocalTime.of(10, 0), LocalTime.of(11, 0));
            })
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsOverlappingTimeRangesForCustomTimeException() {
    assertThatThrownBy(
            () -> {
              AppointmentSchedule schedule =
                  new AppointmentSchedule(
                      validAppointmentType(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1));
              AppointmentScheduleException scheduleException =
                  schedule.addScheduleException(
                      LocalDate.of(2026, 8, 10), ScheduleExceptionType.CUSTOM_TIME);
              scheduleException.addExceptionTimeRange(LocalTime.of(12, 0), LocalTime.of(14, 0));
              scheduleException.addExceptionTimeRange(LocalTime.of(13, 0), LocalTime.of(15, 0));
            })
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsCustomTimeExceptionWithoutTimeRanges() {
    assertThatThrownBy(
            () -> {
              AppointmentSchedule schedule =
                  new AppointmentSchedule(
                      validAppointmentType(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1));
              schedule.addWeeklyTimeRange(
                  DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));
              schedule.addScheduleException(
                  LocalDate.of(2026, 8, 10), ScheduleExceptionType.CUSTOM_TIME);
              schedule.validate();
            })
        .isInstanceOf(IllegalArgumentException.class);
  }

  AppointmentType validAppointmentType() {
    return new AppointmentType(
        "상담",
        AppointmentMethod.ONE_ON_ONE,
        (short) 50,
        (short) 10,
        (short) 30,
        true,
        new Operator());
  }
}
