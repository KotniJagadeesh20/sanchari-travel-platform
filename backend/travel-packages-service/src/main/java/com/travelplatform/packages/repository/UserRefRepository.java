package com.travelplatform.packages.repository;

import com.travelplatform.packages.entity.UserRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface UserRefRepository extends JpaRepository<UserRef, UUID> {
    UserRef findByEmail(String email);
}
