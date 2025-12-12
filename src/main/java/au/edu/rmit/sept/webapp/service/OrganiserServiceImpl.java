package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.repository.OrganiserRepository;
import org.springframework.stereotype.Service;

@Service
public class OrganiserServiceImpl implements OrganiserService {

    private final OrganiserRepository organiserRepository;
    public OrganiserServiceImpl(OrganiserRepository organiserRepository) {this.organiserRepository = organiserRepository;}

    @Override
    public void saveOrganiser(int id, int club_id, String role){
        organiserRepository.saveOrganiser(id, club_id, role);
    }

}
