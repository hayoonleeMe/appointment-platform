package io.github.hayoonleeme.appointmentplatform.appointment.type;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operators/{operatorId}/appointment-types")
@RequiredArgsConstructor
public class AppointmentTypeController {
  private final AppointmentTypeService appointmentTypeService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(
      @PathVariable Long operatorId, @RequestBody @Valid RegisterRequest request) {
    AppointmentType appointmentType =
        appointmentTypeService.register(
            operatorId,
            request.name,
            request.appointmentMethod,
            request.durationMinutes,
            request.preparationMinutes,
            request.startIntervalMinutes,
            request.active);
    return new RegisterResponse(appointmentType.getId());
  }

  @PatchMapping("/{appointmentTypeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(
      @PathVariable Long operatorId,
      @PathVariable Long appointmentTypeId,
      @RequestBody @Valid UpdateRequest request) {
    appointmentTypeService.updateActive(operatorId, appointmentTypeId, request.active);
  }

  public record RegisterRequest(
      @NotBlank(message = "name must not be blank")
          @Length(max = 50, message = "name must not exceed 50 characters")
          String name,
      @NotNull(message = "appointmentMethod must not be null") AppointmentMethod appointmentMethod,
      @NotNull(message = "durationMinutes must not be null")
          @Positive(message = "durationMinutes must be positive")
          Short durationMinutes,
      @NotNull(message = "preparationMinutes must not be null")
          @PositiveOrZero(message = "preparationMinutes must be zero or positive")
          Short preparationMinutes,
      @Positive(message = "startIntervalMinutes must be positive") Short startIntervalMinutes,
      @NotNull(message = "active must not be null") Boolean active) {
    @AssertTrue(
        message = "startIntervalMinutes must be present for ONE_ON_ONE and absent for GROUP")
    public boolean isStartIntervalMinutesValid() {
      if (appointmentMethod == AppointmentMethod.ONE_ON_ONE && startIntervalMinutes == null) {
        return false;
      }
      if (appointmentMethod == AppointmentMethod.GROUP && startIntervalMinutes != null) {
        return false;
      }
      return true;
    }
  }

  public record RegisterResponse(Long id) {}

  public record UpdateRequest(@NotNull(message = "active must not be null") Boolean active) {}
}
