package com.travelplatform.auth.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.enums.Role;

@Repository
public interface UserAdminRepository extends JpaRepository<UserAdmin, UUID> {

	UserAdmin findByEmail(String email);

	/** Used to gate admin self-registration — see UserAdminController#registerAdmin. */
	boolean existsByRole(Role role);

	/**
	 * SpEL enum reference avoids a plain string literal that would break
	 * if the enum name changes and works correctly with @Enumerated(STRING).
	 */
	@Query("SELECT u FROM UserAdmin u WHERE u.role = :#{T(com.travelplatform.auth.enums.Role).ROLE_USER}")
	List<UserAdmin> findAllUsers();

	@Transactional
	int deleteUserByEmail(String eMail);
}
