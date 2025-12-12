package au.edu.rmit.sept.webapp.repository;

import java.util.List;
import java.util.Optional;

import au.edu.rmit.sept.webapp.model.Organiser_Profile;
import au.edu.rmit.sept.webapp.model.Student_Profile;
import au.edu.rmit.sept.webapp.model.User;

public interface UserRepository {
    
    List<User> findAll();
    
    Optional<User> findById(Long id);
    
    Optional<User> findByEmail(String email);
    
    //User save(User user);
    
    void deleteById(Long id);

    Optional<User> findByUsernameAndPassword(String email, String password);

    User register(String email, String password);

    void saveUserData(int id, String name, String course, String interest);

    void updateUserData(int id, String name, String course, String interest);

    Optional<Student_Profile> findProfileByUserId(int id);

    void deleteProfileData(int id);

    void deleteUser(int id);

    Optional<Organiser_Profile> getOrganiserProfileByUserId(int userId);
    // Update the stored password hash for the user
    void updatePassword(int id, String newHashedPassword);

    void updateUserStatus(int userId, String status);
    
    List<User> findByStatus(String status);

    Optional<String> getUserRoleById(int userId);

    Optional<Integer> getOrganiserIdByUserId(int userId);

    boolean isOrganiserForEvent(int userId, int eventId);

    void assignUserRole(int userId, int roleId);

}