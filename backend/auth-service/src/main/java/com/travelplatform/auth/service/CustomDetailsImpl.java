package com.travelplatform.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.repository.UserAdminRepository;

@Service
public class CustomDetailsImpl implements UserDetailsService{
	
	@Autowired(required=true)
	private UserAdminRepository userRepo;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		UserAdmin useradmin=userRepo.findByEmail(username);
		if(useradmin==null) {
			throw new UsernameNotFoundException("User 404");
		}
		
		return new CustomUserAdminDetails(useradmin);
	}

}
