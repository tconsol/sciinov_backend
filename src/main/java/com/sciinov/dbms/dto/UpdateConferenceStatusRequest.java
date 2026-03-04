package com.sciinov.dbms.dto;

import com.sciinov.dbms.entity.Conference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConferenceStatusRequest {
    /** ACTIVE or INACTIVE */
    private Conference.Status status;
}
