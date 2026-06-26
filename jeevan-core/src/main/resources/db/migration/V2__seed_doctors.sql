-- Seed: 22 doctors across 10 specialties + recurring weekly availability.
-- Doctor ids are explicit (seed-only table; no runtime inserts), so availability
-- can reference them deterministically. Several doctors have a mid-day break,
-- expressed as two windows on the same weekday.

INSERT INTO doctors (id, name, specialty, bio) VALUES
    (1,  'Dr. Asha Mehta',      'Cardiology',       'Interventional cardiologist with a focus on preventive heart care.'),
    (2,  'Dr. Rohan Verma',     'Cardiology',       'Specialises in arrhythmia management and cardiac imaging.'),
    (3,  'Dr. Priya Nair',      'Dermatology',      'Treats chronic skin conditions and cosmetic dermatology.'),
    (4,  'Dr. Karan Malhotra',  'Dermatology',      'Focus on paediatric dermatology and allergy testing.'),
    (5,  'Dr. Sneha Iyer',      'Pediatrics',       'General paediatrics and childhood immunisation.'),
    (6,  'Dr. Arjun Rao',       'Pediatrics',       'Neonatal and early-childhood developmental care.'),
    (7,  'Dr. Neha Gupta',      'Orthopedics',      'Sports injuries and joint-replacement surgery.'),
    (8,  'Dr. Vikram Singh',    'Orthopedics',      'Spine care and trauma rehabilitation.'),
    (9,  'Dr. Anjali Desai',    'Neurology',        'Headache, epilepsy and movement-disorder clinics.'),
    (10, 'Dr. Sameer Khan',     'Neurology',        'Stroke care and neuromuscular disorders.'),
    (11, 'Dr. Pooja Reddy',     'General Medicine', 'Primary care, diabetes and hypertension management.'),
    (12, 'Dr. Aditya Joshi',    'General Medicine', 'Internal medicine with a focus on preventive screening.'),
    (13, 'Dr. Meera Pillai',    'ENT',              'Ear, nose and throat surgery and hearing care.'),
    (14, 'Dr. Rahul Chopra',    'ENT',              'Sinus and voice disorders.'),
    (15, 'Dr. Divya Menon',     'Gynecology',       'Womens health, prenatal and fertility care.'),
    (16, 'Dr. Sunita Rao',      'Gynecology',       'High-risk pregnancy and laparoscopic surgery.'),
    (17, 'Dr. Nikhil Bose',     'Psychiatry',       'Anxiety, depression and cognitive behavioural therapy.'),
    (18, 'Dr. Tara Krishnan',   'Psychiatry',       'Adolescent mental health and counselling.'),
    (19, 'Dr. Manish Agarwal',  'Ophthalmology',    'Cataract surgery and retinal care.'),
    (20, 'Dr. Kavya Shetty',    'Ophthalmology',    'Glaucoma management and paediatric eye care.'),
    (21, 'Dr. Imran Sheikh',    'General Medicine', 'Family medicine and weekend acute-care clinics.'),
    (22, 'Dr. Ritu Saxena',     'Cardiology',       'Heart-failure clinic and post-operative follow-up.');

INSERT INTO doctor_availability (doctor_id, day_of_week, start_time, end_time) VALUES
    -- 1 Cardiology
    (1, 'MONDAY',    '17:00', '21:00'),
    (1, 'WEDNESDAY', '17:00', '21:00'),
    (1, 'FRIDAY',    '09:00', '12:00'),
    -- 2 Cardiology (break on Tuesday)
    (2, 'TUESDAY',   '09:00', '13:00'),
    (2, 'TUESDAY',   '17:00', '20:00'),
    (2, 'THURSDAY',  '17:00', '21:00'),
    -- 3 Dermatology (break on Wednesday)
    (3, 'MONDAY',    '10:00', '13:00'),
    (3, 'WEDNESDAY', '10:00', '13:00'),
    (3, 'WEDNESDAY', '14:00', '17:00'),
    (3, 'FRIDAY',    '10:00', '13:00'),
    -- 4 Dermatology
    (4, 'TUESDAY',   '16:00', '20:00'),
    (4, 'THURSDAY',  '16:00', '20:00'),
    (4, 'SATURDAY',  '10:00', '14:00'),
    -- 5 Pediatrics (mornings, Mon-Fri)
    (5, 'MONDAY',    '09:00', '12:00'),
    (5, 'TUESDAY',   '09:00', '12:00'),
    (5, 'WEDNESDAY', '09:00', '12:00'),
    (5, 'THURSDAY',  '09:00', '12:00'),
    (5, 'FRIDAY',    '09:00', '12:00'),
    -- 6 Pediatrics
    (6, 'MONDAY',    '15:00', '19:00'),
    (6, 'WEDNESDAY', '15:00', '19:00'),
    (6, 'SATURDAY',  '09:00', '13:00'),
    -- 7 Orthopedics
    (7, 'TUESDAY',   '17:00', '21:00'),
    (7, 'THURSDAY',  '17:00', '21:00'),
    (7, 'SATURDAY',  '16:00', '20:00'),
    -- 8 Orthopedics (break on Monday)
    (8, 'MONDAY',    '09:00', '13:00'),
    (8, 'MONDAY',    '14:00', '17:00'),
    (8, 'WEDNESDAY', '09:00', '13:00'),
    -- 9 Neurology
    (9, 'WEDNESDAY', '18:00', '21:00'),
    (9, 'FRIDAY',    '18:00', '21:00'),
    (9, 'SUNDAY',    '10:00', '13:00'),
    -- 10 Neurology (break on Thursday)
    (10, 'MONDAY',   '11:00', '14:00'),
    (10, 'THURSDAY', '11:00', '14:00'),
    (10, 'THURSDAY', '16:00', '19:00'),
    -- 11 General Medicine (mornings, Mon-Fri)
    (11, 'MONDAY',    '09:00', '13:00'),
    (11, 'TUESDAY',   '09:00', '13:00'),
    (11, 'WEDNESDAY', '09:00', '13:00'),
    (11, 'THURSDAY',  '09:00', '13:00'),
    (11, 'FRIDAY',    '09:00', '13:00'),
    -- 12 General Medicine (evenings)
    (12, 'MONDAY',    '17:00', '21:00'),
    (12, 'TUESDAY',   '17:00', '21:00'),
    (12, 'WEDNESDAY', '17:00', '21:00'),
    -- 13 ENT (break on Thursday)
    (13, 'TUESDAY',   '10:00', '13:00'),
    (13, 'THURSDAY',  '10:00', '13:00'),
    (13, 'THURSDAY',  '15:00', '18:00'),
    -- 14 ENT
    (14, 'MONDAY',    '16:00', '20:00'),
    (14, 'FRIDAY',    '16:00', '20:00'),
    (14, 'SATURDAY',  '11:00', '15:00'),
    -- 15 Gynecology (break on Friday)
    (15, 'WEDNESDAY', '09:00', '12:00'),
    (15, 'FRIDAY',    '09:00', '12:00'),
    (15, 'FRIDAY',    '17:00', '20:00'),
    -- 16 Gynecology
    (16, 'MONDAY',    '10:00', '14:00'),
    (16, 'THURSDAY',  '10:00', '14:00'),
    (16, 'SATURDAY',  '10:00', '14:00'),
    -- 17 Psychiatry
    (17, 'TUESDAY',   '18:00', '21:00'),
    (17, 'THURSDAY',  '18:00', '21:00'),
    (17, 'SUNDAY',    '16:00', '19:00'),
    -- 18 Psychiatry
    (18, 'MONDAY',    '13:00', '17:00'),
    (18, 'WEDNESDAY', '13:00', '17:00'),
    (18, 'FRIDAY',    '13:00', '17:00'),
    -- 19 Ophthalmology
    (19, 'MONDAY',    '09:00', '12:00'),
    (19, 'TUESDAY',   '09:00', '12:00'),
    (19, 'WEDNESDAY', '14:00', '18:00'),
    -- 20 Ophthalmology
    (20, 'THURSDAY',  '16:00', '20:00'),
    (20, 'FRIDAY',    '16:00', '20:00'),
    (20, 'SATURDAY',  '09:00', '13:00'),
    -- 21 General Medicine (weekend; break on Saturday)
    (21, 'SATURDAY',  '09:00', '13:00'),
    (21, 'SATURDAY',  '14:00', '18:00'),
    (21, 'SUNDAY',    '09:00', '13:00'),
    -- 22 Cardiology (evenings)
    (22, 'MONDAY',    '18:00', '21:00'),
    (22, 'WEDNESDAY', '18:00', '21:00'),
    (22, 'FRIDAY',    '18:00', '21:00');
