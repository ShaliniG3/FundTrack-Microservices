package com.cts.fundtrack.identity.security;

import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom implementation of Spring Security's {@link UserDetailsService} that
 * loads user authentication data from the Identity Service's own database.
 *
 * <p>Spring Security's {@link org.springframework.security.authentication.AuthenticationManager}
 * delegates to this service during the username-and-password authentication flow
 * (i.e., at login time) to retrieve the stored credentials and authorities for a
 * given principal name (email address).</p>
 *
 * <p>The returned {@link UserDetails} object is built using Spring Security's
 * fluent builder. The {@code roles()} method automatically prepends the
 * {@code ROLE_} prefix, so passing {@code "ADMIN"} results in the authority
 * {@code "ROLE_ADMIN"} — consistent with the authorities declared on
 * {@link User#getAuthorities()}.</p>
 *
 * <p>This bean is picked up by
 * {@link com.cts.fundtrack.identity.config.SecurityConfig} when constructing the
 * {@link org.springframework.security.authentication.AuthenticationManager}.</p>
 *
 * @see UserDetailsService
 * @see UserRepository
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Locates the user by their email address and maps the entity to a
     * Spring Security {@link UserDetails} object.
     *
     * <p>Steps performed:</p>
     * <ol>
     *   <li>Query {@link UserRepository} for a {@link User} whose email matches
     *       the supplied {@code email} parameter.</li>
     *   <li>Throw {@link UsernameNotFoundException} if no match is found, which
     *       Spring Security translates to an authentication failure.</li>
     *   <li>Build and return a {@link UserDetails} instance containing the email,
     *       BCrypt-encoded password, and role-derived authority.</li>
     * </ol>
     *
     * @param email the email address submitted by the user at login; used as the
     *              Spring Security principal name
     * @return a fully populated {@link UserDetails} instance ready for credential
     *         verification by the authentication manager
     * @throws UsernameNotFoundException if no user with the given email exists in
     *                                   the database
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Fetch user from the Identity database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 2. Map the entity to Spring Security's UserDetails.
        // Note: .roles() automatically adds the "ROLE_" prefix.
        // If your Enum is "USER", Spring sees it as "ROLE_USER".
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
