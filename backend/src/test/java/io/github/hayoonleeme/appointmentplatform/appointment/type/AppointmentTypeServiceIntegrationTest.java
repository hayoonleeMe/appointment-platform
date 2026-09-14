package io.github.hayoonleeme.appointmentplatform.appointment.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hayoonleeme.appointmentplatform.TestContainerConfig;
import io.github.hayoonleeme.appointmentplatform.appointment.schedule.AppointmentScheduleParams;
import io.github.hayoonleeme.appointmentplatform.appointment.schedule.AppointmentScheduleRepository;
import io.github.hayoonleeme.appointmentplatform.appointment.schedule.ScheduleExceptionType;
import io.github.hayoonleeme.appointmentplatform.operator.Operator;
import io.github.hayoonleeme.appointmentplatform.operator.OperatorRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestContainerConfig.class)
@SpringBootTest
class AppointmentTypeServiceIntegrationTest {
  @Autowired private OperatorRepository operatorRepository;
  @Autowired private AppointmentTypeRepository appointmentTypeRepository;
  @Autowired private AppointmentScheduleRepository appointmentScheduleRepository;
  @Autowired private AppointmentTypeService appointmentTypeService;

  @AfterEach
  void cleanDatabase() {
    appointmentScheduleRepository.deleteAll();
    appointmentTypeRepository.deleteAll();
    operatorRepository.deleteAll();
  }

  @Test
  void rollsBackRegistrationWhenScheduleValidationFails() {
    Operator operator = operatorRepository.save(new Operator());

    assertThatThrownBy(
            () ->
                appointmentTypeService.register(
                    operator.getId(),
                    "상담",
                    AppointmentMethod.ONE_ON_ONE,
                    (short) 50,
                    (short) 10,
                    (short) 30,
                    true,
                    invalidScheduleParams()))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(appointmentTypeRepository.count()).isZero();
    assertThat(appointmentScheduleRepository.count()).isZero();
  }

  private static AppointmentScheduleParams invalidScheduleParams() {
    return new AppointmentScheduleParams(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        List.of(
            new AppointmentScheduleParams.WeeklyTimeRange(
                DayOfWeek.MONDAY,
                new AppointmentScheduleParams.TimeRange(LocalTime.of(9, 0), LocalTime.of(18, 0)))),
        List.of(
            new AppointmentScheduleParams.ScheduleException(
                LocalDate.of(2026, 1, 2), ScheduleExceptionType.CUSTOM_TIME, List.of())));
  }
}
