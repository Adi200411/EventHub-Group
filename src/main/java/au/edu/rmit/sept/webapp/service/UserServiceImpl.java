package au.edu.rmit.sept.webapp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import au.edu.rmit.sept.webapp.model.Organiser_Profile;
import au.edu.rmit.sept.webapp.model.Student_Profile;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.OrganiserRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrganiserRepository organiserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, OrganiserRepository organiserRepository) {
        this.userRepository = userRepository;
        this.organiserRepository = organiserRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
//
//    @Override
//    public User createUser(User user) {
//        return userRepository.save(user);
//    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public User findByUsernameAndPassword(String username, String password) {
        // Fetch user by email and verify password against stored hash
        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String storedHash = user.password_hash();
            if (storedHash != null && passwordEncoder.matches(password, storedHash)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public User register(String username, String password) {
        // Hash the password before saving
        String hashed = passwordEncoder.encode(password);
        User newUser = userRepository.register(username, hashed);
        
        // Assign default STUDENT role (role_id = 1) to new user
        if (newUser != null) {
            userRepository.assignUserRole(newUser.user_id(), 1); // 1 = STUDENT role
        }
        
        return newUser;
    }

    @Override
    public void saveUser(int id, String name, String course, String interest) {
        userRepository.saveUserData(id, name, course, interest);
    }

    @Override
    public void updateUser(int id, String name, String course, String interest){
        userRepository.updateUserData(id, name, course, interest);
    }

    @Override
    public void deleteProfile(int id) {
        organiserRepository.deleteOrganiserData(id);
        userRepository.deleteProfileData(id);
        userRepository.deleteUser(id);
    }

    @Override
    public Optional<Organiser_Profile> findOrganiserById(int userId) {
        return userRepository.getOrganiserProfileByUserId(userId);
    }
    public void updatePassword(int id, String newPassword) {
        // Hash plaintext password and delegate to repository
        String hashed = passwordEncoder.encode(newPassword);
        userRepository.updatePassword(id, hashed);
    }

    @Override
    public Optional<Student_Profile> getProfileByUserId(int id) {
        return userRepository.findProfileByUserId(id);
    }

        /**
     * Deactivate a user account (set status = 'DEACTIVATED')
     */
    @Override
    public void deactivateUser(int userId) {
        userRepository.updateUserStatus(userId, "DEACTIVATED");
    }

    /**
     * Reactivate a user account (set status = 'ACTIVE')
     */
    @Override
    public void reactivateUser(int userId) {
        userRepository.updateUserStatus(userId, "ACTIVE");
    }

    /**
     * Ban a user account (set status = 'BANNED')
     */
    @Override
    public void banUser(int userId) {
        userRepository.updateUserStatus(userId, "BANNED");
    }

    /**
     * Retrieve all users by status (e.g., ACTIVE, DEACTIVATED, BANNED)
     */
    @Override
    public List<User> getUsersByStatus(String status) {
        return userRepository.findByStatus(status);
    }

    @Override
    public String getUserRoleById(int userId) {
        return userRepository.getUserRoleById(userId)
            .orElse("STUDENT"); // default if not found
}

    @Override
    public Integer getOrganiserIdByUserId(int userId) {
        return userRepository.getOrganiserIdByUserId(userId)
            .orElse(null);
}

    @Override
    public void assignUserRole(int userId, int roleId) {
        userRepository.assignUserRole(userId, roleId);
    }


}