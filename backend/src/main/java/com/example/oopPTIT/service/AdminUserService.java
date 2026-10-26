package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.response.CurrentUserResponse;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ResponsePage<List<CurrentUserResponse>> getUsers(Pageable pageable) {
        Set<String> fields = Set.of("id", "username", "email", "role", "status", "createdAt");
        if (pageable.getSort().stream().anyMatch(order -> !fields.contains(order.getProperty()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported user sort field");
        }
        var page = userRepository.findAll(pageable).map(CurrentUserResponse::from);
        return ResponsePage.<List<CurrentUserResponse>>builder().pageNo(page.getNumber()).pageSize(page.getSize())
                .totalPage(page.getTotalPages()).items(page.getContent()).build();
    }
}
