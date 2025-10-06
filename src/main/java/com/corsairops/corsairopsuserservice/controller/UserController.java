package com.corsairops.corsairopsuserservice.controller;

import com.corsairops.corsairopsuserservice.service.UserService;
import com.corsairops.shared.annotations.CommonReadResponses;
import com.corsairops.shared.dto.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.OK;

@Tag(name = "User Management", description = "APIs for managing users")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "Get all users")
    @CommonReadResponses
    @GetMapping
    @ResponseStatus(OK)
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "Get users by a list of IDs")
    @CommonReadResponses
    @GetMapping("/ids")
    @ResponseStatus(OK)
    public List<User> getUsersByIds(@RequestParam String ids) {
        Set<String> idSet = Set.of(ids.split(","));
        return userService.getUsersByIds(idSet);
    }

    @Operation(summary = "Get a user by ID")
    @CommonReadResponses
    @GetMapping("/{id}")
    @ResponseStatus(OK)
    public User getUserById(@PathVariable String id) {
        return userService.getUserById(id);
    }
}