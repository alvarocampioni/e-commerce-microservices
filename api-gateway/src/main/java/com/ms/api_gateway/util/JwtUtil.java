package com.ms.api_gateway.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.ms.api_gateway.dto.TokenValidationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtUtil {

    private final String secret;

    public JwtUtil(@Value("${JWT.SECRET.KEY}") String secret){
        this.secret = secret;
    }

    public TokenValidationDTO validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String subject = JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
            
            String role = JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getClaim("role").asString();

            return new TokenValidationDTO(subject, role);
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
