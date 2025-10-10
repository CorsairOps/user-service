package com.corsairops.corsairopsuserservice.service;

import com.corsairops.corsairopsuserservice.config.AuthServiceProperties;
import com.corsairops.shared.dto.User;
import com.corsairops.shared.exception.HttpResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthServiceProperties authServiceProperties;
    private final Keycloak keycloak;

    public List<User> getAllUsers() {
        try {
            RealmResource realmResource = keycloak.realm(authServiceProperties.getRealm());
            List<UserRepresentation> userRepresentations = realmResource.users().list();
            List<User> users = new ArrayList<>();
            for (UserRepresentation userRep : userRepresentations) {
                List<RoleRepresentation> roles = getUserRealmRoles(realmResource, userRep.getId());
                List<String> roleNames = extractRoleNames(roles);
                users.add(new User(
                        userRep.getId(),
                        userRep.getUsername(),
                        userRep.getEmail(),
                        userRep.getFirstName(),
                        userRep.getLastName(),
                        userRep.isEnabled(),
                        userRep.getCreatedTimestamp(),
                        roleNames
                ));
            }
            return users;
        } catch (Exception e) {
            log.error("Error fetching all users: {}", e.getMessage());
            throw new HttpResponseException("User Service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }


    public User getUserById(String userId) {
        try {
            UserRepresentation userRep = keycloak.realm(authServiceProperties.getRealm())
                    .users()
                    .get(userId)
                    .toRepresentation();

            if (userRep == null) {
                throw new HttpResponseException("User not found", HttpStatus.NOT_FOUND);
            }

            List<RoleRepresentation> roles = getUserRealmRoles(keycloak.realm(authServiceProperties.getRealm()), userId);
            List<String> roleNames = extractRoleNames(roles);
            return new User(
                    userRep.getId(),
                    userRep.getUsername(),
                    userRep.getEmail(),
                    userRep.getFirstName(),
                    userRep.getLastName(),
                    userRep.isEnabled(),
                    userRep.getCreatedTimestamp(),
                    roleNames
            );
        } catch (Exception e) {
            log.error("Error fetching user by ID: {}", e.getMessage());
            throw new HttpResponseException("User Service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public List<User> getUsersByIds(Set<String> userIds, boolean allowEmpty) {
        if (userIds == null || userIds.isEmpty()) {
            if (allowEmpty) {
                return List.of();
            } else {
                throw new HttpResponseException("No user IDs provided", HttpStatus.BAD_REQUEST);
            }
        }

        RealmResource realmResource = keycloak.realm(authServiceProperties.getRealm());
        return userIds.parallelStream()
                .map(id -> {
                    try {
                        UserRepresentation userRep = realmResource.users().get(id).toRepresentation();
                        if (userRep == null) {
                            if (allowEmpty) {
                                return null;
                            }
                            throw new HttpResponseException("User with ID " + id + " not found", HttpStatus.NOT_FOUND);
                        } else {
                            List<RoleRepresentation> roles = getUserRealmRoles(realmResource, id);
                            List<String> roleNames = extractRoleNames(roles);
                            return new User(
                                    userRep.getId(),
                                    userRep.getUsername(),
                                    userRep.getEmail(),
                                    userRep.getFirstName(),
                                    userRep.getLastName(),
                                    userRep.isEnabled(),
                                    userRep.getCreatedTimestamp(),
                                    roleNames
                            );
                        }
                    } catch (Exception e) {
                        if (allowEmpty) {
                            return null;
                        }
                        throw new HttpResponseException("User with ID " + id + " not found", HttpStatus.NOT_FOUND);
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> extractRoleNames(List<RoleRepresentation> realmRoles) {
        Set<String> validRoleNames = Set.of("ADMIN", "PLANNER", "OPERATOR", "TECHNICIAN", "ANALYST");
        return realmRoles.stream()
                .map(RoleRepresentation::getName)
                .filter(validRoleNames::contains)
                .toList();
    }

    private List<RoleRepresentation> getUserRealmRoles(RealmResource realmResource, String userId) {
        return realmResource
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .listAll();
    }

}