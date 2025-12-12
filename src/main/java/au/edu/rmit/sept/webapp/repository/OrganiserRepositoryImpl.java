package au.edu.rmit.sept.webapp.repository;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class OrganiserRepositoryImpl implements OrganiserRepository {

    private final DataSource dataSource;

    public OrganiserRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void saveOrganiser(int id, int club_id, String role){
        String insertSql = "INSERT INTO Organiser_Profile (user_id, club_id, role_title) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(insertSql) )
        {
            stm.setInt(1, id);
            stm.setInt(2, club_id);
            stm.setString(3, role);
            stm.executeUpdate();

        }catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error creating organiser profile", e);

        }
    }

    @Override
    public void deleteOrganiserData(int id) {
        String deleteSql = "DELETE FROM Organiser_Profile WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stm = connection.prepareStatement(deleteSql)) {
            stm.setInt(1, id);
            int rowsDeleted = stm.executeUpdate();
            if (rowsDeleted == 1) {
                System.out.println("Deleted " + rowsDeleted + " profile");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
