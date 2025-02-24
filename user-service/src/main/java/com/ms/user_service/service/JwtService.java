package com.ms.user_service.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    public String secret;

    public JwtService(@Value("${JWT.SECRET.KEY}")String secret) {
        this.secret = secret;
    }

    public String generateToken(String email, String role) {
        try{
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("auth-api")
                    .withSubject(email)
                    .withClaim("role", role)
                    .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new RuntimeException("Error generating token", e);
        }
    }
}
