class NotificationManager {
    constructor() {
        this.userId = null;
        this.notifications = [];
        this.unreadCount = 0;
        this.init();
    }

    init() {
        this.userId = this.getUserId();
        
        if (this.userId) {
            this.loadNotifications();
            setInterval(() => this.loadUnreadCount(), 30000);
        }
    }

    getUserId() {
        const userIdElement = document.querySelector('[data-user-id]');
        if (userIdElement) {
            return parseInt(userIdElement.getAttribute('data-user-id'));
        }
        return null;
    }

    async loadNotifications() {
        if (!this.userId) return;

        try {
            const response = await fetch(`/api/notifications/${this.userId}/unread`);
            if (response.ok) {
                this.notifications = await response.json();
                this.unreadCount = this.notifications.length;
                this.updateUI();
            }
        } catch (error) {
            console.error('Error loading notifications:', error);
        }
    }

    async loadUnreadCount() {
        if (!this.userId) return;

        try {
            const response = await fetch(`/api/notifications/${this.userId}/unread/count`);
            if (response.ok) {
                const data = await response.json();
                this.unreadCount = data.count;
                this.updateBadge();
            }
        } catch (error) {
            console.error('Error loading unread count:', error);
        }
    }

    async markAsRead(notificationId) {
        try {
            const response = await fetch(`/api/notifications/${notificationId}/read`, {
                method: 'PUT'
            });
            if (response.ok) {
                this.notifications = this.notifications.filter(n => n.notification_id !== notificationId);
                this.unreadCount = Math.max(0, this.unreadCount - 1);
                this.updateUI();
            }
        } catch (error) {
            console.error('Error marking notification as read:', error);
        }
    }

    async markAllAsRead() {
        if (!this.userId) return;

        try {
            const response = await fetch(`/api/notifications/${this.userId}/read-all`, {
                method: 'PUT'
            });
            if (response.ok) {
                this.notifications = [];
                this.unreadCount = 0;
                this.updateUI();
            }
        } catch (error) {
            console.error('Error marking all as read:', error);
        }
    }

    updateUI() {
        this.updateBadge();
        this.updateDropdown();
    }

    updateBadge() {
        const badge = document.getElementById('notification-badge');
        if (badge) {
            badge.textContent = this.unreadCount;
            badge.style.display = this.unreadCount > 0 ? 'flex' : 'none';
        }
    }

    updateDropdown() {
        const dropdown = document.getElementById('notification-dropdown');
        if (!dropdown) return;

        if (this.notifications.length === 0) {
            dropdown.innerHTML = `
                <div class="notification-empty">
                    <p>No new notifications</p>
                </div>
            `;
            return;
        }

        const notificationsHTML = this.notifications.map(notification => `
            <div class="notification-item ${notification.read_at ? 'read' : 'unread'}" 
                 data-notification-id="${notification.notification_id}">
                <div class="notification-content">
                    <div class="notification-title">${this.escapeHtml(notification.title)}</div>
                    <div class="notification-message">${this.escapeHtml(notification.message)}</div>
                    <div class="notification-time">${this.formatTime(notification.created_at)}</div>
                </div>
                <button class="notification-mark-read" 
                        onclick="notificationManager.markAsRead(${notification.notification_id})"
                        title="Mark as read">
                    ✓
                </button>
            </div>
        `).join('');

        dropdown.innerHTML = `
            <div class="notification-header">
                <span>Notifications</span>
                <button onclick="notificationManager.markAllAsRead()" class="mark-all-read">
                    Mark all as read
                </button>
            </div>
            ${notificationsHTML}
        `;
    }

    formatTime(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
        if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
        
        return date.toLocaleDateString();
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    toggleDropdown() {
        const dropdown = document.getElementById('notification-dropdown');
        if (dropdown) {
            const isVisible = dropdown.style.display === 'block';
            dropdown.style.display = isVisible ? 'none' : 'block';
            
            if (!isVisible) {
                this.loadNotifications();
            }
        }
    }
}

let notificationManager;
document.addEventListener('DOMContentLoaded', () => {
    notificationManager = new NotificationManager();
});

document.addEventListener('click', (event) => {
    const notificationPanel = document.getElementById('notification-panel');
    const dropdown = document.getElementById('notification-dropdown');
    
    if (notificationPanel && dropdown && 
        !notificationPanel.contains(event.target)) {
        dropdown.style.display = 'none';
    }
});
