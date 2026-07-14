package com.travelplatform.busbooking.dto;

import java.util.UUID;

import java.util.List;

import com.travelplatform.busbooking.entity.UserAdmin;

public class UserAdminResponse {
	private String jwt;

	/** Refresh token UUID issued alongside the access token (jwt). */
	private java.util.UUID refreshToken;

	private String message;

	private boolean success;

	private UserAdmin userAdmin;
	
	private List<UserAdmin> users;

	public UserAdminResponse() {

	}
	
	public UserAdminResponse(String jwt, String message, boolean success, UserAdmin userAdmin) {
		super();
		this.jwt = jwt;
		this.message = message;
		this.success = success;
		this.userAdmin = userAdmin;
	}



	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public UserAdmin getUserAdmin() {
		return userAdmin;
	}

	public void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}

	public String getJwt() {
		return jwt;
	}

	public void setJwt(String jwt) {
		this.jwt = jwt;
	}

	public java.util.UUID getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(java.util.UUID refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<UserAdmin> getUsers() {
		return users;
	}

	public void setUsers(List<UserAdmin> users) {
		this.users = users;
	}
	

}