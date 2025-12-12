-- USERS & ROLES
DROP TABLE IF EXISTS User_Roles, Roles, Student_Profile, Clubs, Organiser_Profile, Events, Keywords, Event_Keywords,
Attendance, Event_Feedback, Event_Photos, Badges, Student_Badges, RSVP, Notifications, Tags, Event_Tags, Users;

CREATE TABLE Users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Roles (
  role_id INT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE User_Roles (
  user_id INT,
  role_id INT,
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES Users(user_id),
  FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);

CREATE TABLE Student_Profile (
  user_id INT PRIMARY KEY,
  name VARCHAR(255),
  course VARCHAR(255),
  interest VARCHAR(255),
  FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE Clubs (
  club_id INT AUTO_INCREMENT PRIMARY KEY,
  create_by INT,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (create_by) REFERENCES Users(user_id)
);

CREATE TABLE Organiser_Profile (
  organiser_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  club_id INT NOT NULL,
  role_title VARCHAR(255),
  FOREIGN KEY (user_id) REFERENCES Users(user_id),
  FOREIGN KEY (club_id) REFERENCES Clubs(club_id)
);

-- EVENTS
CREATE TABLE Events (
  event_id INT AUTO_INCREMENT PRIMARY KEY,
  organiser_id INT,
  club_id INT,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  location VARCHAR(255),
  date DATE,
  start_time DATETIME,
  finish_time DATETIME,
  status VARCHAR(100),
  capacity INT,
  FOREIGN KEY (organiser_id) REFERENCES Organiser_Profile(organiser_id),
  FOREIGN KEY (club_id) REFERENCES Clubs(club_id)
);

-- KEYWORDS & TAGS
CREATE TABLE Keywords (
  keyword_id INT AUTO_INCREMENT PRIMARY KEY,
  keyword VARCHAR(100) UNIQUE
);

CREATE TABLE Event_Keywords (
  event_id INT,
  keyword_id INT,
  PRIMARY KEY (event_id, keyword_id),
  FOREIGN KEY (event_id) REFERENCES Events(event_id),
  FOREIGN KEY (keyword_id) REFERENCES Keywords(keyword_id)
);

CREATE TABLE Tags (
  tag_id INT AUTO_INCREMENT PRIMARY KEY,
  tag_name VARCHAR(100) UNIQUE
);

CREATE TABLE Event_Tags (
  event_id INT,
  tag_id INT,
  PRIMARY KEY (event_id, tag_id),
  FOREIGN KEY (event_id) REFERENCES Events(event_id),
  FOREIGN KEY (tag_id) REFERENCES Tags(tag_id)
);

-- ATTENDANCE & RSVP
CREATE TABLE Attendance (
  event_id INT,
  user_id INT,
  checkin_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (event_id, user_id),
  FOREIGN KEY (event_id) REFERENCES Events(event_id),
  FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE RSVP (
  event_id INT,
  user_id INT,
  rsvp_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  qr_code CHAR(36) UNIQUE, -- UUID
  PRIMARY KEY (event_id, user_id),
  FOREIGN KEY (event_id) REFERENCES Events(event_id),
  FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- FEEDBACK
CREATE TABLE Event_Feedback (
  feedback_id INT AUTO_INCREMENT PRIMARY KEY,
  event_id INT,
  user_id INT,
  rating INT CHECK (rating BETWEEN 1 AND 5),
  comments TEXT,
  submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (event_id, user_id) REFERENCES Attendance(event_id, user_id)
);

-- PHOTOS
CREATE TABLE Event_Photos (
  photo_id INT AUTO_INCREMENT PRIMARY KEY,
  event_id INT,
  organiser_id INT,
  url VARCHAR(500),
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (event_id) REFERENCES Events(event_id),
  FOREIGN KEY (organiser_id) REFERENCES Organiser_Profile(organiser_id)
);

-- BADGES
CREATE TABLE Badges (
  badge_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) UNIQUE,
  description TEXT,
  criteria TEXT
);

CREATE TABLE Student_Badges (
  user_id INT,
  badge_id INT,
  earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, badge_id),
  FOREIGN KEY (user_id) REFERENCES Student_Profile(user_id),
  FOREIGN KEY (badge_id) REFERENCES Badges(badge_id)
);

-- NOTIFICATIONS
CREATE TABLE Notifications (
  notification_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  type VARCHAR(100),
  title VARCHAR(255),
  message TEXT,
  link VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  read_at TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES Users(user_id)
);




/*Indexes for Event Management */

/* Core event timelines & titles */
CREATE INDEX idx_Events_date        ON Events (date);
CREATE INDEX idx_Events_start_time  ON Events (start_time);
CREATE INDEX idx_Events_finish_time ON Events (finish_time);
CREATE INDEX idx_Events_title       ON Events (title);

/* Foreign-key lookup helpers */
CREATE INDEX idx_Events_organiser_id ON Events (organiser_id);
CREATE INDEX idx_Events_club_id      ON Events (club_id);

CREATE INDEX idx_orgprofile_user_id  ON Organiser_Profile (user_id);
CREATE INDEX idx_orgprofile_club_id  ON Organiser_Profile (club_id);

/* Tag/keyword filtering (common join patterns) */
CREATE INDEX idx_event_tags_tag_id      ON Event_Tags (tag_id);
CREATE INDEX idx_event_tags_event_id    ON Event_Tags (event_id);
CREATE INDEX idx_event_keywords_kw_id   ON Event_Keywords (keyword_id);
CREATE INDEX idx_event_keywords_event   ON Event_Keywords (event_id);

/* Attendance / RSVP (already have PKs, but FK lookups benefit too) */
CREATE INDEX idx_attendance_event_id ON Attendance (event_id);
CREATE INDEX idx_attendance_user_id  ON Attendance (user_id);

CREATE INDEX idx_rsvp_event_id ON RSVP (event_id);
CREATE INDEX idx_rsvp_user_id  ON RSVP (user_id);

/* Photos / Feedback / Badges / Notifications */
CREATE INDEX idx_photos_event_id     ON Event_Photos (event_id);
CREATE INDEX idx_photos_organiser_id ON Event_Photos (organiser_id);

CREATE INDEX idx_feedback_event_user ON Event_Feedback (event_id, user_id);
CREATE INDEX idx_badges_user_id      ON Student_Badges (user_id);
CREATE INDEX idx_badges_badge_id     ON Student_Badges (badge_id);

CREATE INDEX idx_notifications_user  ON Notifications (user_id);



-- -- 1) Insert dummy users
-- INSERT INTO Users (user_id, email, password_hash) VALUES
-- (1, 'techorg@example.com', 'x'),
-- (2, 'musicorg@example.com', 'x'),
-- (3, 'artorg@example.com', 'x'),
-- (4, 'teststudent@example.com', 'x')
-- ON DUPLICATE KEY UPDATE email = VALUES(email);

-- -- 2) Insert dummy clubs
-- INSERT INTO Clubs (club_id, name, description) VALUES
-- (1, 'Tech Club', 'Technology and coding events'),
-- (2, 'Music Club', 'Live music and performances'),
-- (3, 'Art Club', 'Student art exhibitions')
-- ON DUPLICATE KEY UPDATE name = VALUES(name);

-- -- 3) Insert organisers (linking users + clubs)
-- INSERT INTO Organiser_Profile (organiser_id, user_id, club_id, role_title) VALUES
-- (1, 1, 1, 'Tech Organiser'),
-- (2, 2, 2, 'Music Organiser'),
-- (3, 3, 3, 'Art Organiser')
-- ON DUPLICATE KEY UPDATE role_title = VALUES(role_title);

-- -- 4) Insert events (now organiser_id & club_id exist!)
-- INSERT INTO Events (event_id, organiser_id, club_id, title, description, location, date, start_time, finish_time, status, capacity) VALUES
-- (10, 1, 1, 'Tech Talk: AI in 2025', 'Explore the latest trends in AI.', 'City Hall Auditorium',
--  '2025-10-01', '2025-10-01 18:00:00', '2025-10-01 20:00:00', 'upcoming', 200),
-- (11, 2, 2, 'Music Night', 'Live performances from local bands.', 'Campus Lawn',
--  '2025-10-05', '2025-10-05 19:00:00', '2025-10-05 22:00:00', 'upcoming', 300),
-- (12, 3, 3, 'Art Exhibition', 'Discover student artwork.', 'Gallery Room',
--  '2025-10-07', '2025-10-07 10:00:00', '2025-10-07 17:00:00', 'upcoming', 150),
-- (13, 1, 1, 'Coding Bootcamp', 'Hands-on workshop for beginners.', 'Library Lab',
--  '2025-10-10', '2025-10-10 09:00:00', '2025-10-10 16:00:00', 'upcoming', 100)
-- ON DUPLICATE KEY UPDATE title = VALUES(title);

-- -- Past Events
-- INSERT INTO Events (event_id, organiser_id, club_id, title, description, location, date, start_time, finish_time, status, capacity) VALUES
--                                                                                                                                         (1, 1, 1, 'SEPTEMBER 2023 Mid-Semester Party', 'A social event to celebrate the middle of the semester.', 'RMIT Building 14', '2023-09-15', '2023-09-15 17:00:00', '2023-09-15 22:00:00', 'past', 150),
--                                                                                                                                         (2, 1, 1, 'Advanced Java Workshop', 'A workshop on advanced Java programming concepts.', 'RMIT Building 80', '2023-10-20', '2023-10-20 10:00:00', '2023-10-20 13:00:00', 'past', 50),
--                                                                                                                                         (3, 1, 1, 'Career Fair 2023', 'Meet with top employers from various industries.', 'RMIT Storey Hall', '2023-08-10', '2023-08-10 11:00:00', '2023-08-10 16:00:00', 'past', 500)
--     ON DUPLICATE KEY UPDATE title = VALUES(title);

-- -- 5) Insert tags
-- INSERT INTO Tags (tag_id, tag_name) VALUES
-- (1, 'AI'),
-- (2, 'Music'),
-- (3, 'Art'),
-- (4, 'Workshop')
-- ON DUPLICATE KEY UPDATE tag_name = VALUES(tag_name);

-- -- 6) Map events to tags
-- INSERT IGNORE INTO Event_Tags (event_id, tag_id) VALUES
-- (10, 1),  -- AI Talk
-- (11, 2),  -- Music Night
-- (12, 3),  -- Art Exhibition
-- (13, 1),  -- Coding Bootcamp → AI
-- (13, 4);  -- Coding Bootcamp → Workshop

-- -- 7) Student RSVPs to AI talk
-- INSERT INTO RSVP (event_id, user_id, rsvp_date, qr_code)
-- VALUES (10, 4, NOW(), UUID())
-- ON DUPLICATE KEY UPDATE rsvp_date = VALUES(rsvp_date);
