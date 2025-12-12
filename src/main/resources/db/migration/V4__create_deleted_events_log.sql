CREATE TABLE IF NOT EXISTS Deleted_Events_Log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL,
    admin_id INT NOT NULL,
    reason TEXT,
    deleted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES Events(event_id),
    FOREIGN KEY (admin_id) REFERENCES Users(user_id)
);
