-- Insert sample roles
INSERT INTO Roles (role_name) VALUES 
('STUDENT'), 
('ORGANISER'), 
('ADMIN');

-- Insert sample users
INSERT INTO Users (email, password_hash) VALUES 
('john.doe@student.rmit.edu.au', '$2a$10$VKduCBhZxZcO14I1CHGPx.NHRedikQS5p7xo4XeKNDzzvYRxFEixG'), -- password is password
('jane.smith@student.rmit.edu.au', '$2a$10$VKduCBhZxZcO14I1CHGPx.NHRedikQS5p7xo4XeKNDzzvYRxFEixG'), -- password is password
('organiser@rmit.edu.au', '$2a$10$VKduCBhZxZcO14I1CHGPx.NHRedikQS5p7xo4XeKNDzzvYRxFEixG'), -- password is password
('admin@eventhub.com', '$2a$10$GS/pViEOFlT0DEqrT77Wc.DorPvXFqaZAs667e7o0pmMWDy4Z/Aia'); -- admin password is: admin, stored hashed;

-- Insert user roles
INSERT INTO User_Roles (user_id, role_id) VALUES 
(1, 1), -- John as STUDENT
(2, 1), -- Jane as STUDENT  
(3, 2), -- Organiser as ORGANISER
(4, 3); -- Admin as ADMIN

-- Insert student profiles
INSERT INTO Student_Profile (user_id, name, course, interest) VALUES 
(1, 'John Doe', 'Bachelor of Computer Science', 'Programming, AI'),
(2, 'Jane Smith', 'Bachelor of Software Engineering', 'Web Development, UX'),
(3, 'Organiser', 'Bachelor of Software Engineering', 'Web Development, UX'),
(4, 'Admin', '', '');

-- Insert sample club
INSERT INTO Clubs (create_by, name, description) VALUES 
(3, 'Computer Science Society', 'A club for computer science students to network and learn together');

-- Insert organiser profile
INSERT INTO Organiser_Profile (user_id, club_id, role_title) VALUES 
(3, 1, 'President'),
(4, 1, 'Admin');

-- Insert sample events
INSERT INTO Events (organiser_id, club_id, title, description, location, date, start_time, finish_time, status, capacity) VALUES 
(1, 1, 'Programming Workshop', 'Learn the basics of Python programming', 'Building 80, Room 10.1', '2025-11-05', '2025-11-05 14:00:00', '2025-11-05 16:00:00', 'ACTIVE', 30),
(1, 1, 'Tech Talk: AI in Industry', 'Industry professionals discuss AI applications', 'Building 80, Room 5.15', '2025-11-12', '2025-11-12 18:00:00', '2025-11-12 19:30:00', 'ACTIVE', 50);

-- Insert sample tags
INSERT INTO Tags (tag_name) VALUES 
('Programming'), 
('AI'), 
('Workshop'), 
('Tech Talk');

-- Link events to tags
INSERT INTO Event_Tags (event_id, tag_id) VALUES 
(1, 1), -- Programming Workshop -> Programming
(1, 3), -- Programming Workshop -> Workshop
(2, 2), -- Tech Talk -> AI
(2, 4); -- Tech Talk -> Tech Talk