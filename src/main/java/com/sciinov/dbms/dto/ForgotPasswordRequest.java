package com.sciinov.dbms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "User ID is required")
    private String userId;
}
