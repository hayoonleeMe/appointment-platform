package io.github.hayoonleeme.appointmentplatform.appointment.type;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hayoonleeme.appointmentplatform.operator.Operator;
import org.junit.jupiter.api.Test;

class AppointmentTypeTest {
  @Test
  void rejectsOneOnOneWithoutStartInterval() {
    assertThatThrownBy(
            () ->
                new AppointmentType(
                    "1",
                    AppointmentMethod.ONE_ON_ONE,
                    (short) 10,
                    (short) 10,
                    null,
                    true,
                    new Operator()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsGroupWithStartInterval() {
    assertThatThrownBy(
            () ->
                new AppointmentType(
                    "1",
                    AppointmentMethod.GROUP,
                    (short) 10,
                    (short) 10,
                    (short) 10,
                    true,
                    new Operator()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
