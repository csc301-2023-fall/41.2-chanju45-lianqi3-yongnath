package com.snowtheghost.project41.api.controllers;

import com.snowtheghost.project41.api.models.requests.users.CreateUserRequest;
import com.snowtheghost.project41.api.models.requests.users.LoginUserRequest;
import com.snowtheghost.project41.api.models.responses.games.GetGameResponse;
import com.snowtheghost.project41.api.models.responses.users.CreateUserResponse;
import com.snowtheghost.project41.api.models.responses.users.GetUserResponse;
import com.snowtheghost.project41.api.models.responses.users.LoginResponse;
import com.snowtheghost.project41.database.models.Game;
import com.snowtheghost.project41.database.models.User;
import com.snowtheghost.project41.services.AuthenticationService;
import com.snowtheghost.project41.services.UserService;
import jakarta.annotation.security.PermitAll;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
@RequestMapping("/users")
public class UserController {

    @Value("${backend.url}")
    private String backendUrl;
    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Autowired
    public UserController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest request) {
        String userId = UUID.randomUUID().toString();
        try {
            if ("RESEARCHER".equals(request.getType())) {
                userService.createResearcherUser(userId, request.getUsername(), request.getEmail(), request.getPassword());
            } else if ("PLAYER".equals(request.getType())) {
                userService.createPlayerUser(userId, request.getUsername(), request.getEmail(), request.getPassword());
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        String token = authenticationService.generateToken(userId);
        return ResponseEntity.created(URI.create(String.format(backendUrl + "/users/%s", userId))).body(new CreateUserResponse(token));
    }

    @PostMapping("/login")
    @PermitAll
    public ResponseEntity<LoginResponse> loginUser(@RequestBody LoginUserRequest request) {
        String token = userService.loginUser(request.getEmail(), request.getPassword());
        if (token != null) {
            return ResponseEntity.ok(new LoginResponse(token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }


    @GetMapping("/{userId}")
    public ResponseEntity<GetUserResponse> getUser(@PathVariable String userId) {
        User user;
        try {
            user = userService.getUser(userId);
        } catch (EntityNotFoundException exception) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(userService.mapUserToResponse(user));
    }

    @GetMapping("/me")
    public ResponseEntity<GetUserResponse> getActiveUser(@RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken) {
        String userId;
        User user;
        try {
            userId = authenticationService.getUserId(bearerToken);
            user = userService.getUser(userId);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(userService.mapUserToResponse(user));
    }


}
