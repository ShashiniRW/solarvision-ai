package com.solarvision.backend.user;

import com.solarvision.backend.user.dto.AccountResponse;
import com.solarvision.backend.user.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Every signed-in user may read their own account and change their own password. */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public AccountResponse getAccount(Authentication authentication) {
        return accountService.getAccount(authentication.getName());
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               Authentication authentication) {
        accountService.changePassword(authentication.getName(), request);
    }
}
