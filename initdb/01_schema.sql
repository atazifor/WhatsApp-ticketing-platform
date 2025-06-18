CREATE
EXTENSION IF NOT EXISTS "pgcrypto";

-- Location table
CREATE TABLE location
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         TEXT NOT NULL,
    region       TEXT,
    country_code TEXT             DEFAULT 'CM',
    lat          DOUBLE PRECISION,
    lon          DOUBLE PRECISION,
    created_at   TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP        NOT NULL DEFAULT now()
);

-- Agency table
CREATE TABLE agency
(
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    TEXT    NOT NULL,
    has_api                 BOOLEAN NOT NULL DEFAULT false,
    api_base_url            TEXT,
    slug                    TEXT UNIQUE,
    logo_url                TEXT,
    max_tickets_per_booking INTEGER          DEFAULT 4,
    created_at              TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP        NOT NULL DEFAULT now()
);

-- Agency contact table
CREATE TABLE agency_contact
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id  UUID REFERENCES agency (id) ON DELETE CASCADE,
    city       TEXT NOT NULL,
    phone      TEXT,
    address    TEXT,
    created_at TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at TIMESTAMP        NOT NULL DEFAULT now()
);

-- Travel class table
CREATE TABLE travel_class
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    description TEXT,
    agency_id   UUID REFERENCES agency (id) ON DELETE CASCADE,
    created_at  TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP        NOT NULL DEFAULT now()
);

-- Bus table
CREATE TABLE bus
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id       UUID REFERENCES agency (id) ON DELETE CASCADE,
    plate_number    TEXT UNIQUE,
    bus_number      TEXT,
    model_name      TEXT,
    seat_layout     JSONB,
    is_open_seating BOOLEAN          DEFAULT false,
    created_at      TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP        NOT NULL DEFAULT now()
);

-- Schedule table
CREATE TABLE schedule
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id      UUID REFERENCES agency (id),
    bus_id         UUID REFERENCES bus (id),
    from_location  UUID REFERENCES location (id),
    to_location    UUID REFERENCES location (id),
    travel_date    DATE    NOT NULL,
    departure_time TIME    NOT NULL,
    total_seats    INTEGER NOT NULL,
    created_at     TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP        NOT NULL DEFAULT now()
);

-- schedule travel level and price
CREATE TABLE schedule_class_price
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id     UUID    NOT NULL REFERENCES schedule (id),
    travel_class_id UUID    NOT NULL REFERENCES travel_class (id),
    price           INTEGER NOT NULL,
    created_at     TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP        NOT NULL DEFAULT now(),
    UNIQUE (schedule_id, travel_class_id)
);

-- Booking session to group tickets
CREATE TABLE booking
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_name TEXT,
    phone         TEXT,
    email         TEXT,
    is_round_trip BOOLEAN DEFAULT FALSE,
    more_details  TEXT,
    created_at    TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP        NOT NULL DEFAULT now()
);

-- Optional: track seat status if needed
CREATE TABLE seat
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id     UUID REFERENCES schedule (id) ON DELETE CASCADE,
    seat_number     TEXT NOT NULL,
    is_sold         BOOLEAN          DEFAULT false,
    travel_class_id UUID REFERENCES travel_class (id),
    created_at      TIMESTAMP        NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP        NOT NULL DEFAULT now(),
    UNIQUE (schedule_id, seat_number)
);

-- Ticket table
-- Create the new ticket table
CREATE TABLE ticket
(
    id              UUID PRIMARY KEY   DEFAULT gen_random_uuid(),
    booking_id      UUID      NOT NULL REFERENCES booking (id) ON DELETE CASCADE,
    schedule_id     UUID      NOT NULL REFERENCES schedule (id) ON DELETE CASCADE,
    passenger_name  TEXT      NOT NULL,
    passenger_email TEXT,
    passenger_phone TEXT,
    seat_number     TEXT,
    qr_code         TEXT,
    is_primary      BOOLEAN   NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_ticket_schedule_seat UNIQUE (schedule_id, seat_number)
);

CREATE INDEX idx_seat_schedule_sold ON seat(schedule_id, is_sold);
CREATE INDEX idx_seat_schedule_number ON seat(schedule_id, seat_number);