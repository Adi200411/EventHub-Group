package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.model.User;

import java.util.List;
import java.util.Optional;

public interface AdminUserService {

    /**
     * Get all registered users for admin view.
     */
    List<User> getAllUsers();

    /**
     * Get a single user by their ID.
     */
    Optional<User> getUserById(Long id);

    /**
     * Deactivate a user account.
     * Changes user status to "DEACTIVATED".
     */
    boolean deactivateUser(Long userId);

    /**
     * Reactivate a previously deactivated account.
     * Changes user status to "ACTIVE".
     */
    boolean reactivateUser(Long userId);

    /**
     * Ban a user account for violations.
     * Changes user status to "BANNED".
     */
    boolean banUser(Long userId);

    List<User> getUsersByStatus(String status);
}
