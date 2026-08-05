package com.travelplatform.auth.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.travelplatform.auth.entity.UserAdmin;


public class CustomUserAdminDetails implements UserDetails {


	private UserAdmin userAdmin;

	public CustomUserAdminDetails(UserAdmin userAdmin) {
		super();
		this.userAdmin = userAdmin;
	}

	public UserAdmin getUserAdmin() {
		return userAdmin;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> authorities = new ArrayList<>();
		if (userAdmin.getRole() != null) {
			authorities.add(new SimpleGrantedAuthority(userAdmin.getRole().name()));
		}
		return authorities;
	}

	@Override
	public String getPassword() {
		return userAdmin.getPassword();
	}

	@Override
	public String getUsername() {
		return userAdmin.getEmail();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
