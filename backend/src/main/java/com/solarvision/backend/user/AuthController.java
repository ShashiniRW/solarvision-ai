package com.solarvision.backend.user;

import com.solarvision.backend.user.dto.RegisterRequest;
import com.solarvision.backend.user.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User savedUser = userService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new RegisterResponse(
                        savedUser.getId(),
                        savedUser.getFirstName(),
                        savedUser.getLastName(),
                        savedUser.getEmail(),
                        savedUser.getStatus()
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.getEmail(), request.getPassword());

        return ResponseEntity.ok(new LoginResponse(token));
    }

    private record LoginResponse(String token) {}

    private record RegisterResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            String status
    ) {}
}