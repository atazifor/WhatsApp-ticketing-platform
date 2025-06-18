-- Insert agency contacts for all agencies in all cities with valid phone numbers and unique addresses
INSERT INTO agency_contact (id, agency_id, city, phone, address, created_at, updated_at)
SELECT gen_random_uuid(),
       a.id,
       l.name,
       -- Generate realistic Cameroonian mobile numbers (+237 6XX XXX XXX)
       '+2376' ||
           -- First 2 digits after 6: 50-99 (common prefixes)
       (50 + (row_number() over () % 50)) ||
           -- Remaining 6 digits: random but realistic combinations
       LPAD(FLOOR(RANDOM() * 10000)::text, 4, '0') ||
       LPAD(FLOOR(RANDOM() * 100)::text, 2, '0'),
       -- Generate unique addresses per location
       CASE
           WHEN l.name = 'Douala' THEN a.name || ' Head Office, ' || l.region || ' Region'
           WHEN l.name = 'Yaounde' THEN 'Central Station, ' || l.region || ' - ' || a.name || ' Desk'
           WHEN l.name = 'Buea' THEN l.name || ' Mountain Terminal, ' || a.name || ' Counter'
           WHEN l.name = 'Ngoundere' THEN a.name || ' Northern Hub, ' || l.region
           WHEN l.name = 'Bafoussam' THEN l.region || ' Regional Terminal, ' || a.name || ' Office'
           WHEN l.name = 'Kribi' THEN a.name || ' Coastal Station, Beach Road'
           WHEN l.name = 'Bamenda' THEN a.name || ' Northwest Terminal, ' || l.region
           END,
       NOW(),
       NOW()
FROM agency a
         CROSS JOIN
     location l
WHERE l.name IN ('Douala', 'Yaounde', 'Buea', 'Ngoundere', 'Bafoussam', 'Kribi', 'Bamenda');

--delete and insert My special Men Travel
DELETE FROM agency_contact
WHERE agency_id = (
    SELECT id FROM agency WHERE name = 'Men Travel'
)
  AND city = 'Douala';

-- Specific entry for Men Travel in Douala with memorable details
INSERT INTO agency_contact (id, agency_id, city, phone, address, created_at, updated_at)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        (SELECT id FROM agency WHERE name = 'Men Travel'),
        'Douala',
        '+237677889900',
        'Bonamoussadi Terminal, Opposite Total Station',
        NOW(),
        NOW()) ON CONFLICT DO NOTHING;