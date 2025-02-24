package com.ms.user_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "t_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;
    private String code;
    private boolean isVerified;
    @CreationTimestamp
    private Timestamp created;

}
