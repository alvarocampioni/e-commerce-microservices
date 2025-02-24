package com.ms.api_gateway.config;

import com.ms.api_gateway.dto.TokenValidationDTO;
import com.ms.api_gateway.util.JwtUtil;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<UserAuthenticationGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;

    @Autowired
    public UserAuthenticationGatewayFilterFactory(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            try{
                String path = exchange.getRequest().getURI().getPath();
                if (path.contains("api/user/auth/") || (path.contains("api/product") && !path.contains("/stock"))) {
                    return chain.filter(exchange);
                }

                // check if the auth header is present
                List<String> headers = exchange.getRequest().getHeaders().get(config.getHeaderName());
                if(headers != null && !headers.isEmpty() && headers.getFirst().contains("Bearer ")){
                    String token = headers.getFirst().substring(7);
                    TokenValidationDTO tokenValidationDTO = jwtUtil.validateToken(token);
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header("X-USER-EMAIL", tokenValidationDTO.email())
                            .header("X-USER-ROLE", tokenValidationDTO.role())
                            .build();
                    return chain.filter(exchange.mutate().request(request).build());
                } else{
                    throw new IllegalAccessException("Unauthorized to perform this action");
                }
            } catch (Exception e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        });
    }

    @Getter
    @Setter
    @Builder
    public static class Config {
        private String headerName;
    }
}
