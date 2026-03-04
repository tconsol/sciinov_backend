package com.sciinov.dbms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    @Builder.Default
    private String type = "Bearer";
    private String id;
    private String userId;
    private String email;
    private List<String> roles;

    public JwtResponse(String accessToken, String id, String userId, String email, List<String> roles) {
        this.token = accessToken;
        this.type = "Bearer";
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.roles = roles;
    }

    public JwtResponse(String accessToken, String refreshToken, String id, String userId, String email, List<String> roles) {
        this.token = accessToken;
        this.refreshToken = refreshToken;
        this.type = "Bearer";
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.roles = roles;
    }
}
