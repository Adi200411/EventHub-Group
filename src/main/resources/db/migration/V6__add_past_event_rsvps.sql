-- V6: Add RSVP records for past events to support "Past Events You Attended" functionality
-- This ensures users who attended past events have corresponding RSVP records

-- Add RSVP records for John Doe (user_id = 1) for events he attended
INSERT INTO RSVP (event_id, user_id, rsvp_date, qr_code) VALUES
(3, 1, '2025-09-10 10:30:00', UUID()), -- John RSVP'd to AI Symposium (attended on 2025-09-15)
(4, 1, '2025-08-15 14:20:00', UUID()), -- John RSVP'd to Coding Bootcamp (attended on 2025-08-20)
(7, 1, '2025-09-01 16:45:00', UUID()); -- John RSVP'd to Jazz Night (attended on 2025-09-05)

-- Add RSVP records for Jane Smith (user_id = 2) for events she attended  
INSERT INTO RSVP (event_id, user_id, rsvp_date, qr_code) VALUES
(6, 2, '2025-08-05 12:15:00', UUID()), -- Jane RSVP'd to Music Festival (attended on 2025-08-10)
(8, 2, '2025-08-25 09:30:00', UUID()), -- Jane RSVP'd to Art Exhibition (attended on 2025-08-30)
(3, 2, '2025-09-12 11:45:00', UUID()); -- Jane RSVP'd to AI Symposium (attended on 2025-09-15)

-- Add some additional RSVP records for users who RSVP'd but maybe didn't attend (for more realistic data)
-- These users RSVP'd to past events but may not have attendance records
INSERT INTO RSVP (event_id, user_id, rsvp_date, qr_code) VALUES
(5, 1, '2025-07-20 13:00:00', UUID()), -- John RSVP'd to Tech Career Fair
(5, 2, '2025-07-22 10:15:00', UUID()), -- Jane RSVP'd to Tech Career Fair
(9, 1, '2025-09-18 15:30:00', UUID()), -- John RSVP'd to Digital Art Workshop
(4, 2, '2025-08-18 17:45:00', UUID()); -- Jane RSVP'd to Coding Bootcamp