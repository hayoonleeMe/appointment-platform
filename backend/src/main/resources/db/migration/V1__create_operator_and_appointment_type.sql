CREATE
    TABLE
        operator(
            id BIGINT AUTO_INCREMENT NOT NULL,
            CONSTRAINT PRIMARY KEY(id)
        );

CREATE
    TABLE
        appointment_type(
            id BIGINT AUTO_INCREMENT NOT NULL,
            name VARCHAR(50) NOT NULL,
            appointment_method VARCHAR(16) NOT NULL,
            duration_minutes SMALLINT NOT NULL,
            preparation_minutes SMALLINT NOT NULL,
            start_interval_minutes SMALLINT NULL,
            active TINYINT(1) NOT NULL,
            operator_id BIGINT NOT NULL,
            CONSTRAINT PRIMARY KEY(id),
            CONSTRAINT fk_appointment_type_operator FOREIGN KEY(operator_id) REFERENCES operator(id),
            CONSTRAINT chk_appointment_type_duration_minutes CHECK(
                duration_minutes > 0
            ),
            CONSTRAINT chk_appointment_type_preparation_minutes CHECK(
                preparation_minutes >= 0
            ),
            CONSTRAINT chk_appointment_type_appointment_method CHECK(
                appointment_method IN(
                    'ONE_ON_ONE',
                    'GROUP'
                )
            ),
            CONSTRAINT chk_appointment_type_start_interval_minutes CHECK(
                (
                    appointment_method = 'ONE_ON_ONE'
                    AND start_interval_minutes > 0
                )
                OR(
                    appointment_method = 'GROUP'
                    AND start_interval_minutes IS NULL
                )
            )
        );