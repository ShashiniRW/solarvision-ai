package com.solarvision.backend.demo;

import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.security.JwtUtil;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Signs a visitor in as one of the demo accounts. Exists only when demo mode is on. */
@Service
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoLoginService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public String login(DemoRole role) {
        User user = userRepository.findByEmail(role.email())
            .filter(User::isActive) // a deactivated demo account cannot be used either
            .orElseThrow(() -> new ResourceNotFoundException("Demo user not found"));

        // the same kind of token a normal login creates
        return jwtUtil.generateToken(user.getEmail());
    }
}
