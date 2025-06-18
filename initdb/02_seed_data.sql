-- First, let's create a function to generate seat data
CREATE OR REPLACE FUNCTION generate_seats_for_schedule(
    schedule_id UUID,
    bus_id UUID,
    total_seats INT,
    sold_percentage INT DEFAULT 0
) RETURNS VOID AS $$
DECLARE
seat_num TEXT;
    is_open BOOLEAN;
    travel_classes UUID[];
    class_idx INT;
    is_sold_flag BOOLEAN;
BEGIN
    -- Get bus seating type
SELECT is_open_seating INTO is_open FROM bus WHERE id = bus_id;

-- Get available travel classes for this bus's agency
SELECT array_agg(id) INTO travel_classes
FROM travel_class
WHERE agency_id = (SELECT agency_id FROM bus WHERE id = bus_id);

-- Generate seats
FOR i IN 1..total_seats LOOP
        -- Format seat number (e.g., A1, B2, etc.)
        seat_num := i;

        -- Randomly determine if seat is sold (for 65% sold schedules)
        is_sold_flag := (random() < sold_percentage::float/100);

        -- For open seating, travel_class_id is NULL
        -- For assigned seating, randomly assign a class
        IF is_open THEN
            INSERT INTO seat (schedule_id, seat_number, is_sold, created_at, updated_at)
            VALUES (schedule_id, seat_num, is_sold_flag, NOW(), NOW());
ELSE
            -- Randomly select a travel class
            class_idx := 1 + floor(random() * array_length(travel_classes, 1))::INT;

INSERT INTO seat (schedule_id, seat_number, is_sold, travel_class_id, created_at, updated_at)
VALUES (schedule_id, seat_num, is_sold_flag, travel_classes[class_idx], NOW(), NOW());
END IF;
END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Now generate seats for schedules with about 65% sold for some routes
-- Parklane Travels (Douala-Yaounde) - 65% sold
SELECT generate_seats_for_schedule(
               id,
               bus_id,
               total_seats,
               65 -- 65% sold
       ) FROM schedule
WHERE agency_id = (SELECT id FROM agency WHERE name = 'Parklane Travels')
  AND from_location = (SELECT id FROM location WHERE name = 'Douala')
  AND to_location = (SELECT id FROM location WHERE name = 'Yaounde');

-- Touristique Express (Douala-Yaounde) - 65% sold
SELECT generate_seats_for_schedule(
               id,
               bus_id,
               total_seats,
               65 -- 65% sold
       ) FROM schedule
WHERE agency_id = (SELECT id FROM agency WHERE name = 'Touristique Express')
  AND from_location = (SELECT id FROM location WHERE name = 'Douala')
  AND to_location = (SELECT id FROM location WHERE name = 'Yaounde');

-- Global Express (Douala-Yaounde) - 30% sold (not sold out)
SELECT generate_seats_for_schedule(
               id,
               bus_id,
               total_seats,
               30 -- 30% sold
       ) FROM schedule
WHERE agency_id = (SELECT id FROM agency WHERE name = 'Global Express')
  AND from_location = (SELECT id FROM location WHERE name = 'Douala')
  AND to_location = (SELECT id FROM location WHERE name = 'Yaounde');

-- Men Travel (Douala-Yaounde) - 90% sold (almost sold out)
SELECT generate_seats_for_schedule(
               id,
               bus_id,
               total_seats,
               90 -- 90% sold
       ) FROM schedule
WHERE agency_id = (SELECT id FROM agency WHERE name = 'Men Travel')
  AND from_location = (SELECT id FROM location WHERE name = 'Douala')
  AND to_location = (SELECT id FROM location WHERE name = 'Yaounde');

-- Generate seats for all other schedules with random availability
SELECT generate_seats_for_schedule(id, bus_id, total_seats)
FROM schedule
WHERE id NOT IN (
    SELECT id FROM schedule
    WHERE (agency_id = (SELECT id FROM agency WHERE name = 'Parklane Travels') AND
           from_location = (SELECT id FROM location WHERE name = 'Douala') AND
           to_location = (SELECT id FROM location WHERE name = 'Yaounde'))
       OR (agency_id = (SELECT id FROM agency WHERE name = 'Touristique Express') AND
           from_location = (SELECT id FROM location WHERE name = 'Douala') AND
           to_location = (SELECT id FROM location WHERE name = 'Yaounde'))
       OR (agency_id = (SELECT id FROM agency WHERE name = 'Global Express') AND
           from_location = (SELECT id FROM location WHERE name = 'Douala') AND
           to_location = (SELECT id FROM location WHERE name = 'Yaounde'))
       OR (agency_id = (SELECT id FROM agency WHERE name = 'Men Travel') AND
           from_location = (SELECT id FROM location WHERE name = 'Douala') AND
           to_location = (SELECT id FROM location WHERE name = 'Yaounde'))
);