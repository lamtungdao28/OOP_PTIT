package com.example.oopPTIT.security;

import com.example.oopPTIT.entity.CourseClass;
import com.example.oopPTIT.repository.CourseClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component("courseAccess")
@RequiredArgsConstructor
public class CourseAccess {
    private final CourseClassRepository courseClassRepository;
    private final CurrentUser currentUser;

    public boolean canManage(Long classId) {
        return currentUser.isAdmin() || courseClassRepository.existsByIdAndLecturer_User_Id(classId, currentUser.id());
    }

    public void requireAssignedLecturer(CourseClass courseClass) {
        if (!currentUser.isAdmin() && !courseClass.getLecturer().getUser().getId().equals(currentUser.id())) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
