package com.example.oopPTIT.security;

import com.example.oopPTIT.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) {
        String identifier = login.strip();
        var matches = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier);
        // Existing usernames may collide with another account's email: never choose arbitrarily.
        if (matches.size() != 1) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return new AuthenticatedUser(matches.getFirst());
    }
}
