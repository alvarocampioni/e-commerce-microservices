package com.ms.user_service.service;

import com.ms.user_service.dto.UserNotificationDTO;
import com.ms.user_service.dto.UserRequest;
import com.ms.user_service.events.UserEventProducer;
import com.ms.user_service.exception.ResourceNotFoundException;
import com.ms.user_service.exception.UnauthorizedException;
import com.ms.user_service.exception.UserNotVerifiedException;
import com.ms.user_service.model.Role;
import com.ms.user_service.model.User;
import com.ms.user_service.repository.UserRepository;
import jakarta.transaction.Transactional;
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
    @Transactional
    public void addUser(UserRequest userRequest) {

        //check if already exists
        User existing = userRepository.findById(userRequest.email()).orElse(null);
        if (existing != null) { throw new UnauthorizedException("User already exists!"); }

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
    @Transactional
    public void generateNewCode(UserRequest userRequest) {
        Optional<User> optional = userRepository.findById(userRequest.email());
        if(optional.isPresent()) {
            User user = optional.get();
            if(!bCryptPasswordEncoder.matches(userRequest.password(), user.getPassword())) {
                throw new UnauthorizedException("Invalid password!");
            }

            if(user.isVerified()){
                throw new UnauthorizedException("User is already verified");
            }

            String code = codeGenerationService.generateCode();
            user.setCode(code);
            userRepository.save(user);
            String subject = "New email verification code";
            String content = "Use the following code to verify your email: " + code;
            userEventProducer.sendUserNotification(new UserNotificationDTO(user.getEmail(), subject, content));
        } else {
            throw new ResourceNotFoundException("User not found");
        }
    }

    // verify the code sent to the email
    @Transactional
    public void verifyEmail(String email, String code) {
        Optional<User> optional = userRepository.findById(email);
        if(optional.isPresent() && optional.get().getCode().equals(code)) {
            User user = optional.get();
            user.setVerified(true);
            userRepository.save(user);
        } else {
            throw new UnauthorizedException("Invalid code, check email or generate new code");
        }
    }

    // generate login jwt token
    public String generateToken(UserRequest userRequest) {
        Optional<User> optional = userRepository.findById(userRequest.email());
        if(optional.isPresent()) {
            User user = optional.get();

            if(!bCryptPasswordEncoder.matches(userRequest.password(), user.getPassword())) {
                throw new UnauthorizedException("Invalid password");
            }

            if(!user.isVerified()){
                throw new UserNotVerifiedException("Please verify your email before performing this action");
            }

            return jwtService.generateToken(userRequest.email(), optional.get().getRole().getName());
        } else {
            throw new ResourceNotFoundException("User not found");
        }
    }

    // change user password and notify
    @Transactional
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

    @Transactional
    public void deleteUser(String role, String email){
        if(!role.equals("ADMIN")){
            throw new UnauthorizedException("Unauthorized to perform this action");
        }

        Optional<User> optional = userRepository.findById(email);
        if(optional.isPresent()){
            userRepository.deleteById(email);
            userEventProducer.sendUserDeleted(email);

            String subject = "Account deleted";
            String content = "Your account has been deleted !";
            userEventProducer.sendUserNotification(new UserNotificationDTO(email, subject, content));
        } else {
            throw new ResourceNotFoundException("User not found");
        }
    }
}
