package com.travelplatform.hotel.service;

import com.travelplatform.hotel.entity.UserRef;
import java.util.UUID;

public interface UserRefService {
    /** Find or create a user_ref record from JWT claims forwarded by the gateway. */
    UserRef findOrCreate(UUID id, String email, String name);
}
