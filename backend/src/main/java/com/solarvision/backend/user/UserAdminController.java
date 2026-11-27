package com.solarvision.backend.user;

import com.solarvision.backend.user.dto.CreateUserRequest;
import com.solarvision.backend.user.dto.TemporaryPasswordResponse;
import com.solarvision.backend.user.dto.UpdateUserRequest;
import com.solarvision.backend.user.dto.UserSummary;
import com.solarvision.backend.user.dto.UserWithTemporaryPassword;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** User management. ADMIN only: checked here AND in SecurityConfig (defense in depth). */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public List<UserSummary> listUsers() {
        return userAdminService.listUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserWithTemporaryPassword createUser(@Valid @RequestBody CreateUserRequest request,
                                                Authentication authentication) {
        return userAdminService.createUser(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public UserSummary updateUser(@PathVariable Long id,
                                  @Valid @RequestBody UpdateUserRequest request,
                                  Authentication authentication) {
        return userAdminService.updateUser(id, request, authentication.getName());
    }

    @PostMapping("/{id}/deactivate")
    public UserSummary deactivate(@PathVariable Long id, Authentication authentication) {
        return userAdminService.setActive(id, false, authentication.getName());
    }

    @PostMapping("/{id}/activate")
    public UserSummary activate(@PathVariable Long id, Authentication authentication) {
        return userAdminService.setActive(id, true, authentication.getName());
    }

    @PostMapping("/{id}/reset-password")
    public TemporaryPasswordResponse resetPassword(@PathVariable Long id) {
        return new TemporaryPasswordResponse(userAdminService.resetPassword(id));
    }
}
