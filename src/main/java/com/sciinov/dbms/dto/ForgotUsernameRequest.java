package com.sciinov.dbms.dto;

import lombok.Data;

@Data
public class ForgotUsernameRequest {
    /** Either email or phoneNumber must be provided */
    private String email;
    private String phoneNumber;
}
