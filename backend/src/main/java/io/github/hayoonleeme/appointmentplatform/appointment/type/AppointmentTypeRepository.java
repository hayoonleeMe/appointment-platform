package io.github.hayoonleeme.appointmentplatform.appointment.type;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentTypeRepository extends JpaRepository<AppointmentType, Long> {
  Optional<AppointmentType> findByIdAndOperatorId(Long id, Long operatorId);
}
