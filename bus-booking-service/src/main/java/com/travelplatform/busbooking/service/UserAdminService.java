package com.travelplatform.busbooking.service;

import com.travelplatform.busbooking.entity.UserAdmin;
import java.util.UUID;

/**
 * Thin user-reference service for bus-booking-service.
 * User account management (register/delete/edit) belongs to auth-service.
 * This service only resolves/creates the lightweight user_ref record
 * needed for booking FK relationships.
 */
public interface UserAdminService {

    /** Find or create a user_ref record from JWT claims forwarded by the gateway. */
    UserAdmin findOrCreate(UUID id, String email);
}
