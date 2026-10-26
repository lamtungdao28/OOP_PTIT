package com.example.oopPTIT.dto.response;

import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;

import java.util.List;

public record CurrentUserResponse(Long userId, String username, String email, List<Role> roles, Status status) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getEmail(),
                List.of(user.getRole()), user.getStatus());
    }
}
