package com.solarvision.backend.user;

import com.solarvision.backend.common.exception.InvalidCredentialsException;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Sign-in and "who am I". Accounts are no longer created here: public sign-up is closed,
 * an administrator creates accounts on the Users page (see UserAdminService).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // A real hash of a throw-away password. Used so that a login attempt with an unknown
    // email takes as long as one with a known email (no timing clue about which emails exist).
    private final String dummyPasswordHash;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-password");
    }

    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Same amount of work as for a real user, then the same generic failure
            passwordEncoder.matches(rawPassword, dummyPasswordHash);
            throw new InvalidCredentialsException();
        }

        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        boolean accountActive = "ACTIVE".equals(user.getStatus());

        if (!passwordMatches || !accountActive) {
            throw new InvalidCredentialsException();
        }

        return jwtUtil.generateToken(user.getEmail());
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
