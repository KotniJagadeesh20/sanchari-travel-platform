package com.travelplatform.busbooking.service;

import com.travelplatform.busbooking.entity.UserAdmin;
import com.travelplatform.busbooking.repository.UserAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class UserAdminServiceimpl implements UserAdminService {

    @Autowired
    private UserAdminRepository userRepo;

    /**
     * Returns an existing user_ref record or creates a new one from the
     * X-Authenticated-Email header forwarded by the API Gateway.
     * This ensures bookings always have a valid FK without requiring a
     * cross-service HTTP call to auth-service.
     */
    @Override
    @Transactional
    public UserAdmin findOrCreate(UUID id, String email) {
        return userRepo.findById(id).orElseGet(() -> {
            UserAdmin ref = new UserAdmin(id, email);
            return userRepo.save(ref);
        });
    }
}
