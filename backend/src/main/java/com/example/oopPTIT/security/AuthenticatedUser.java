package com.example.oopPTIT.security;

import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.util.Status;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class AuthenticatedUser extends org.springframework.security.core.userdetails.User {
    private final Long userId;

    public AuthenticatedUser(User user) {
        super(user.getUsername(), user.getPassword(), user.getStatus() == Status.ACTIVE,
                true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        this.userId = user.getId();
    }

    public Long getUserId() {
        return userId;
    }
}
