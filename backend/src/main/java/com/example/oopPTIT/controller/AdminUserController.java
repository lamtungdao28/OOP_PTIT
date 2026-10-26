package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.response.CurrentUserResponse;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService userService;

    @GetMapping
    public ResponseData<ResponsePage<List<CurrentUserResponse>>> getUsers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new ResponseData<>(200, "Users retrieved successfully", userService.getUsers(pageable));
    }
}
