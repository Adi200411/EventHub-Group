package au.edu.rmit.sept.webapp.repository;

public interface OrganiserRepository {

    void saveOrganiser(int id, int club_id, String role);

    void deleteOrganiserData(int id);
}
