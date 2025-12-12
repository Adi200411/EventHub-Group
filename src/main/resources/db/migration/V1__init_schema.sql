-- USERS & ROLES
-- DROP TABLE IF EXISTS User_Roles, Roles, Student_Profile, Clubs, Organiser_Profile, Events, Keywords, Event_Keywords,
-- Attendance, Event_Feedback, Event_Photos, Badges, Student_Badges, RSVP, Notifications, Tags, Event_Tags, Users;

CREATE TABLE IF NOT EXISTS Users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Roles (
  role_id INT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS User_Roles (
  user_id INT,
  role_id INT,
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES Users(user_id),
  FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);

CREATE TABLE IF NOT EXISTS Student_Profile (
  user_id INT PRIMARY KEY,
  name VARCHAR(255),
  course VARCHAR(255),
  interest VARCHAR(255),
  FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Clubs (
  club_id INT AUTO_INCREMENT PRIMARY KEY,
  create_by INT,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (create_by) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Organiser_Profile (
  organiser_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  club_id INT NOT NULL,
  role_title VARCHAR(255),
  FOREIGN KEY (user_id) REFERENCES Users(user_id),
  FOREIGN KEY (club_id) REFERENCES Clubs(club_id)
);

-- EVENTS
CREATE TABLE IF NOT EXISTS Events (
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
CREATE TABLE IF NOT EXISTS Keywords (
  keyword_id INT AUTO_INCREMENT PRIMARY KEY,
  keyword VARCHAR(100) UNIQUE
);

CREATE TABLE IF NOT EXISTS Event_Keywords (
  event_id INT,
  keyword_id INT,
  PRIMARY KEY (event_id, keyword_id),
  FOREIGN KEY (event_id) REFERENCES Events(event_id),
  FOREIGN KEY (keyword_id) REFERENCES Keywords(keyword_id)
);

CREATE TABLE IF NOT EXISTS Tags (
  tag_id INT AUTO_INCREMENT PRIMARY KEY,
  tag_name VARCHAR(100) UNIQUE
);

CREATE TABLE IF NOT EXISTS Event_Tags (
  event_id INT,
  tag_id INT,
  PRIMARY KEY (event_id, tag_id),
  FOREIGN KEY (event_id) REFERENCES Events(event_id),
  FOREIGN KEY (tag_id) REFERENCES Tags(tag_id)
);

-- ATTENDANCE & RSVP
CREATE TABLE IF NOT EXISTS Attendance (
  event_id INT,
  user_id INT,
  checkin_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (event_id, user_id),
  FOREIGN KEY (event_id) REFERENCES Events(event_id),
  FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS RSVP (
  event_id INT,
  user_id INT,
  rsvp_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  qr_code CHAR(36) UNIQUE, -- UUID
  PRIMARY KEY (event_id, user_id),
  FOREIGN KEY (event_id) REFERENCES Events(event_id),
  FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- FEEDBACK
CREATE TABLE IF NOT EXISTS Event_Feedback (
  feedback_id INT AUTO_INCREMENT PRIMARY KEY,
  event_id INT,
  user_id INT,
  rating INT CHECK (rating BETWEEN 1 AND 5),
  comments TEXT,
  submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (event_id, user_id) REFERENCES Attendance(event_id, user_id)
);

-- PHOTOS
CREATE TABLE IF NOT EXISTS Event_Photos (
  photo_id INT AUTO_INCREMENT PRIMARY KEY,
  event_id INT,
  organiser_id INT,
  url VARCHAR(500),
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (event_id) REFERENCES Events(event_id),
  FOREIGN KEY (organiser_id) REFERENCES Organiser_Profile(organiser_id)
);

-- BADGES
CREATE TABLE IF NOT EXISTS Badges (
  badge_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) UNIQUE,
  description TEXT,
  criteria TEXT
);

CREATE TABLE IF NOT EXISTS Student_Badges (
  user_id INT,
  badge_id INT,
  earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, badge_id),
  FOREIGN KEY (user_id) REFERENCES Student_Profile(user_id),
  FOREIGN KEY (badge_id) REFERENCES Badges(badge_id)
);

-- NOTIFICATIONS
CREATE TABLE IF NOT EXISTS Notifications (
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
