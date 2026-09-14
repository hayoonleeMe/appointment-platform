package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import io.github.hayoonleeme.appointmentplatform.appointment.type.AppointmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AppointmentScheduleService {
  private final AppointmentScheduleRepository appointmentScheduleRepository;

  @Transactional
  public void register(AppointmentType appointmentType, AppointmentScheduleParams params) {
    AppointmentSchedule schedule =
        new AppointmentSchedule(appointmentType, params.startDate(), params.endDate());

    params
        .weeklyTimeRanges()
        .forEach(
            weeklyTimeRange ->
                schedule.addWeeklyTimeRange(
                    weeklyTimeRange.dayOfWeek(),
                    weeklyTimeRange.timeRange().startTime(),
                    weeklyTimeRange.timeRange().endTime()));

    for (AppointmentScheduleParams.ScheduleException paramScheduleException :
        params.scheduleExceptions()) {
      AppointmentScheduleException scheduleException =
          schedule.addScheduleException(
              paramScheduleException.date(), paramScheduleException.type());
      paramScheduleException
          .timeRanges()
          .forEach(
              timeRange ->
                  scheduleException.addExceptionTimeRange(
                      timeRange.startTime(), timeRange.endTime()));
    }

    schedule.validate();
    appointmentScheduleRepository.save(schedule);
  }

  @Transactional(readOnly = true)
  public AppointmentScheduleDetails getSchedule(Long operatorId, Long appointmentTypeId) {
    return AppointmentScheduleDetails.from(
        appointmentScheduleRepository
            .findByAppointmentTypeOperatorIdAndAppointmentTypeId(operatorId, appointmentTypeId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "appointment schedule not found")));
  }

  @Transactional
  public void replaceSchedule(
      Long operatorId, Long appointmentTypeId, AppointmentScheduleParams params) {
    AppointmentSchedule schedule =
        appointmentScheduleRepository
            .findByAppointmentTypeOperatorIdAndAppointmentTypeId(operatorId, appointmentTypeId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "appointment schedule not found"));
    schedule.replace(params);
  }
}
