package com.solarvision.backend.demo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * One-click demo sign-in. When app.demo.enabled is not true, this controller does not exist,
 * so both endpoints answer 404 and the login page hides the demo buttons.
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoLoginController {

    private final DemoLoginService demoLoginService;

    /** Tells the login page that demo mode is on, and which roles can be tried. */
    @GetMapping("/demo")
    public DemoInfo demoInfo() {
        return new DemoInfo(true, List.of(DemoRole.values()));
    }

    /** Signs in as a demo account. An unknown role (or "ADMIN") is rejected with 400. */
    @PostMapping("/demo-login")
    public TokenResponse demoLogin(@Valid @RequestBody DemoLoginRequest request) {
        return new TokenResponse(demoLoginService.login(request.role()));
    }

    public record DemoLoginRequest(@NotNull DemoRole role) {}

    public record TokenResponse(String token) {}

    public record DemoInfo(boolean enabled, List<DemoRole> roles) {}
}
