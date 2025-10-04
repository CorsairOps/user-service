package com.corsairops.corsairopsuserservice.controller;

import com.corsairops.corsairopsuserservice.service.UserService;
import com.corsairops.shared.dto.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/ids")
    public List<User> getUsersByIds(@RequestParam String ids) {
        Set<String> idSet = Set.of(ids.split(","));
        return userService.getUsersByIds(idSet);
    }
}