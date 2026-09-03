package io.github.hayoonleeme.appointmentplatform.appointment.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.hayoonleeme.appointmentplatform.TestContainerConfig;
import io.github.hayoonleeme.appointmentplatform.operator.Operator;
import io.github.hayoonleeme.appointmentplatform.operator.OperatorRepository;
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
  @Autowired private JsonMapper jsonMapper;

  @AfterEach
  void clean() {
    appointmentTypeRepository.deleteAll();
    operatorRepository.deleteAll();
  }

  @Test
  void registerOneOnOneAppointmentTypeSuccessfully() throws Exception {
    Operator operator = operatorRepository.save(new Operator());
    Long operatorId = operator.getId();

    AppointmentTypeController.RegisterRequest request =
        new AppointmentTypeController.RegisterRequest(
            "상담", AppointmentMethod.ONE_ON_ONE, (short) 50, (short) 10, (short) 30, true);

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
  }

  @Test
  void registerGroupAppointmentTypeSuccessfully() throws Exception {
    Operator operator = operatorRepository.save(new Operator());
    Long operatorId = operator.getId();

    AppointmentTypeController.RegisterRequest request =
        new AppointmentTypeController.RegisterRequest(
            "상담", AppointmentMethod.GROUP, (short) 50, (short) 10, null, true);

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
  }

  @Test
  void rejectsOneOnOneAppointmentTypeWithoutStartInterval() throws Exception {
    AppointmentTypeController.RegisterRequest request =
        new AppointmentTypeController.RegisterRequest(
            "상담", AppointmentMethod.ONE_ON_ONE, (short) 50, (short) 10, null, true);

    mockMvc
        .perform(
            post("/api/operators/{operatorId}/appointment-types", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
    assertThat(appointmentTypeRepository.count()).isZero();
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
  }

  private static Stream<Arguments> invalidRegisterRequests() {
    return Stream.of(
        Arguments.of(
            "blank name",
            new AppointmentTypeController.RegisterRequest(
                "   ", AppointmentMethod.ONE_ON_ONE, (short) 50, (short) 10, (short) 30, true)),
        Arguments.of(
            "name longer than 50 characters",
            new AppointmentTypeController.RegisterRequest(
                "a".repeat(51),
                AppointmentMethod.ONE_ON_ONE,
                (short) 50,
                (short) 10,
                (short) 30,
                true)),
        Arguments.of(
            "zero duration",
            new AppointmentTypeController.RegisterRequest(
                "상담", AppointmentMethod.ONE_ON_ONE, (short) 0, (short) 10, (short) 30, true)),
        Arguments.of(
            "negative preparation minutes",
            new AppointmentTypeController.RegisterRequest(
                "상담", AppointmentMethod.ONE_ON_ONE, (short) 50, (short) -1, (short) 30, true)));
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
