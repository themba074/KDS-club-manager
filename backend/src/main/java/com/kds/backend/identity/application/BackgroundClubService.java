package com.kds.backend.identity.application;

import com.kds.backend.identity.repository.ClubAccessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Public Identity boundary for background jobs that subsequently establish TenantContext per club. */
@Service @Transactional(readOnly=true)
public class BackgroundClubService {
    private final ClubAccessRepository clubs;
    public BackgroundClubService(ClubAccessRepository clubs){this.clubs=clubs;}
    public List<UUID> allClubIds(){return clubs.allClubIds();}
}
