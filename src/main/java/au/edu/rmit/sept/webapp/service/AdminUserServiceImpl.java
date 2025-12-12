package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    private final AdminUserRepository adminUserRepository;

    public AdminUserServiceImpl(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public List<User> getAllUsers() {
        logger.info("Fetching all users for admin view");
        return adminUserRepository.findAllUsers();
    }

    @Override
    public Optional<User> getUserById(Long id) {
        logger.info("Fetching user by ID: {}", id);
        return adminUserRepository.findUserById(id);
    }

    @Override
    public boolean deactivateUser(Long userId) {
        logger.info("Deactivating user ID: {}", userId);
        return adminUserRepository.updateUserStatus(userId, "DEACTIVATED");
    }

    @Override
    public boolean reactivateUser(Long userId) {
        logger.info("Reactivating user ID: {}", userId);
        return adminUserRepository.updateUserStatus(userId, "ACTIVE");
    }

    @Override
    public boolean banUser(Long userId) {
        logger.info("Banning user ID: {}", userId);
        return adminUserRepository.updateUserStatus(userId, "BANNED");
    }

    @Override
    public List<User> getUsersByStatus(String status) {
        logger.info("Fetching users by status: {}", status);
        return adminUserRepository.findUsersByStatus(status);
}

}
