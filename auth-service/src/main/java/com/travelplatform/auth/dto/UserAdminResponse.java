package com.travelplatform.auth.dto;

import java.util.UUID;

import java.util.List;

public class UserAdminResponse {
	private String jwt;

	/** Refresh token UUID issued alongside the access token (jwt). */
	private java.util.UUID refreshToken;

	private String message;

	private boolean success;

	/** Never the raw entity — UserProfileResponse guarantees no password/credential fields leak into this response. */
	private UserProfileResponse userAdmin;
	
	private List<UserProfileResponse> users;

	public UserAdminResponse() {

	}
	
	public UserAdminResponse(String jwt, String message, boolean success, UserProfileResponse userAdmin) {
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

	public UserProfileResponse getUserAdmin() {
		return userAdmin;
	}

	public void setUserAdmin(UserProfileResponse userAdmin) {
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

	public List<UserProfileResponse> getUsers() {
		return users;
	}

	public void setUsers(List<UserProfileResponse> users) {
		this.users = users;
	}
	

}