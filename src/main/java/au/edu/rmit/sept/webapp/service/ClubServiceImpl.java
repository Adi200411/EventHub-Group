package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.model.Clubs;
import au.edu.rmit.sept.webapp.repository.ClubRepositoryImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClubServiceImpl implements ClubService {
    private final ClubRepositoryImpl clubRepository;

    public ClubServiceImpl(ClubRepositoryImpl repository) {this.clubRepository = repository;}

    public List<Clubs> getAllClubs(){
        return clubRepository.getAllClubs();
    }
}
