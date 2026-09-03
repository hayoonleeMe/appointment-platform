package io.github.hayoonleeme.appointmentplatform.appointment.type;

import io.github.hayoonleeme.appointmentplatform.operator.Operator;
import io.github.hayoonleeme.appointmentplatform.operator.OperatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AppointmentTypeService {
  private final OperatorRepository operatorRepository;
  private final AppointmentTypeRepository appointmentTypeRepository;

  @Transactional
  public AppointmentType register(
      Long operatorId,
      String name,
      AppointmentMethod appointmentMethod,
      Short durationMinutes,
      Short preparationMinutes,
      Short startIntervalMinutes,
      boolean active) {
    Operator operator =
        operatorRepository
            .findById(operatorId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "operator not found"));
    AppointmentType appointmentType =
        new AppointmentType(
            name,
            appointmentMethod,
            durationMinutes,
            preparationMinutes,
            startIntervalMinutes,
            active,
            operator);
    return appointmentTypeRepository.save(appointmentType);
  }

  @Transactional
  public void updateActive(Long operatorId, Long appointmentTypeId, boolean active) {
    AppointmentType appointmentType =
        appointmentTypeRepository
            .findByIdAndOperatorId(appointmentTypeId, operatorId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "appointment type not found"));
    appointmentType.updateActive(active);
  }
}
