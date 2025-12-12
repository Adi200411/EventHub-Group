package au.edu.rmit.sept.webapp.service;

import java.util.List;
import java.util.Optional;

import au.edu.rmit.sept.webapp.model.Organiser_Profile;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.model.Student_Profile;

public interface UserService {

    List<User> getAllUsers();

    Optional<User> getUserById(Long id);

    Optional<User> getUserByEmail(String email);

//    User createUser(User user);

    void deleteUser(Long id);

    User findByUsernameAndPassword(String username, String password);

    User register(String username, String password);

    void saveUser(int id, String name, String course, String interest);

    void updateUser(int id, String name, String course, String interest);

    void deleteProfile(int id);

    Optional<Organiser_Profile> findOrganiserById(int userId);

    // Update password (plaintext provided to service; service should hash)
    void updatePassword(int id, String newPassword);

    Optional<Student_Profile> getProfileByUserId(int id);

    void deactivateUser(int userId);

    void reactivateUser(int userId);

    void banUser(int userId);

    List<User> getUsersByStatus(String status);

    String getUserRoleById(int userId);

    Integer getOrganiserIdByUserId(int userId);

    void assignUserRole(int userId, int roleId);
}