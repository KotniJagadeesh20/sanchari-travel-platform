package com.travelplatform.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.travelplatform.auth.entity.RefreshToken;
import com.travelplatform.auth.entity.UserAdmin;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	Optional<RefreshToken> findByToken(UUID token);

	@Modifying
	@Transactional
	@Query("DELETE FROM RefreshToken r WHERE r.user = ?1")
	void deleteByUser(UserAdmin user);

	@Modifying
	@Transactional
	@Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = ?1")
	void revokeAllByUser(UserAdmin user);
}
