package au.edu.rmit.sept.webapp.repository;

import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;

import au.edu.rmit.sept.webapp.model.User;

@SpringBootTest
public class UserRepositoryImplTest {

    UserRepository repo;

    @Autowired
    DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    public void setUp() {
        flyway.migrate();
        repo = new UserRepositoryImpl(dataSource);
    }
    
    @AfterEach
    public void tearDown() {
        flyway.clean();
    }

    @Test
    void findByUsernameAndPassword_should_findUserWithValidCredentials() {
        String email = "john.doe@student.rmit.edu.au";      //find user 1
        String password = "$2a$10$example.hash.for.password123";    //example valid password for user 1
        Optional<User> user = repo.findByUsernameAndPassword(email, password);
        assertNotNull(user);
    }

    @Test
    void findByUsernameAndPassword_shouldNot_findUserWithInvalidPassword() {
        String email = "john.doe@student.rmit.edu.au";      //find user 1
        String password = "$2a$10$example.hash.for.password";    //invalid password for user 1
        Optional<User> user = repo.findByUsernameAndPassword(email, password);
        assertEquals(Optional.empty(), user);
    }

    @Test
    void findByUsernameAndPassword_shouldNot_findUserWithInvalidEmail() {
        String email = "john.doe@rmit.edu.au";      //typo in user 1 email
        String password = "$2a$10$example.hash.for.password123";    //valid password for user 1
        Optional<User> user = repo.findByUsernameAndPassword(email, password);
        assertEquals(Optional.empty(), user);
    }

    @Test
    void register_should_registerUser() {
        String insertEmail = "test.example@student.rmit.edu.au";
        String insertPassword = "hash.for.test.password12345";
        User test_user = repo.register(insertEmail, insertPassword);
        assertNotNull(test_user);
    }

    @Test
    void register_shouldNot_registerUser_UserExists() {
        String existing_email = "john.doe@student.rmit.edu.au";      //find user 1
        String password = "$2a$10$example.hash.for.password123";    //example valid password for user 1
        User test_user = repo.register(existing_email, password);
        assertNull(test_user);
    }

    @Test
    void saveUserData_should_saveUserData() {
        String userToSaveEmail = "test.example@student.rmit.edu.au";
        String userToSavePassword = "hash.for.test.password12345";
        User test_user = repo.register(userToSaveEmail, userToSavePassword);

        String userToSaveName = "Test Name";
        String userToSaveCourse = "Course 1";
        String userToSaveInterest = "Interest 1";

        assertDoesNotThrow(() ->
                repo.saveUserData(test_user.user_id(), userToSaveName, userToSaveCourse, userToSaveInterest)
        );
    }

    @Test
    void saveUserData_shouldNot_saveUserData_UserNotExist() {
        String userToNotSaveName = "Test Name";
        String userToNotSaveCourse = "Course 1";
        String userToNotSaveInterest = "Interest 1";
        DataAccessResourceFailureException exception = assertThrows(DataAccessResourceFailureException.class, () ->
                repo.saveUserData(0, userToNotSaveName, userToNotSaveCourse, userToNotSaveInterest)
        );
        assertNotNull(exception);
    }

    @Test
    void findAll_should_returnAllUsers() {
        List<User> users = repo.findAll();
        assertNotNull(users);
        assertEquals(4, users.size());  //3 users expected in migration when setting up database
        assertNotEquals(users.get(0), users.get(1)); //unique users in users list
    }

    @Test
    void updateUserData_should_updateUserData() {
        String userToSaveEmail = "test.example@student.rmit.edu.au";
        String userToSavePassword = "hash.for.test.password12345";
        User test_user = repo.register(userToSaveEmail, userToSavePassword);
        String userToSaveName = "Test Name";
        String userToSaveCourse = "Course 1";
        String userToSaveInterest = "Interest 1";
        repo.saveUserData(test_user.user_id(), userToSaveName, userToSaveCourse, userToSaveInterest);

        String userToUpdateName = "New Test Name";
        String userToUpdateCourse = "Course 2";
        String userToUpdateInterest = "Interest 2";

        assertDoesNotThrow(() ->
                repo.updateUserData(test_user.user_id(), userToUpdateName, userToUpdateCourse, userToUpdateInterest)
        );
    }
    
    @Test
    void updateUserData_shouldNot_updateUserData_invalidId() {
        String userToSaveEmail = "test.example@student.rmit.edu.au";
        String userToSavePassword = "hash.for.test.password12345";
        User test_user = repo.register(userToSaveEmail, userToSavePassword);
        String userToSaveName = "Test Name";
        String userToSaveCourse = "Course 1";
        String userToSaveInterest = "Interest 1";
        repo.saveUserData(test_user.user_id(), userToSaveName, userToSaveCourse, userToSaveInterest);

        String userToUpdateName = "New Test Name";
        String userToUpdateCourse = "Course 2";
        String userToUpdateInterest = "Interest 2";

        DataAccessResourceFailureException exception = assertThrows(DataAccessResourceFailureException.class, () ->
                repo.updateUserData(0, userToUpdateName, userToUpdateCourse, userToUpdateInterest) //user id 0 does not exist, not update
        );
        assertNotNull(exception);
    }

    @Test
    void deleteProfileData_should_deleteProfileData() {
        String userToSaveEmail = "test.example@student.rmit.edu.au";
        String userToSavePassword = "hash.for.test.password12345";
        User test_user = repo.register(userToSaveEmail, userToSavePassword);

        assertEquals(5, repo.findAll().size()); //user added

        repo.deleteProfileData(test_user.user_id());
    }

    @Test
    void deleteUserData_should_deleteUserData() {
        String userToSaveEmail = "test.example@student.rmit.edu.au";
        String userToSavePassword = "hash.for.test.password12345";
        User test_user = repo.register(userToSaveEmail, userToSavePassword);

        assertEquals(5, repo.findAll().size()); //user added

        repo.deleteUser(test_user.user_id());
    }
}
