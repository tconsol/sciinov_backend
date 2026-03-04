package com.sciinov.dbms.dto;

import java.util.List;

public class JwtResponse {
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private String id;
    private String userId;
    private String email;
    private List<String> roles;

    public JwtResponse(String accessToken, String id, String userId, String email, List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.roles = roles;
    }

    public JwtResponse(String accessToken, String refreshToken, String id, String userId, String email, List<String> roles) {
        this.token = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.roles = roles;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
