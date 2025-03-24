package com.ms.user_service.controller;

import com.ms.user_service.dto.LoginResponseDTO;
import com.ms.user_service.dto.UserConfirmationDTO;
import com.ms.user_service.dto.UserRequest;
import com.ms.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
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
    @Operation(summary = "Register New User", description = "Registers user and sends a verification email.")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> register(@RequestBody UserRequest user) {
        userService.addUser(user);
        return new ResponseEntity<>("User registered !", HttpStatus.CREATED);
    }

    @PostMapping("auth/login")
    @Operation(summary = "Login", description = "Logs user in and returns a JWT token for API authentication.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoginResponseDTO> login(@RequestBody UserRequest user) {
        return new ResponseEntity<>(new LoginResponseDTO(userService.generateToken(user)), HttpStatus.OK);
    }

    @PutMapping("auth/email/verify")
    @Operation(summary = "Verify Email", description = "Verifies the email by using the code received.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> verifyEmail(@RequestBody UserConfirmationDTO user) {
        userService.verifyEmail(user.email(), user.code());
        return new ResponseEntity<>("Email verified !", HttpStatus.OK);
    }

    @PutMapping("auth/code")
    @Operation(summary = "Generate Code", description = "Generates new email verification code.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> generateNewCode(@RequestBody UserRequest user) {
        userService.generateNewCode(user);
        return new ResponseEntity<>("New code generated !", HttpStatus.OK);
    }

    @PutMapping("/password")
    @Operation(summary = "Update Password", description = "Changes user password.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> updatePassword(@RequestParam String password, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        userService.updatePassword(email, password);
        return new ResponseEntity<>("Password updated !", HttpStatus.OK);
    }

    @DeleteMapping("/{deletedEmail}")
    @Operation(summary = "Delete User", description = "Removes specified user from the database.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteUser(@PathVariable String deletedEmail, HttpServletRequest request) {
        String role = request.getHeader("X-USER-ROLE");
        String requestedEmail = request.getHeader("X-USER-EMAIL");
        userService.deleteUser(role, deletedEmail, requestedEmail);
        return new ResponseEntity<>("User deleted !", HttpStatus.OK);
    }
}
