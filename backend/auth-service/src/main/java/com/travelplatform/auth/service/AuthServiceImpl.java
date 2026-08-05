package com.travelplatform.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.travelplatform.auth.service.CustomDetailsImpl;
import com.travelplatform.auth.config.JwtProvider;
import com.travelplatform.auth.dto.LoginRequest;
import com.travelplatform.auth.dto.RefreshTokenRequest;
import com.travelplatform.auth.dto.RegisterRequest;
import com.travelplatform.auth.dto.TokenRefreshResponse;
import com.travelplatform.auth.entity.RefreshToken;
import com.travelplatform.auth.entity.UserAdmin;
import com.travelplatform.auth.enums.Role;
import com.travelplatform.auth.exception.EmailAlreadyExistsException;
import com.travelplatform.auth.exception.TokenRefreshException;
import com.travelplatform.auth.repository.UserAdminRepository;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserAdminRepository userAdminRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomDetailsImpl customDetailsImpl;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    // ─── Register ────────────────────────────────────────────────────────────

    @Override
    public AuthResult register(RegisterRequest request, Role role) {

        if (userAdminRepo.findByEmail(request.getEmail()) != null) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        UserAdmin user = new UserAdmin();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setAge(request.getAge());
        user.setDob(request.getDob());
        user.setRole(role);

        UserAdmin saved = userAdminRepo.save(user);

        Authentication auth = buildAuthentication(saved.getEmail());
        SecurityContextHolder.getContext().setAuthentication(auth);

        String accessToken = jwtProvider.generateToken(auth);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(saved);

        return new AuthResult(accessToken, refreshToken.getToken(), saved);
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    @Override
    public AuthResult login(LoginRequest request) {

        Authentication auth = authenticate(request.getEmail(), request.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);

        String accessToken = jwtProvider.generateToken(auth);

        UserAdmin user = userAdminRepo.findByEmail(request.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResult(accessToken, refreshToken.getToken(), user);
    }

    // ─── Refresh ─────────────────────────────────────────────────────────────

    @Override
    public TokenRefreshResponse refresh(RefreshTokenRequest request) {

        RefreshToken existing = refreshTokenService.verifyValid(request.getRefreshToken());

        UserAdmin user = existing.getUser();
        Authentication auth = buildAuthentication(user.getEmail());

        String newAccessToken = jwtProvider.generateToken(auth);

        // Rotate: revoke the used token, issue a fresh one.
        refreshTokenService.revoke(existing);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new TokenRefreshResponse(
                true, newAccessToken, newRefreshToken.getToken(), "Token refreshed successfully");
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.findByToken(request.getRefreshToken())
                .ifPresentOrElse(
                        refreshTokenService::revoke,
                        () -> { throw new TokenRefreshException("Refresh token not found."); });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Verifies email exists and password matches, then returns a fully-populated
     * Authentication object with the user's granted authorities.
     */
    private Authentication authenticate(String email, String rawPassword) {
        UserDetails userDetails = customDetailsImpl.loadUserByUsername(email);

        if (userDetails == null) {
            throw new BadCredentialsException("No account found for: " + email);
        }
        if (!passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("Incorrect password for: " + email);
        }

        return new UsernamePasswordAuthenticationToken(
                userDetails.getUsername(), null, userDetails.getAuthorities());
    }

    /** Loads UserDetails and returns an authenticated token — no password needed (post-register). */
    private Authentication buildAuthentication(String email) {
        UserDetails userDetails = customDetailsImpl.loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(
                userDetails.getUsername(), null, userDetails.getAuthorities());
    }
}
