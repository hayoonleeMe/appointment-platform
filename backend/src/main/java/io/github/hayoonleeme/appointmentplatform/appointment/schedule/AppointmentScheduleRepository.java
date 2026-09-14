package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentScheduleRepository extends JpaRepository<AppointmentSchedule, Long> {
  Optional<AppointmentSchedule> findByAppointmentTypeOperatorIdAndAppointmentTypeId(
      Long operatorId, Long appointmentTypeId);
}
