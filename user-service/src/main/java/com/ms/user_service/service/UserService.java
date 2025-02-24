package com.ms.user_service.service;

import com.ms.user_service.dto.UserConfirmationDTO;
import com.ms.user_service.dto.UserNotificationDTO;
import com.ms.user_service.dto.UserRequest;
import com.ms.user_service.events.UserEventProducer;
import com.ms.user_service.model.Role;
import com.ms.user_service.model.User;
import com.ms.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserEventProducer userEventProducer;
    private final CodeGenerationService codeGenerationService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtService jwtService;

    @Autowired
    public UserService(UserRepository userRepository, UserEventProducer userEventProducer, CodeGenerationService codeGenerationService, BCryptPasswordEncoder bCryptPasswordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userEventProducer = userEventProducer;
        this.codeGenerationService = codeGenerationService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.jwtService = jwtService;
    }

    // register user
    public void addUser(UserRequest userRequest) {

        //check if already exists
        User existing = userRepository.findById(userRequest.email()).orElse(null);
        if (existing != null) { throw new IllegalArgumentException("User already exists!"); }

        // generate email verification code
        String code = codeGenerationService.generateCode();

        // create the user
        User user = new User();
        user.setEmail(userRequest.email());
        user.setCode(code);
        user.setRole(Role.CUSTOMER);
        user.setVerified(false);
        long time = System.currentTimeMillis();
        user.setCreated(new Timestamp(time));

        // encrypt password
        String password = bCryptPasswordEncoder.encode(userRequest.password());
        user.setPassword(password);

        userRepository.save(user);

        // notify user
        String subject = "Email verification code";
        String content = "Use the following code to verify your email: " + code;
        userEventProducer.sendUserNotification(new UserNotificationDTO(user.getEmail(), subject, content));
    }

    // generate a new code to verify email
    public void generateNewCode(UserRequest userRequest) {
        Optional<User> optional = userRepository.findById(userRequest.email());
        if(optional.isPresent() && !optional.get().isVerified() && bCryptPasswordEncoder.matches(userRequest.password(), optional.get().getPassword())) {
            User user = optional.get();
            String code = codeGenerationService.generateCode();
            user.setCode(code);
            userRepository.save(user);
            String subject = "New email verification code";
            String content = "Use the following code to verify your email: " + code;
            userEventProducer.sendUserNotification(new UserNotificationDTO(user.getEmail(), subject, content));
        } else {
            throw new IllegalArgumentException("Invalid Request");
        }
    }

    // verify the code sent to the email
    public void verifyEmail(String email, String code) {
        Optional<User> optional = userRepository.findById(email);
        if(optional.isPresent() && optional.get().getCode().equals(code)) {
            User user = optional.get();
            user.setVerified(true);
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("Invalid code");
        }
    }

    // generate login jwt token
    public String generateToken(UserRequest userRequest) {
        Optional<User> optional = userRepository.findById(userRequest.email());
        if(optional.isPresent() && optional.get().isVerified() && bCryptPasswordEncoder.matches(userRequest.password(), optional.get().getPassword())) {
            return jwtService.generateToken(userRequest.email(), optional.get().getRole().getName());
        } else {
            throw new IllegalArgumentException("Please verify email to login !");
        }
    }

    // change user password and notify
    public void updatePassword(String email, String password){
        Optional<User> optional = userRepository.findById(email);
        if(optional.isPresent()) {
            User user = optional.get();
            user.setPassword(bCryptPasswordEncoder.encode(password));
            userRepository.save(user);
            String subject = "Password updated";
            String content = "Your password was successfully updated !";
            userEventProducer.sendUserNotification(new UserNotificationDTO(user.getEmail(), subject, content));
        }
    }
}
