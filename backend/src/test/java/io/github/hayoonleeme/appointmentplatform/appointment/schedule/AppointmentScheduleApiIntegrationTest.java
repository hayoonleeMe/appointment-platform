package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.hayoonleeme.appointmentplatform.TestContainerConfig;
import io.github.hayoonleeme.appointmentplatform.appointment.type.*;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

@Import(TestContainerConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AppointmentScheduleApiIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private OperatorRepository operatorRepository;
  @Autowired private AppointmentTypeRepository appointmentTypeRepository;
  @Autowired private AppointmentScheduleRepository appointmentScheduleRepository;
  @Autowired private AppointmentTypeService appointmentTypeService;
  @Autowired private JsonMapper jsonMapper;
  @Autowired private AppointmentScheduleService appointmentScheduleService;

  @AfterEach
  void afterEach() {
    appointmentScheduleRepository.deleteAll();
    appointmentTypeRepository.deleteAll();
    operatorRepository.deleteAll();
  }

  @Test
  void getsScheduleWithoutExceptions() throws Exception {
    Operator operator = operatorRepository.save(new Operator());
    AppointmentType appointmentType =
        registerAppointmentType(operator, scheduleParamsWithoutExceptions());

    mockMvc
        .perform(
            get(
                    "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}/schedule",
                    operator.getId(),
                    appointmentType.getId())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.startDate").value("2026-01-01"))
        .andExpect(jsonPath("$.endDate").value("2026-12-31"))
        .andExpect(jsonPath("$.weeklyTimeRanges").isArray())
        .andExpect(jsonPath("$.weeklyTimeRanges.length()").value(1))
        .andExpect(jsonPath("$.weeklyTimeRanges[0].dayOfWeek").value("MONDAY"))
        .andExpect(jsonPath("$.exceptions").isEmpty());
  }

  @Test
  void getsScheduleWithExceptions() throws Exception {
    Operator operator = operatorRepository.save(new Operator());
    AppointmentType appointmentType =
        registerAppointmentType(operator, scheduleParamsWithExceptions());

    MvcResult result =
        mockMvc
            .perform(
                get(
                        "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}/schedule",
                        operator.getId(),
                        appointmentType.getId())
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn();

    AppointmentScheduleDetails actual =
        jsonMapper.readValue(
            result.getResponse().getContentAsString(), AppointmentScheduleDetails.class);

    assertThat(actual)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(expectedScheduleDetailsWithExceptions());
  }

  @Test
  void rejectsGetScheduleForNonexistentAppointmentType() throws Exception {
    Operator operator = operatorRepository.save(new Operator());

    mockMvc
        .perform(
            get(
                    "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}/schedule",
                    operator.getId(),
                    1)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsGetScheduleForAppointmentTypeOwnedByAnotherOperator() throws Exception {
    Operator owner = operatorRepository.save(new Operator());
    Operator otherOperator = operatorRepository.save(new Operator());
    AppointmentType appointmentType =
        registerAppointmentType(owner, scheduleParamsWithoutExceptions());

    mockMvc
        .perform(
            get(
                    "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}/schedule",
                    otherOperator.getId(),
                    appointmentType.getId())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void replacesScheduleSuccessfully() throws Exception {
    Operator operator = operatorRepository.save(new Operator());
    AppointmentType appointmentType =
        registerAppointmentType(operator, scheduleParamsBeforeReplacement());

    mockMvc
        .perform(
            put(
                    "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}/schedule",
                    operator.getId(),
                    appointmentType.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(scheduleReplacementRequest())))
        .andExpect(status().isNoContent());

    AppointmentScheduleDetails actual =
        appointmentScheduleService.getSchedule(operator.getId(), appointmentType.getId());
    assertThat(actual)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(expectedReplacedScheduleDetails());
  }

  @Test
  void doesNotChangeScheduleWhenReplacementRequestIsInvalid() throws Exception {
    Operator operator = operatorRepository.save(new Operator());
    AppointmentType appointmentType =
        registerAppointmentType(operator, scheduleParamsWithExceptions());

    mockMvc
        .perform(
            put(
                    "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}/schedule",
                    operator.getId(),
                    appointmentType.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(invalidScheduleReplacementRequest())))
        .andExpect(status().isBadRequest());

    AppointmentScheduleDetails actual =
        appointmentScheduleService.getSchedule(operator.getId(), appointmentType.getId());
    assertThat(actual)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(expectedScheduleDetailsWithExceptions());
  }

  @Test
  void rejectsReplaceScheduleForNonexistentAppointmentType() throws Exception {
    Operator operator = operatorRepository.save(new Operator());

    mockMvc
        .perform(
            put(
                    "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}/schedule",
                    operator.getId(),
                    Long.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(scheduleReplacementRequest())))
        .andExpect(status().isNotFound());
  }

  @Test
  void doesNotChangeScheduleWhenAppointmentTypeBelongsToAnotherOperator() throws Exception {
    Operator owner = operatorRepository.save(new Operator());
    Operator otherOperator = operatorRepository.save(new Operator());
    AppointmentType appointmentType =
        registerAppointmentType(owner, scheduleParamsWithExceptions());

    mockMvc
        .perform(
            put(
                    "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}/schedule",
                    otherOperator.getId(),
                    appointmentType.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(scheduleReplacementRequest())))
        .andExpect(status().isNotFound());

    AppointmentScheduleDetails actual =
        appointmentScheduleService.getSchedule(owner.getId(), appointmentType.getId());
    assertThat(actual)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(expectedScheduleDetailsWithExceptions());
  }

  private AppointmentType registerAppointmentType(
      Operator operator, AppointmentScheduleParams scheduleParams) {
    return appointmentTypeService.register(
        operator.getId(),
        "상담",
        AppointmentMethod.ONE_ON_ONE,
        (short) 50,
        (short) 10,
        (short) 30,
        true,
        scheduleParams);
  }

  private static AppointmentScheduleParams scheduleParamsWithoutExceptions() {
    return new AppointmentScheduleParams(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        List.of(
            new AppointmentScheduleParams.WeeklyTimeRange(
                DayOfWeek.MONDAY,
                new AppointmentScheduleParams.TimeRange(LocalTime.of(9, 0), LocalTime.of(18, 0)))),
        List.of());
  }

  private static AppointmentScheduleParams scheduleParamsWithExceptions() {
    return new AppointmentScheduleParams(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        List.of(
            new AppointmentScheduleParams.WeeklyTimeRange(
                DayOfWeek.MONDAY,
                new AppointmentScheduleParams.TimeRange(LocalTime.of(9, 0), LocalTime.of(18, 0)))),
        List.of(
            new AppointmentScheduleParams.ScheduleException(
                LocalDate.of(2026, 6, 1), ScheduleExceptionType.CLOSED, List.of()),
            new AppointmentScheduleParams.ScheduleException(
                LocalDate.of(2026, 7, 1),
                ScheduleExceptionType.CUSTOM_TIME,
                List.of(
                    new AppointmentScheduleParams.TimeRange(
                        LocalTime.of(12, 0), LocalTime.of(16, 0))))));
  }

  private static AppointmentScheduleParams scheduleParamsBeforeReplacement() {
    return new AppointmentScheduleParams(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        List.of(
            new AppointmentScheduleParams.WeeklyTimeRange(
                DayOfWeek.MONDAY,
                new AppointmentScheduleParams.TimeRange(LocalTime.of(9, 0), LocalTime.of(18, 0))),
            new AppointmentScheduleParams.WeeklyTimeRange(
                DayOfWeek.TUESDAY,
                new AppointmentScheduleParams.TimeRange(LocalTime.of(10, 0), LocalTime.of(17, 0)))),
        List.of(
            new AppointmentScheduleParams.ScheduleException(
                LocalDate.of(2026, 5, 1), ScheduleExceptionType.CLOSED, List.of())));
  }

  private static AppointmentScheduleDetails expectedScheduleDetailsWithExceptions() {
    return new AppointmentScheduleDetails(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        List.of(
            new AppointmentScheduleDetails.WeeklyTimeRange(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))),
        List.of(
            new AppointmentScheduleDetails.ScheduleException(
                LocalDate.of(2026, 6, 1), ScheduleExceptionType.CLOSED, List.of()),
            new AppointmentScheduleDetails.ScheduleException(
                LocalDate.of(2026, 7, 1),
                ScheduleExceptionType.CUSTOM_TIME,
                List.of(
                    new AppointmentScheduleDetails.ScheduleException.TimeRange(
                        LocalTime.of(12, 0), LocalTime.of(16, 0))))));
  }

  private static AppointmentScheduleDetails expectedReplacedScheduleDetails() {
    return new AppointmentScheduleDetails(
        LocalDate.of(2026, 2, 1),
        LocalDate.of(2026, 11, 30),
        List.of(
            new AppointmentScheduleDetails.WeeklyTimeRange(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))),
        List.of(
            new AppointmentScheduleDetails.ScheduleException(
                LocalDate.of(2026, 6, 1), ScheduleExceptionType.CLOSED, List.of()),
            new AppointmentScheduleDetails.ScheduleException(
                LocalDate.of(2026, 7, 1),
                ScheduleExceptionType.CUSTOM_TIME,
                List.of(
                    new AppointmentScheduleDetails.ScheduleException.TimeRange(
                        LocalTime.of(12, 0), LocalTime.of(16, 0))))));
  }

  private static AppointmentTypeController.ScheduleRequest scheduleReplacementRequest() {
    return new AppointmentTypeController.ScheduleRequest(
        LocalDate.of(2026, 2, 1),
        LocalDate.of(2026, 11, 30),
        List.of(
            new AppointmentTypeController.ScheduleRequest.WeeklyTimeRangeRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))),
        List.of(
            new AppointmentTypeController.ScheduleRequest.ScheduleExceptionRequest(
                LocalDate.of(2026, 6, 1), ScheduleExceptionType.CLOSED, List.of()),
            new AppointmentTypeController.ScheduleRequest.ScheduleExceptionRequest(
                LocalDate.of(2026, 7, 1),
                ScheduleExceptionType.CUSTOM_TIME,
                List.of(
                    new AppointmentTypeController.ScheduleRequest.TimeRangeRequest(
                        LocalTime.of(12, 0), LocalTime.of(16, 0))))));
  }

  private static AppointmentTypeController.ScheduleRequest invalidScheduleReplacementRequest() {
    return new AppointmentTypeController.ScheduleRequest(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        List.of(
            new AppointmentTypeController.ScheduleRequest.WeeklyTimeRangeRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 0))),
        List.of());
  }
}
