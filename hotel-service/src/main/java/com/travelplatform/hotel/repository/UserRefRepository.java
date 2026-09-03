package com.travelplatform.hotel.repository;

import com.travelplatform.hotel.entity.UserRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRefRepository extends JpaRepository<UserRef, UUID> {
}
