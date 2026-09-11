package io.github.hayoonleeme.appointmentplatform.appointment.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.hayoonleeme.appointmentplatform.TestContainerConfig;
import io.github.hayoonleeme.appointmentplatform.appointment.schedule.AppointmentScheduleParams;
import io.github.hayoonleeme.appointmentplatform.appointment.schedule.AppointmentScheduleRepository;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@Import(TestContainerConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentTypeUpdateApiIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private OperatorRepository operatorRepository;
  @Autowired private AppointmentTypeRepository appointmentTypeRepository;
  @Autowired private AppointmentScheduleRepository appointmentScheduleRepository;
  @Autowired private AppointmentTypeService appointmentTypeService;
  @Autowired private JsonMapper jsonMapper;

  @AfterEach
  void cleanDatabase() {
    appointmentScheduleRepository.deleteAll();
    appointmentTypeRepository.deleteAll();
    operatorRepository.deleteAll();
  }

  @Test
  void updateActiveAppointmentTypeSuccessfully() throws Exception {
    Operator operator = operatorRepository.save(new Operator());
    AppointmentType appointmentType = registerAppointmentType(operator);

    mockMvc
        .perform(
            patch(
                    "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}",
                    operator.getId(),
                    appointmentType.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    jsonMapper.writeValueAsString(
                        new AppointmentTypeController.UpdateRequest(false))))
        .andExpect(status().isNoContent());

    AppointmentType updated =
        appointmentTypeRepository
            .findByIdAndOperatorId(appointmentType.getId(), operator.getId())
            .orElseThrow();
    assertThat(updated.isActive()).isFalse();
  }

  @Test
  void rejectsUpdateForAnotherOperatorsAppointmentType() throws Exception {
    Operator operator1 = operatorRepository.save(new Operator());
    Operator operator2 = operatorRepository.save(new Operator());
    AppointmentType appointmentType = registerAppointmentType(operator1);

    mockMvc
        .perform(
            patch(
                    "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}",
                    operator2.getId(),
                    appointmentType.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    jsonMapper.writeValueAsString(
                        new AppointmentTypeController.UpdateRequest(false))))
        .andExpect(status().isNotFound());
  }

  private AppointmentType registerAppointmentType(Operator operator) {
    return appointmentTypeService.register(
        operator.getId(),
        "상담",
        AppointmentMethod.ONE_ON_ONE,
        (short) 50,
        (short) 10,
        (short) 30,
        true,
        validScheduleParams());
  }

  private static AppointmentScheduleParams validScheduleParams() {
    return new AppointmentScheduleParams(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        List.of(
            new AppointmentScheduleParams.WeeklyTimeRange(
                DayOfWeek.MONDAY,
                new AppointmentScheduleParams.TimeRange(LocalTime.of(9, 0), LocalTime.of(18, 0)))),
        List.of());
  }
}
