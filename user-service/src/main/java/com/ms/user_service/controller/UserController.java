package com.ms.user_service.controller;

import com.ms.user_service.dto.LoginResponseDTO;
import com.ms.user_service.dto.UserConfirmationDTO;
import com.ms.user_service.dto.UserRequest;
import com.ms.user_service.service.UserService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
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
    public ResponseEntity<String> updatePassword(@RequestParam String password, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        userService.updatePassword(email, password);
        return new ResponseEntity<>("Password updated !", HttpStatus.OK);
    }

    @DeleteMapping("/{deletedEmail}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteUser(@PathVariable String deletedEmail, HttpServletRequest request) {
        String role = request.getHeader("X-USER-ROLE");
        String requestedEmail = request.getHeader("X-USER-EMAIL");
        userService.deleteUser(role, deletedEmail, requestedEmail);
        return new ResponseEntity<>("User deleted !", HttpStatus.OK);
    }
}
