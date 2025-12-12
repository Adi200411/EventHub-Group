-- V3: Add Previous Events and Sample Photo Data for Gallery Testing
-- This migration adds past events with photos to test the gallery functionality

-- Insert additional clubs for variety
INSERT INTO Clubs (create_by, name, description) VALUES 
(3, 'Music Society', 'Bringing together music lovers for concerts and events'),
(3, 'Art & Design Club', 'Student community for creative arts and design projects');

-- Insert additional organiser profiles
INSERT INTO Organiser_Profile (user_id, club_id, role_title) VALUES 
(2, 2, 'Music Coordinator'),  -- Jane as music organiser
(1, 3, 'Art Director');       -- John as art organiser

-- Insert additional tags for events
INSERT INTO Tags (tag_name) VALUES 
('Academic'), 
('Social'), 
('Career'), 
('Music'), 
('Art'),
('Digital Art');

-- Insert PAST EVENTS (before today - October 3, 2025) for testing gallery
INSERT INTO Events (organiser_id, club_id, title, description, location, date, start_time, finish_time, status, capacity) VALUES 
-- Computer Science Society past events
(1, 1, 'AI & Machine Learning Symposium 2025', 'Comprehensive look at the latest developments in AI and ML with industry experts sharing insights on future trends.', 'RMIT Building 14, Lecture Theatre', '2025-09-15', '2025-09-15 09:00:00', '2025-09-15 17:00:00', 'past', 200),

(1, 1, 'Spring Coding Bootcamp', 'Intensive 3-day coding workshop covering modern web development, databases, and deployment strategies for beginners.', 'RMIT Building 80, Computer Labs', '2025-08-20', '2025-08-20 09:00:00', '2025-08-22 17:00:00', 'past', 50),

(1, 1, 'Tech Career Fair 2025', 'Meet with leading tech companies including Google, Microsoft, Atlassian and local startups. Network and find internship opportunities.', 'RMIT Storey Hall', '2025-07-25', '2025-07-25 10:00:00', '2025-07-25 16:00:00', 'past', 500),

-- Music Society past events  
(2, 2, 'Summer Music Festival', 'Outdoor music festival featuring student bands, local artists, and food trucks. A celebration of diverse musical talents.', 'RMIT Campus Lawn', '2025-08-10', '2025-08-10 14:00:00', '2025-08-10 22:00:00', 'past', 300),

(2, 2, 'Jazz Night at the Café', 'Intimate jazz performance in a cozy setting with local jazz musicians and student performers. Light refreshments provided.', 'RMIT Student Café', '2025-09-05', '2025-09-05 19:00:00', '2025-09-05 23:00:00', 'past', 80),

-- Art & Design Club past events
(3, 3, 'Student Art Exhibition 2025', 'Annual showcase of outstanding student artwork including paintings, sculptures, digital art, and mixed media installations.', 'RMIT Gallery Space', '2025-08-30', '2025-08-30 10:00:00', '2025-08-30 18:00:00', 'past', 150),

(3, 3, 'Creative Workshop: Digital Art', 'Hands-on workshop exploring digital art techniques using industry-standard software. All skill levels welcome.', 'RMIT Design Studios', '2025-09-20', '2025-09-20 13:00:00', '2025-09-20 17:00:00', 'past', 25);

-- Map past events to tags
INSERT INTO Event_Tags (event_id, tag_id) VALUES
-- AI Symposium (assuming event_id 3)
(3, 5), -- Academic
(3, 2), -- AI  
(3, 7), -- Career

-- Coding Bootcamp (assuming event_id 4)
(4, 5), -- Academic
(4, 3), -- Workshop

-- Tech Career Fair (assuming event_id 5)
(5, 7), -- Career
(5, 5), -- Academic

-- Music Festival (assuming event_id 6)
(6, 6), -- Social
(6, 8), -- Music

-- Jazz Night (assuming event_id 7)
(7, 6), -- Social
(7, 8), -- Music

-- Art Exhibition (assuming event_id 8)
(8, 5), -- Academic
(8, 9), -- Art

-- Digital Art Workshop (assuming event_id 9)
(9, 3), -- Workshop
(9, 9), -- Art
(9, 10); -- Digital Art

-- Sample Photo Data for Past Events
-- Using the provided sample image for all photo records
INSERT INTO Event_Photos (event_id, organiser_id, url, uploaded_at) VALUES
-- AI Symposium photos (event_id 3)
(3, 1, '/images/sample.jpg', '2025-09-15 10:30:00'),
(3, 1, '/images/sample.jpg', '2025-09-15 14:20:00'),
(3, 1, '/images/sample.jpg', '2025-09-15 16:45:00'),
(3, 1, '/images/sample.jpg', '2025-09-15 15:15:00'),

-- Coding Bootcamp photos (event_id 4)
(4, 1, '/images/sample.jpg', '2025-08-20 11:00:00'),
(4, 1, '/images/sample.jpg', '2025-08-21 09:30:00'),
(4, 1, '/images/sample.jpg', '2025-08-22 15:45:00'),

-- Tech Career Fair photos (event_id 5)
(5, 1, '/images/sample.jpg', '2025-07-25 11:30:00'),
(5, 1, '/images/sample.jpg', '2025-07-25 13:15:00'),
(5, 1, '/images/sample.jpg', '2025-07-25 14:45:00'),
(5, 1, '/images/sample.jpg', '2025-07-25 15:30:00'),

-- Music Festival photos (event_id 6)
(6, 2, '/images/sample.jpg', '2025-08-10 16:20:00'),
(6, 2, '/images/sample.jpg', '2025-08-10 19:45:00'),
(6, 2, '/images/sample.jpg', '2025-08-10 17:30:00'),
(6, 2, '/images/sample.jpg', '2025-08-10 20:15:00'),
(6, 2, '/images/sample.jpg', '2025-08-10 21:00:00'),

-- Jazz Night photos (event_id 7)  
(7, 2, '/images/sample.jpg', '2025-09-05 20:30:00'),
(7, 2, '/images/sample.jpg', '2025-09-05 21:15:00'),
(7, 2, '/images/sample.jpg', '2025-09-05 22:00:00'),

-- Art Exhibition photos (event_id 8)
(8, 3, '/images/sample.jpg', '2025-08-30 10:15:00'),
(8, 3, '/images/sample.jpg', '2025-08-30 12:30:00'),
(8, 3, '/images/sample.jpg', '2025-08-30 14:45:00'),
(8, 3, '/images/sample.jpg', '2025-08-30 16:20:00'),

-- Digital Art Workshop photos (event_id 9)
(9, 3, '/images/sample.jpg', '2025-09-20 13:30:00'),
(9, 3, '/images/sample.jpg', '2025-09-20 15:15:00'),
(9, 3, '/images/sample.jpg', '2025-09-20 16:45:00');

-- Sample attendance records for past events (students who attended)
INSERT INTO Attendance (event_id, user_id, checkin_time) VALUES
(3, 1, '2025-09-15 09:15:00'), -- John attended AI Symposium
(4, 1, '2025-08-20 09:05:00'), -- John attended Coding Bootcamp
(6, 2, '2025-08-10 14:30:00'), -- Jane attended Music Festival
(8, 2, '2025-08-30 10:30:00'), -- Jane attended Art Exhibition
(3, 2, '2025-09-15 09:20:00'), -- Jane also attended AI Symposium
(7, 1, '2025-09-05 19:15:00'); -- John attended Jazz Night