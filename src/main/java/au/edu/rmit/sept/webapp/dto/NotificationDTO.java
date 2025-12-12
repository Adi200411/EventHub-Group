package au.edu.rmit.sept.webapp.dto;
/**
 * Data Transfer Object for creating a new notification.
 * Created with assist of llm
 */
public class NotificationDTO {
    private int userId;
    private String type;
    private String title;
    private String message;
    private String link;

    public NotificationDTO() {}

    public NotificationDTO(int userId, String type, String title, String message, String link) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
