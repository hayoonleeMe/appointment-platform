CREATE
    TABLE
        appointment_schedule(
            id BIGINT NOT NULL AUTO_INCREMENT,
            appointment_type_id BIGINT NOT NULL UNIQUE,
            start_date DATE NOT NULL,
            end_date DATE NOT NULL,
            CONSTRAINT PRIMARY KEY(id),
            CONSTRAINT fk_appointment_schedule_appointment_type FOREIGN KEY(appointment_type_id) REFERENCES appointment_type(id),
            CONSTRAINT chk_appointment_schedule_start_date_end_date CHECK(
                start_date <= end_date
            )
        );

CREATE
    TABLE
        appointment_schedule_weekly_time_range(
            id BIGINT NOT NULL AUTO_INCREMENT,
            appointment_schedule_id BIGINT NOT NULL,
            day_of_week VARCHAR(9) NOT NULL,
            start_time TIME NOT NULL,
            end_time TIME NOT NULL,
            CONSTRAINT PRIMARY KEY(id),
            CONSTRAINT fk_appointment_schedule_weekly_time_range_appointment_schedule FOREIGN KEY(appointment_schedule_id) REFERENCES appointment_schedule(id),
            CONSTRAINT chk_appointment_schedule_weekly_time_range_day_of_week CHECK(
                day_of_week IN(
                    'MONDAY',
                    'TUESDAY',
                    'WEDNESDAY',
                    'THURSDAY',
                    'FRIDAY',
                    'SATURDAY',
                    'SUNDAY'
                )
            ),
            CONSTRAINT chk_appointment_schedule_weekly_time_range_start_time CHECK(
                start_time >= '00:00:00'
                AND start_time < '24:00:00'
            ),
            CONSTRAINT chk_appointment_schedule_weekly_time_range_end_time CHECK(
                end_time >= '00:00:00'
                AND end_time < '24:00:00'
            ),
            CONSTRAINT chk_appointment_schedule_weekly_time_range_start_time_end_time CHECK(
                start_time < end_time
            )
        );

CREATE
    TABLE
        appointment_schedule_exception(
            id BIGINT NOT NULL AUTO_INCREMENT,
            appointment_schedule_id BIGINT NOT NULL,
            exception_date DATE NOT NULL,
            exception_type VARCHAR(16) NOT NULL,
            CONSTRAINT PRIMARY KEY(id),
            CONSTRAINT fk_appointment_schedule_exception_appointment_schedule FOREIGN KEY(appointment_schedule_id) REFERENCES appointment_schedule(id),
            CONSTRAINT chk_appointment_schedule_exception_exception_type CHECK(
                exception_type IN(
                    'CLOSED',
                    'CUSTOM_TIME'
                )
            ),
            CONSTRAINT uk_appointment_schedule_exception_schedule_date UNIQUE(
                appointment_schedule_id,
                exception_date
            )
        );

CREATE
    TABLE
        appointment_schedule_exception_time_range(
            id BIGINT NOT NULL AUTO_INCREMENT,
            appointment_schedule_exception_id BIGINT NOT NULL,
            start_time TIME NOT NULL,
            end_time TIME NOT NULL,
            CONSTRAINT PRIMARY KEY(id),
            CONSTRAINT fk_appointment_schedule_exception_time_range_exception FOREIGN KEY(appointment_schedule_exception_id) REFERENCES appointment_schedule_exception(id),
            CONSTRAINT chk_appointment_schedule_exception_time_range_start_time CHECK(
                start_time >= '00:00:00'
                AND start_time < '24:00:00'
            ),
            CONSTRAINT chk_appointment_schedule_exception_time_range_end_time CHECK(
                end_time >= '00:00:00'
                AND end_time < '24:00:00'
            ),
            CONSTRAINT chk_schedule_exception_time_range_start_time_end_time CHECK(
                start_time < end_time
            )
        );
