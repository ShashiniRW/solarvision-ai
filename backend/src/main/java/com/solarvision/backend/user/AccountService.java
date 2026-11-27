package com.solarvision.backend.user;

import com.solarvision.backend.common.exception.ConflictException;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.user.dto.AccountResponse;
import com.solarvision.backend.user.dto.ChangePasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The signed-in user's own account: who am I, and change my password. */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String email) {
        return AccountResponse.from(findByEmail(email));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);

        // the current password must be known, so a forgotten open laptop is not enough
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ConflictException("Your current password is not correct");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new ConflictException("Choose a new password that is different from the current one");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
