package com.travelplatform.busbooking.repository;

import com.travelplatform.busbooking.entity.UserAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface UserAdminRepository extends JpaRepository<UserAdmin, UUID> {
    UserAdmin findByEmail(String email);
}
