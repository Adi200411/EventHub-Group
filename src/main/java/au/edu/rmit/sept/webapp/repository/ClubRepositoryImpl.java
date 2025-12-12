package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.Clubs;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ClubRepositoryImpl implements ClubRepository {

    private final DataSource dataSource;

    public ClubRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Clubs>getAllClubs(){
        List<Clubs> clubs = new ArrayList<>();
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement stm = connection.prepareStatement("SELECT * FROM Clubs;");
                ResultSet rs = stm.executeQuery()
        ) {
            while (rs.next()) {
                LocalDate createdAt = rs.getDate("created_at").toLocalDate();

                Clubs c = new Clubs(
                        rs.getInt("club_id"),
                        rs.getInt("create_by"),
                        rs.getString("name"),
                        rs.getString("description"),
                        createdAt
                );
                clubs.add(c);
            }
            return clubs;
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Error in Club list", e);
        }
    }

}
