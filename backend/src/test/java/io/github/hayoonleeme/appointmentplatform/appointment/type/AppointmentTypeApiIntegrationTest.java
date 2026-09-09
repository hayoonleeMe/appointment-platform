package io.github.hayoonleeme.appointmentplatform.appointment.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.hayoonleeme.appointmentplatform.TestContainerConfig;
import io.github.hayoonleeme.appointmentplatform.appointment.schedule.AppointmentScheduleParams;
import io.github.hayoonleeme.appointmentplatform.appointment.schedule.AppointmentScheduleRepository;
import io.github.hayoonleeme.appointmentplatform.appointment.schedule.ScheduleExceptionType;
import io.github.hayoonleeme.appointmentplatform.operator.Operator;
import io.github.hayoonleeme.appointmentplatform.operator.OperatorRepository;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
public class AppointmentTypeApiIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private OperatorRepository operatorRepository;
  @Autowired private AppointmentTypeRepository appointmentTypeRepository;
  @Autowired private AppointmentScheduleRepository appointmentScheduleRepository;
  @Autowired private AppointmentTypeService appointmentTypeService;
  @Autowired private EntityManager entityManager;
  @Autowired private JsonMapper jsonMapper;

  @AfterEach
  void clean() {
    appointmentScheduleRepository.deleteAll();
    appointmentTypeRepository.deleteAll();
    operatorRepository.deleteAll();
  }

  @Test
  void registerOneOnOneAppointmentTypeSuccessfully() throws Exception {
    Operator operator = operatorRepository.save(new Operator());
    Long operatorId = operator.getId();

    AppointmentTypeController.RegisterRequest request =
        new AppointmentTypeController.RegisterRequest(
            "상담",
            AppointmentMethod.ONE_ON_ONE,
            (short) 50,
            (short) 10,
            (short) 30,
            true,
            validSchedule());

    MvcResult result =
        mockMvc
            .perform(
                post("/api/operators/{operatorId}/appointment-types", operatorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andReturn();

    long appointmentTypeId =
        jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").longValue();
    assertThat(appointmentTypeRepository.findByIdAndOperatorId(appointmentTypeId, operatorId))
        .isPresent();
    assertThat(appointmentScheduleRepository.count()).isOne();
  }

  @Test
  void registerGroupAppointmentTypeSuccessfully() throws Exception {
    Operator operator = operatorRepository.save(new Operator());
    Long operatorId = operator.getId();

    AppointmentTypeController.RegisterRequest request =
        new AppointmentTypeController.RegisterRequest(
            "상담", AppointmentMethod.GROUP, (short) 50, (short) 10, null, true, validSchedule());

    MvcResult result =
        mockMvc
            .perform(
                post("/api/operators/{operatorId}/appointment-types", operatorId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andReturn();

    long appointmentTypeId =
        jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").longValue();
    assertThat(appointmentTypeRepository.findByIdAndOperatorId(appointmentTypeId, operatorId))
        .isPresent();
    assertThat(appointmentScheduleRepository.count()).isOne();
  }

  @Test
  void registerAppointmentTypeWithCompleteScheduleSuccessfully() throws Exception {
    Operator operator = operatorRepository.save(new Operator());

    AppointmentTypeController.RegisterRequest request = validOneOnOneRequest(completeSchedule());

    mockMvc
        .perform(
            post("/api/operators/{operatorId}/appointment-types", operator.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    assertThat(appointmentScheduleRepository.count()).isOne();
    assertThat(tableCount("appointment_schedule_weekly_time_range")).isEqualTo(3);
    assertThat(tableCount("appointment_schedule_exception")).isEqualTo(2);
    assertThat(tableCount("appointment_schedule_exception_time_range")).isEqualTo(2);
    List<?> exceptionTypes =
        entityManager
            .createNativeQuery(
                "select exception_type from appointment_schedule_exception order by exception_date")
            .getResultList();
    assertThat(exceptionTypes).hasSize(2);
    assertThat(exceptionTypes.get(0)).isEqualTo("CLOSED");
    assertThat(exceptionTypes.get(1)).isEqualTo("CUSTOM_TIME");
  }

  @Test
  void rejectsOneOnOneAppointmentTypeWithoutStartInterval() throws Exception {
    AppointmentTypeController.RegisterRequest request =
        new AppointmentTypeController.RegisterRequest(
            "상담",
            AppointmentMethod.ONE_ON_ONE,
            (short) 50,
            (short) 10,
            null,
            true,
            validSchedule());

    mockMvc
        .perform(
            post("/api/operators/{operatorId}/appointment-types", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
    assertThat(appointmentTypeRepository.count()).isZero();
    assertThat(appointmentScheduleRepository.count()).isZero();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRegisterRequests")
  void rejectsInvalidRegisterRequest(
      String scenario, AppointmentTypeController.RegisterRequest request) throws Exception {
    mockMvc
        .perform(
            post("/api/operators/{operatorId}/appointment-types", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    assertThat(appointmentTypeRepository.count()).isZero();
    assertThat(appointmentScheduleRepository.count()).isZero();
  }

  private static Stream<Arguments> invalidRegisterRequests() {
    return Stream.of(
        Arguments.of(
            "blank name",
            new AppointmentTypeController.RegisterRequest(
                "   ",
                AppointmentMethod.ONE_ON_ONE,
                (short) 50,
                (short) 10,
                (short) 30,
                true,
                validSchedule())),
        Arguments.of(
            "name longer than 50 characters",
            new AppointmentTypeController.RegisterRequest(
                "a".repeat(51),
                AppointmentMethod.ONE_ON_ONE,
                (short) 50,
                (short) 10,
                (short) 30,
                true,
                validSchedule())),
        Arguments.of(
            "zero duration",
            new AppointmentTypeController.RegisterRequest(
                "상담",
                AppointmentMethod.ONE_ON_ONE,
                (short) 0,
                (short) 10,
                (short) 30,
                true,
                validSchedule())),
        Arguments.of(
            "negative preparation minutes",
            new AppointmentTypeController.RegisterRequest(
                "상담",
                AppointmentMethod.ONE_ON_ONE,
                (short) 50,
                (short) -1,
                (short) 30,
                true,
                validSchedule())),
        Arguments.of("missing schedule", validOneOnOneRequest(null)),
        Arguments.of(
            "empty weekly time ranges",
            validOneOnOneRequest(
                schedule(
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), List.of(), List.of()))),
        Arguments.of(
            "reversed schedule period",
            validOneOnOneRequest(
                schedule(
                    LocalDate.of(2026, 12, 31),
                    LocalDate.of(2026, 1, 1),
                    validWeeklyTimeRanges(),
                    List.of()))),
        Arguments.of(
            "reversed weekly time range",
            validOneOnOneRequest(
                schedule(
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    List.of(
                        weeklyTimeRange(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(9, 0))),
                    List.of()))),
        Arguments.of(
            "overlapping weekly time ranges",
            validOneOnOneRequest(
                schedule(
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    List.of(
                        weeklyTimeRange(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        weeklyTimeRange(
                            DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0))),
                    List.of()))),
        Arguments.of(
            "exception date outside schedule period",
            validOneOnOneRequest(
                schedule(
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    validWeeklyTimeRanges(),
                    List.of(
                        exception(
                            LocalDate.of(2027, 1, 1), ScheduleExceptionType.CLOSED, List.of()))))),
        Arguments.of(
            "duplicate exception dates",
            validOneOnOneRequest(
                schedule(
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    validWeeklyTimeRanges(),
                    List.of(
                        exception(
                            LocalDate.of(2026, 1, 2), ScheduleExceptionType.CLOSED, List.of()),
                        exception(
                            LocalDate.of(2026, 1, 2),
                            ScheduleExceptionType.CUSTOM_TIME,
                            validTimeRanges()))))),
        Arguments.of(
            "time range for CLOSED exception",
            validOneOnOneRequest(
                schedule(
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    validWeeklyTimeRanges(),
                    List.of(
                        exception(
                            LocalDate.of(2026, 1, 2),
                            ScheduleExceptionType.CLOSED,
                            validTimeRanges()))))),
        Arguments.of(
            "empty time ranges for CUSTOM_TIME exception",
            validOneOnOneRequest(
                schedule(
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    validWeeklyTimeRanges(),
                    List.of(
                        exception(
                            LocalDate.of(2026, 1, 2),
                            ScheduleExceptionType.CUSTOM_TIME,
                            List.of()))))),
        Arguments.of(
            "overlapping time ranges for CUSTOM_TIME exception",
            validOneOnOneRequest(
                schedule(
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    validWeeklyTimeRanges(),
                    List.of(
                        exception(
                            LocalDate.of(2026, 1, 2),
                            ScheduleExceptionType.CUSTOM_TIME,
                            List.of(
                                timeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                                timeRange(LocalTime.of(11, 0), LocalTime.of(13, 0)))))))));
  }

  private static AppointmentTypeController.ScheduleRequest validSchedule() {
    return new AppointmentTypeController.ScheduleRequest(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        List.of(
            new AppointmentTypeController.ScheduleRequest.WeeklyTimeRangeRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))),
        List.of());
  }

  @Test
  void rejectsRegistrationForNonexistentOperator() throws Exception {
    mockMvc
        .perform(
            post("/api/operators/{operatorId}/appointment-types", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validOneOnOneRequest(validSchedule()))))
        .andExpect(status().isNotFound());

    assertThat(appointmentTypeRepository.count()).isZero();
    assertThat(appointmentScheduleRepository.count()).isZero();
  }

  @Test
  void rollsBackRegistrationWhenScheduleValidationFails() {
    Operator operator = operatorRepository.save(new Operator());
    AppointmentScheduleParams params =
        new AppointmentScheduleParams(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            List.of(
                new AppointmentScheduleParams.WeeklyTimeRange(
                    DayOfWeek.MONDAY,
                    new AppointmentScheduleParams.TimeRange(
                        LocalTime.of(9, 0), LocalTime.of(18, 0)))),
            List.of(
                new AppointmentScheduleParams.ScheduleException(
                    LocalDate.of(2026, 1, 2), ScheduleExceptionType.CUSTOM_TIME, List.of())));

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
                    params))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(appointmentTypeRepository.count()).isZero();
    assertThat(appointmentScheduleRepository.count()).isZero();
  }

  private static AppointmentTypeController.RegisterRequest validOneOnOneRequest(
      AppointmentTypeController.ScheduleRequest schedule) {
    return new AppointmentTypeController.RegisterRequest(
        "상담", AppointmentMethod.ONE_ON_ONE, (short) 50, (short) 10, (short) 30, true, schedule);
  }

  private static AppointmentTypeController.ScheduleRequest completeSchedule() {
    return schedule(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        List.of(
            weeklyTimeRange(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
            weeklyTimeRange(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(18, 0)),
            weeklyTimeRange(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(17, 0))),
        List.of(
            exception(LocalDate.of(2026, 1, 6), ScheduleExceptionType.CLOSED, List.of()),
            exception(
                LocalDate.of(2026, 1, 7),
                ScheduleExceptionType.CUSTOM_TIME,
                List.of(
                    timeRange(LocalTime.of(11, 0), LocalTime.of(12, 0)),
                    timeRange(LocalTime.of(13, 0), LocalTime.of(14, 0))))));
  }

  private static AppointmentTypeController.ScheduleRequest schedule(
      LocalDate startDate,
      LocalDate endDate,
      List<AppointmentTypeController.ScheduleRequest.WeeklyTimeRangeRequest> weeklyTimeRanges,
      List<AppointmentTypeController.ScheduleRequest.ScheduleExceptionRequest> exceptions) {
    return new AppointmentTypeController.ScheduleRequest(
        startDate, endDate, weeklyTimeRanges, exceptions);
  }

  private static List<AppointmentTypeController.ScheduleRequest.WeeklyTimeRangeRequest>
      validWeeklyTimeRanges() {
    return List.of(weeklyTimeRange(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)));
  }

  private static AppointmentTypeController.ScheduleRequest.WeeklyTimeRangeRequest weeklyTimeRange(
      DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    return new AppointmentTypeController.ScheduleRequest.WeeklyTimeRangeRequest(
        dayOfWeek, startTime, endTime);
  }

  private static AppointmentTypeController.ScheduleRequest.ScheduleExceptionRequest exception(
      LocalDate date,
      ScheduleExceptionType type,
      List<AppointmentTypeController.ScheduleRequest.TimeRangeRequest> timeRanges) {
    return new AppointmentTypeController.ScheduleRequest.ScheduleExceptionRequest(
        date, type, timeRanges);
  }

  private static List<AppointmentTypeController.ScheduleRequest.TimeRangeRequest> validTimeRanges() {
    return List.of(timeRange(LocalTime.of(9, 0), LocalTime.of(18, 0)));
  }

  private static AppointmentTypeController.ScheduleRequest.TimeRangeRequest timeRange(
      LocalTime startTime, LocalTime endTime) {
    return new AppointmentTypeController.ScheduleRequest.TimeRangeRequest(startTime, endTime);
  }

  private long tableCount(String tableName) {
    return ((Number)
            entityManager.createNativeQuery("select count(*) from " + tableName).getSingleResult())
        .longValue();
  }

  @Test
  void updateActiveAppointmentTypeSuccessfully() throws Exception {
    Operator operator = operatorRepository.save(new Operator());
    Long operatorId = operator.getId();

    AppointmentType appointmentType =
        appointmentTypeRepository.save(
            new AppointmentType(
                "상담",
                AppointmentMethod.ONE_ON_ONE,
                (short) 50,
                (short) 10,
                (short) 30,
                true,
                operator));
    Long appointmentTypeId = appointmentType.getId();

    AppointmentTypeController.UpdateRequest request =
        new AppointmentTypeController.UpdateRequest(false);

    mockMvc
        .perform(
            patch(
                    "/api/operators/{operatorId}/appointment-types/{appointmentTypeId}",
                    operatorId,
                    appointmentTypeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    AppointmentType updated =
        appointmentTypeRepository
            .findByIdAndOperatorId(appointmentTypeId, operatorId)
            .orElseThrow();
    assertThat(updated.isActive()).isFalse();
  }

  @Test
  void rejectsUpdateForAnotherOperatorsAppointmentType() throws Exception {
    Operator operator1 = operatorRepository.save(new Operator());
    Operator operator2 = operatorRepository.save(new Operator());

    AppointmentType appointmentType =
        appointmentTypeRepository.save(
            new AppointmentType(
                "상담",
                AppointmentMethod.ONE_ON_ONE,
                (short) 50,
                (short) 10,
                (short) 10,
                true,
                operator1));

    AppointmentTypeController.UpdateRequest request =
        new AppointmentTypeController.UpdateRequest(false);

    mockMvc
        .perform(
            patch(
                    "/api/operators/{operator2Id}/appointment-types/{appointmentTypeId}",
                    operator2.getId(),
                    appointmentType.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }
}
