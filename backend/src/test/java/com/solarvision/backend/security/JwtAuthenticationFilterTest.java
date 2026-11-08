package com.solarvision.backend.security;

import com.solarvision.backend.user.Role;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "some.jwt.token";
    private static final String EMAIL = "engineer@example.com";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }

    private User user(String status, String... roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            Role role = mock(Role.class);
            when(role.getName()).thenReturn(name);
            roles.add(role);
        }
        User user = new User();
        user.setEmail(EMAIL);
        user.setStatus(status);
        user.setRoles(roles);
        return user;
    }

    private void givenValidTokenFor(User user) {
        when(jwtUtil.validateToken(TOKEN)).thenReturn(true);
        when(jwtUtil.getEmailFromToken(TOKEN)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    // ---------- tests ----------

    @Test
    void activeUser_withValidToken_isAuthenticated_withRoles() throws Exception {
        givenValidTokenFor(user("ACTIVE", "ENGINEER"));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), chain);

        assertThat(currentAuthentication()).isNotNull();
        assertThat(currentAuthentication().getName()).isEqualTo(EMAIL);
        assertThat(currentAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ENGINEER");
        assertThat(chain.getRequest()).isNotNull(); // the request continued
    }

    @Test
    void deactivatedUser_withValidToken_isNotAuthenticated() throws Exception {
        givenValidTokenFor(user("INACTIVE"));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), chain);

        assertThat(currentAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull(); // continues, but anonymous -> 401 later
    }

    @Test
    void invalidToken_isNotAuthenticated_andDatabaseIsNotQueried() throws Exception {
        when(jwtUtil.validateToken(TOKEN)).thenReturn(false);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(currentAuthentication()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void noAuthorizationHeader_isNotAuthenticated() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(currentAuthentication()).isNull();
        verifyNoInteractions(jwtUtil, userRepository);
    }
}