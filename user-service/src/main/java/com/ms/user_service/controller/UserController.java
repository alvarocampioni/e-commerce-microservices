package com.ms.user_service.controller;

import com.ms.user_service.dto.LoginResponseDTO;
import com.ms.user_service.dto.UserConfirmationDTO;
import com.ms.user_service.dto.UserRequest;
import com.ms.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<String> register(@RequestBody UserRequest user) {
        userService.addUser(user);
        return new ResponseEntity<>("User registered !", HttpStatus.CREATED);
    }

    @PostMapping("auth/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoginResponseDTO> login(@RequestBody UserRequest user) {
        return new ResponseEntity<>(new LoginResponseDTO(userService.generateToken(user)), HttpStatus.OK);
    }

    @PutMapping("auth/email/verify")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> verifyEmail(@RequestBody UserConfirmationDTO user) {
        userService.verifyEmail(user.email(), user.code());
        return new ResponseEntity<>("Email verified !", HttpStatus.OK);
    }

    @PutMapping("auth/code")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> generateNewCode(@RequestBody UserRequest user) {
        userService.generateNewCode(user);
        return new ResponseEntity<>("New code generated !", HttpStatus.OK);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> updatePassword(@RequestHeader("X-USER-EMAIL") String email, @RequestParam String password) {
        userService.updatePassword(email, password);
        return new ResponseEntity<>("Password updated !", HttpStatus.OK);
    }

    @DeleteMapping("/{email}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteUser(@RequestHeader(value = "X-USER-ROLE") String role, @PathVariable String email) {
        userService.deleteUser(role, email);
        return new ResponseEntity<>("User deleted !", HttpStatus.OK);
    }
}
