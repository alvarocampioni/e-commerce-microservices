package com.ms.user_service.controller;

import com.ms.user_service.dto.UserConfirmationDTO;
import com.ms.user_service.dto.UserRequest;
import com.ms.user_service.service.UserService;
import jakarta.persistence.PostRemove;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody UserRequest user) {
        userService.addUser(user);
    }

    @PostMapping("auth/login")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String login(@RequestBody UserRequest user) {
        return userService.generateToken(user);
    }

    @PostMapping("auth/email/verify")
    public void verifyEmail(@RequestBody UserConfirmationDTO user) {
        userService.verifyEmail(user.email(), user.code());
    }

    @PostMapping("auth/code")
    public void generateNewCode(@RequestBody UserRequest user) {
        userService.generateNewCode(user);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updatePassword(@RequestHeader("X-USER-EMAIL") String email, @RequestParam String password) {
        userService.updatePassword(email, password);
    }



}
