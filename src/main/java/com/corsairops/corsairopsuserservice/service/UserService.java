package com.corsairops.corsairopsuserservice.service;

import com.corsairops.corsairopsuserservice.model.CachedUser;
import com.corsairops.corsairopsuserservice.repository.CachedUserRepository;
import com.corsairops.shared.dto.User;
import com.corsairops.shared.exception.HttpResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${cognito.user-pool-id}")
    private String userPoolId;

    private final CognitoIdentityProviderClient cognitoClient;
    private final CachedUserRepository cachedUserRepository;

    public List<User> getAllUsers() {
        try {
            // Check cache first
            long cacheCount = cachedUserRepository.count();
            int estimated = getUserPoolEstimatedCount();

            if (cacheCount < estimated) {
                log.info("Cache miss: fetching users from Cognito and updating Redis cache");
                // Cache is stale or empty, fetch from Cognito and update cache
                ListUsersResponse response = listAllUsers();

                List<User> users = response.users().stream()
                        .map(this::mapToUser)
                        .toList();

                cacheUsers(users);
                return users;
            } else {
                // Cache hit
                log.info("Cache hit: returning users from Redis cache");
                return getAllCachedUsers().stream()
                        .map(this::mapToUser)
                        .toList();
            }
        } catch (AwsServiceException e) {
            throw new HttpResponseException(e.awsErrorDetails().errorMessage(), HttpStatus.valueOf(e.statusCode()));
        } catch (SdkClientException e) {
            log.error(e.getMessage(), e);
            throw new HttpResponseException("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public List<User> getUsersByIds(Set<String> ids) {
        try {
            try {
                List<CachedUser> cachedUsers = getCachedUsersByIds(ids);
                Set<String> found = cachedUsers.stream().map(CachedUser::getId).collect(toSet());

                List<User> result = new ArrayList<>(cachedUsers.stream()
                        .map(this::mapToUser)
                        .toList());

                Set<String> missing = new HashSet<>(ids);
                missing.removeAll(found);

                if (!missing.isEmpty()) {
                    log.info("Fetching {} missing users from Cognito", missing.size());
                    ListUsersResponse response = listAllUsers();
                    List<User> fetchedUsers = response.users().stream()
                            .filter(userType -> missing.contains(getAttributeValue(userType, "sub")))
                            .map(this::mapToUser)
                            .toList();
                    cacheUsers(fetchedUsers);
                    result.addAll(fetchedUsers);
                }

                return result;
            } catch (Exception e) {
                log.error("Error accessing cache: {}", e.getMessage(), e);
            }
            ListUsersResponse response = listAllUsers();
            List<User> users = response.users().stream()
                    .filter(userType -> ids.contains(getAttributeValue(userType, "sub")))
                    .map(this::mapToUser)
                    .toList();
            cacheUsers(users);
            return users;
        } catch (AwsServiceException e) {
            throw new HttpResponseException(e.awsErrorDetails().errorMessage(), HttpStatus.valueOf(e.statusCode()));
        } catch (SdkClientException e) {
            log.error(e.getMessage(), e);
            throw new HttpResponseException("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public User getUserById(String id) {
        try {
            try {
                return getCachedUsersByIds(Set.of(id)).stream()
                        .findFirst()
                        .map(this::mapToUser)
                        .orElse(null);
            } catch (Exception e) {
                log.error("Error accessing cache: {}", e.getMessage(), e);
            }
            ListUsersResponse response = listUserById(id);
            List<User> users = response.users().stream()
                    .filter(userType -> id.equals(getAttributeValue(userType, "sub")))
                    .map(this::mapToUser)
                    .toList();
            if (!users.isEmpty()) {
                cacheUsers(users);
                return users.getFirst();
            }

            throw new HttpResponseException("User not found", HttpStatus.NOT_FOUND);
        } catch (AwsServiceException e) {
            throw new HttpResponseException(e.awsErrorDetails().errorMessage(), HttpStatus.valueOf(e.statusCode()));
        } catch (SdkClientException e) {
            log.error(e.getMessage(), e);
            throw new HttpResponseException("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private ListUsersResponse listUserById(String id) {
        ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                .userPoolId(userPoolId)
                .filter("sub=\"" + id + "\"")
                .attributesToGet("sub", "email", "given_name", "family_name", "gender")
                .build();

        return cognitoClient.listUsers(listUsersRequest);
    }

    private ListUsersResponse listAllUsers() throws AwsServiceException, SdkClientException {
        ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                .userPoolId(userPoolId)
                .attributesToGet("sub", "email", "given_name", "family_name", "gender")
                .build();

        return cognitoClient.listUsers(listUsersRequest);
    }

    private Integer getUserPoolEstimatedCount() {
        UserPoolType userPool = describeUserPool();
        return userPool.estimatedNumberOfUsers();
    }

    private UserPoolType describeUserPool() {
        try {
            DescribeUserPoolResponse response = cognitoClient.describeUserPool(DescribeUserPoolRequest.builder()
                    .userPoolId(userPoolId)
                    .build());

            return response.userPool();
        } catch (AwsServiceException e) {
            throw new HttpResponseException(e.awsErrorDetails().errorMessage(), HttpStatus.valueOf(e.statusCode()));
        } catch (SdkClientException e) {
            log.error(e.getMessage(), e);
            throw new HttpResponseException("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private User mapToUser(UserType userType) {
        String id = getAttributeValue(userType, "sub");
        String email = getAttributeValue(userType, "email");
        String givenName = getAttributeValue(userType, "given_name");
        String familyName = getAttributeValue(userType, "family_name");
        String gender = getAttributeValue(userType, "gender");
        return new User(id, email, givenName, familyName, gender);
    }

    private User mapToUser(CachedUser cachedUser) {
        return new User(
                cachedUser.getId(),
                cachedUser.getEmail(),
                cachedUser.getGivenName(),
                cachedUser.getFamilyName(),
                cachedUser.getGender()
        );
    }

    private String getAttributeValue(UserType userType, String attributeName) {
        return userType.attributes().stream()
                .filter(attr -> attr.name().equalsIgnoreCase(attributeName))
                .findFirst()
                .map(AttributeType::value)
                .orElse(null);
    }

    private List<CachedUser> getAllCachedUsers() {
        Iterable<CachedUser> cachedUsersIterable = cachedUserRepository.findAll();
        List<CachedUser> listCachedUsers = new ArrayList<>();
        cachedUsersIterable.forEach(listCachedUsers::add);
        return listCachedUsers;
    }

    private List<CachedUser> getCachedUsersByIds(Set<String> ids) {
        Iterable<CachedUser> cachedUsersIterable = cachedUserRepository.findAllById(ids);
        List<CachedUser> listCachedUsers = new ArrayList<>();
        cachedUsersIterable.forEach(listCachedUsers::add);
        return listCachedUsers;
    }

    private void cacheUsers(List<User> users) {
        List<CachedUser> cachedUsers = users.stream()
                .map(user -> CachedUser.builder()
                        .id(user.id())
                        .email(user.email())
                        .givenName(user.givenName())
                        .familyName(user.familyName())
                        .gender(user.gender())
                        .build())
                .toList();
        cachedUserRepository.saveAll(cachedUsers);
    }

}