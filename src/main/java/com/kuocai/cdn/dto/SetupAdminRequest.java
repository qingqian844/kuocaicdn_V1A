package com.kuocai.cdn.dto;

import lombok.Data;

@Data
public class SetupAdminRequest {
    private String currentPassword;
    private String userName;
    private String email;
    private String newPassword;
}
