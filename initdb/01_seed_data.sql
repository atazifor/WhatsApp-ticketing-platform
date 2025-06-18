-- Insert locations
INSERT INTO location (name, region) VALUES
                                        ('Douala', 'Littoral'),
                                        ('Yaounde', 'Centre'),
                                        ('Buea', 'Southwest'),
                                        ('Ngoundere', 'Adamawa'),
                                        ('Bafoussam', 'West'),
                                        ('Kribi', 'South'),
                                        ('Bamenda', 'Northwest');

-- Insert agencies
INSERT INTO agency (name, has_api, slug, max_tickets_per_booking) VALUES
                                                                      ('Parklane Travels', false, 'parklane-travels', 4),
                                                                      ('Touristique Express', false, 'touristique-express', 4),
                                                                      ('Global Express', false, 'global-express', 4),
                                                                      ('Men Travel', false, 'men-travel', 4),
                                                                      ('Nso Boys', false, 'nso-boys', 4),
                                                                      ('Moghamo', false, 'moghamo', 4),
                                                                      ('Blue Bird', false, 'blue-bird', 4);

-- Insert travel classes for each agency
-- Parklane Travels (VIP, Regular - no mixed classes per bus)
INSERT INTO travel_class (name, agency_id) VALUES
                                               ('VIP', (SELECT id FROM agency WHERE name = 'Parklane Travels')),
                                               ('Regular', (SELECT id FROM agency WHERE name = 'Parklane Travels'));

-- Touristique Express (VIP, Regular mixed classes per bus)
INSERT INTO travel_class (name, agency_id) VALUES
                                               ('VIP', (SELECT id FROM agency WHERE name = 'Touristique Express')),
                                               ('Regular', (SELECT id FROM agency WHERE name = 'Touristique Express'));

-- Global Express (VIP, Regular open seating)
INSERT INTO travel_class (name, agency_id) VALUES
                                               ('VIP', (SELECT id FROM agency WHERE name = 'Global Express')),
                                               ('Regular', (SELECT id FROM agency WHERE name = 'Global Express'));

-- Men Travel (Master+, Master Class, VIP, Regular with mixed configurations)
INSERT INTO travel_class (name, agency_id) VALUES
                                               ('Master+', (SELECT id FROM agency WHERE name = 'Men Travel')),
                                               ('Master Class', (SELECT id FROM agency WHERE name = 'Men Travel')),
                                               ('VIP', (SELECT id FROM agency WHERE name = 'Men Travel')),
                                               ('Regular', (SELECT id FROM agency WHERE name = 'Men Travel'));

-- Nso Boys (Regular only)
INSERT INTO travel_class (name, agency_id) VALUES
    ('Regular', (SELECT id FROM agency WHERE name = 'Nso Boys'));

-- Moghamo (VIP, Regular mixed classes)
INSERT INTO travel_class (name, agency_id) VALUES
                                               ('VIP', (SELECT id FROM agency WHERE name = 'Moghamo')),
                                               ('Regular', (SELECT id FROM agency WHERE name = 'Moghamo'));

-- Blue Bird (VIP, Regular mixed classes)
INSERT INTO travel_class (name, agency_id) VALUES
                                               ('VIP', (SELECT id FROM agency WHERE name = 'Blue Bird')),
                                               ('Regular', (SELECT id FROM agency WHERE name = 'Blue Bird'));

-- Insert buses for each agency
-- Parklane Travels buses
INSERT INTO bus (agency_id, plate_number, bus_number, model_name, is_open_seating) VALUES
                                                                                       ((SELECT id FROM agency WHERE name = 'Parklane Travels'), 'LT123PA', 'PL001', 'Toyota Coaster', true),
                                                                                       ((SELECT id FROM agency WHERE name = 'Parklane Travels'), 'LT124PA', 'PL002', 'Toyota Coaster', true);

-- Touristique Express buses
INSERT INTO bus (agency_id, plate_number, bus_number, model_name, is_open_seating) VALUES
                                                                                       ((SELECT id FROM agency WHERE name = 'Touristique Express'), 'LT125TE', 'TE001', 'Mercedes Sprinter', false),
                                                                                       ((SELECT id FROM agency WHERE name = 'Touristique Express'), 'LT126TE', 'TE002', 'Mercedes Sprinter', false);

-- Global Express buses
INSERT INTO bus (agency_id, plate_number, bus_number, model_name, is_open_seating) VALUES
                                                                                       ((SELECT id FROM agency WHERE name = 'Global Express'), 'LT127GE', 'GE001', 'Higer Bus', true),
                                                                                       ((SELECT id FROM agency WHERE name = 'Global Express'), 'LT128GE', 'GE002', 'Higer Bus', true);

-- Men Travel buses
INSERT INTO bus (agency_id, plate_number, bus_number, model_name, is_open_seating) VALUES
                                                                                       ((SELECT id FROM agency WHERE name = 'Men Travel'), 'LT129MT', 'MT001', 'Yutong Bus', false),
                                                                                       ((SELECT id FROM agency WHERE name = 'Men Travel'), 'LT130MT', 'MT002', 'Yutong Bus', true),
                                                                                       ((SELECT id FROM agency WHERE name = 'Men Travel'), 'LT131MT', 'MT003', 'Yutong Bus', false);

-- Nso Boys buses
INSERT INTO bus (agency_id, plate_number, bus_number, model_name, is_open_seating) VALUES
                                                                                       ((SELECT id FROM agency WHERE name = 'Nso Boys'), 'NW132NB', 'NB001', 'Toyota Hiace', true),
                                                                                       ((SELECT id FROM agency WHERE name = 'Nso Boys'), 'NW133NB', 'NB002', 'Toyota Hiace', true);

-- Moghamo buses
INSERT INTO bus (agency_id, plate_number, bus_number, model_name, is_open_seating) VALUES
                                                                                       ((SELECT id FROM agency WHERE name = 'Moghamo'), 'NW134MG', 'MG001', 'Toyota Hiace', true),
                                                                                       ((SELECT id FROM agency WHERE name = 'Moghamo'), 'NW135MG', 'MG002', 'Toyota Hiace', true);

-- Blue Bird buses
INSERT INTO bus (agency_id, plate_number, bus_number, model_name, is_open_seating) VALUES
                                                                                       ((SELECT id FROM agency WHERE name = 'Blue Bird'), 'LT136BB', 'BB001', 'Toyota Coaster', true),
                                                                                       ((SELECT id FROM agency WHERE name = 'Blue Bird'), 'LT137BB', 'BB002', 'Toyota Coaster', true);

-- Insert schedules with future dates (starting from tomorrow)
-- Parklane Travels routes: Douala-Yaounde, Yaounde-Buea
INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Douala'),
    (SELECT id FROM location WHERE name = 'Yaounde'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '08:00',
    18
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Parklane Travels'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT123PA';

INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Yaounde'),
    (SELECT id FROM location WHERE name = 'Buea'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '14:00',
    18
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Parklane Travels'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT124PA';

-- Touristique Express routes: Douala-Yaounde, Yaounde-Ngoundere, Douala-Ngoundere
INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Douala'),
    (SELECT id FROM location WHERE name = 'Yaounde'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '07:30',
    14
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Touristique Express'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT125TE';

INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Yaounde'),
    (SELECT id FROM location WHERE name = 'Ngoundere'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '13:00',
    14
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Touristique Express'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT126TE';

INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Douala'),
    (SELECT id FROM location WHERE name = 'Ngoundere'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '06:00',
    14
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Touristique Express'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT125TE'
  AND n % 2 = 0; -- Only every other day

-- Global Express routes: Douala-Yaounde, Yaounde-Bafoussam, Bafoussam-Douala
INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Douala'),
    (SELECT id FROM location WHERE name = 'Yaounde'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '09:00',
    16
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Global Express'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT127GE';

INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Yaounde'),
    (SELECT id FROM location WHERE name = 'Bafoussam'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '12:30',
    16
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Global Express'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT128GE';

INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Bafoussam'),
    (SELECT id FROM location WHERE name = 'Douala'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '15:00',
    16
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Global Express'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT127GE'
  AND n % 2 = 0; -- Only every other day

-- Men Travel routes: Douala-Yaounde, Yaounde-Kribi, Kribi-Douala
INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Douala'),
    (SELECT id FROM location WHERE name = 'Yaounde'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '08:30',
    20
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Men Travel'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT129MT';

INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Yaounde'),
    (SELECT id FROM location WHERE name = 'Kribi'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '14:00',
    20
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Men Travel'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT130MT';

INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Kribi'),
    (SELECT id FROM location WHERE name = 'Douala'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '16:00',
    20
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Men Travel'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT131MT'
  AND n % 2 = 0; -- Only every other day

-- Nso Boys routes: Bamenda-Yaounde, Douala-Bamenda
INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Bamenda'),
    (SELECT id FROM location WHERE name = 'Yaounde'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '07:00',
    14
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Nso Boys'
  AND b.agency_id = a.id
  AND b.plate_number = 'NW132NB';

INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Douala'),
    (SELECT id FROM location WHERE name = 'Bamenda'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '12:00',
    14
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Nso Boys'
  AND b.agency_id = a.id
  AND b.plate_number = 'NW133NB';

-- Moghamo routes: Bamenda-Yaounde, Douala-Bamenda
INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Bamenda'),
    (SELECT id FROM location WHERE name = 'Yaounde'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '06:30',
    14
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Moghamo'
  AND b.agency_id = a.id
  AND b.plate_number = 'NW134MG';

INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Douala'),
    (SELECT id FROM location WHERE name = 'Bamenda'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '11:30',
    14
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Moghamo'
  AND b.agency_id = a.id
  AND b.plate_number = 'NW135MG';

-- Blue Bird routes: Yaounde-Bafoussam, Douala-Bafoussam
INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Yaounde'),
    (SELECT id FROM location WHERE name = 'Bafoussam'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '10:00',
    16
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Blue Bird'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT136BB';

INSERT INTO schedule (agency_id, bus_id, from_location, to_location, travel_date, departure_time, total_seats)
SELECT
    a.id,
    b.id,
    (SELECT id FROM location WHERE name = 'Douala'),
    (SELECT id FROM location WHERE name = 'Bafoussam'),
    CURRENT_DATE + INTERVAL '1 day' + (n || ' days')::interval,
    '15:00',
    16
FROM agency a, bus b, generate_series(0, 30) n
WHERE a.name = 'Blue Bird'
  AND b.agency_id = a.id
  AND b.plate_number = 'LT137BB';

-- Insert prices for each schedule and class
-- Parklane Travels (no mixed classes per bus)
INSERT INTO schedule_class_price (schedule_id, travel_class_id, price)
SELECT
    s.id,
    tc.id,
    CASE
        WHEN tc.name = 'VIP' THEN 8000
        ELSE 5000
        END
FROM schedule s
         JOIN agency a ON s.agency_id = a.id
         JOIN travel_class tc ON tc.agency_id = a.id
WHERE a.name = 'Parklane Travels'
  AND (
    (s.bus_id = (SELECT id FROM bus WHERE plate_number = 'LT123PA') AND tc.name = 'VIP') OR
    (s.bus_id = (SELECT id FROM bus WHERE plate_number = 'LT124PA') AND tc.name = 'Regular')
    );

-- Touristique Express (mixed classes)
INSERT INTO schedule_class_price (schedule_id, travel_class_id, price)
SELECT
    s.id,
    tc.id,
    CASE
        WHEN tc.name = 'VIP' THEN 8500
        ELSE 5500
        END
FROM schedule s
         JOIN agency a ON s.agency_id = a.id
         JOIN travel_class tc ON tc.agency_id = a.id
WHERE a.name = 'Touristique Express';

-- Global Express (mixed classes, open seating)
INSERT INTO schedule_class_price (schedule_id, travel_class_id, price)
SELECT
    s.id,
    tc.id,
    CASE
        WHEN tc.name = 'VIP' THEN 7500
        ELSE 4500
        END
FROM schedule s
         JOIN agency a ON s.agency_id = a.id
         JOIN travel_class tc ON tc.agency_id = a.id
WHERE a.name = 'Global Express';

-- Men Travel (complex class structure)
INSERT INTO schedule_class_price (schedule_id, travel_class_id, price)
SELECT
    s.id,
    tc.id,
    CASE
        WHEN tc.name = 'Master+' THEN 12000
        WHEN tc.name = 'Master Class' THEN 10000
        WHEN tc.name = 'VIP' THEN 8000
        ELSE 5000
        END
FROM schedule s
         JOIN agency a ON s.agency_id = a.id
         JOIN travel_class tc ON tc.agency_id = a.id
WHERE a.name = 'Men Travel'
  AND (
    (s.bus_id = (SELECT id FROM bus WHERE plate_number = 'LT129MT') AND tc.name IN ('Master+', 'Master Class')) OR
    (s.bus_id = (SELECT id FROM bus WHERE plate_number = 'LT130MT') AND tc.name IN ('VIP', 'Regular')) OR
    (s.bus_id = (SELECT id FROM bus WHERE plate_number = 'LT131MT') AND tc.name = 'VIP')
    );

-- Nso Boys (Regular only)
INSERT INTO schedule_class_price (schedule_id, travel_class_id, price)
SELECT
    s.id,
    tc.id,
    4000
FROM schedule s
         JOIN agency a ON s.agency_id = a.id
         JOIN travel_class tc ON tc.agency_id = a.id
WHERE a.name = 'Nso Boys';

-- Moghamo (mixed classes)
INSERT INTO schedule_class_price (schedule_id, travel_class_id, price)
SELECT
    s.id,
    tc.id,
    CASE
        WHEN tc.name = 'VIP' THEN 7000
        ELSE 4500
        END
FROM schedule s
         JOIN agency a ON s.agency_id = a.id
         JOIN travel_class tc ON tc.agency_id = a.id
WHERE a.name = 'Moghamo';

-- Blue Bird (mixed classes)
INSERT INTO schedule_class_price (schedule_id, travel_class_id, price)
SELECT
    s.id,
    tc.id,
    CASE
        WHEN tc.name = 'VIP' THEN 7500
        ELSE 5000
        END
FROM schedule s
         JOIN agency a ON s.agency_id = a.id
         JOIN travel_class tc ON tc.agency_id = a.id
WHERE a.name = 'Blue Bird';