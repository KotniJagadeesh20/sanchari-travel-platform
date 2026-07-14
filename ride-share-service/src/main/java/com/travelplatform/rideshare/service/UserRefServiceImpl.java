package com.travelplatform.rideshare.service;

import com.travelplatform.rideshare.entity.UserRef;
import com.travelplatform.rideshare.repository.UserRefRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class UserRefServiceImpl implements UserRefService {

    @Autowired
    private UserRefRepository userRefRepo;

    @Override
    @Transactional
    public UserRef findOrCreate(UUID id, String email, String name) {
        return userRefRepo.findById(id)
                .map(existing -> {
                    // Keep the cached display hints fresh in case the user changed them in auth-service.
                    boolean changed = false;
                    if (email != null && !email.equals(existing.getEmail())) { existing.setEmail(email); changed = true; }
                    if (name != null && !name.equals(existing.getName())) { existing.setName(name); changed = true; }
                    return changed ? userRefRepo.save(existing) : existing;
                })
                .orElseGet(() -> userRefRepo.save(new UserRef(id, email, name)));
    }
}
