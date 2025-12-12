package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.User;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository {

    /**
     * Retrieve all users in the system.
     */
    List<User> findAllUsers();

    /**
     * Find a specific user by ID.
     */
    Optional<User> findUserById(Long id);

    /**
     * Update a user’s status to ACTIVE, DEACTIVATED, or BANNED.
     * @return true if update succeeded, false otherwise.
     */
    boolean updateUserStatus(Long userId, String status);

    List<User> findUsersByStatus(String status);

}
