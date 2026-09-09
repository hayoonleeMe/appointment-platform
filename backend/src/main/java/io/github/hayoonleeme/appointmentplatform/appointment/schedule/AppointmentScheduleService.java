package io.github.hayoonleeme.appointmentplatform.appointment.schedule;

import io.github.hayoonleeme.appointmentplatform.appointment.type.AppointmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
