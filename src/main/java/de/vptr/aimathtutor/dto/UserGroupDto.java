package de.vptr.aimathtutor.dto;

import jakarta.validation.constraints.NotBlank;

public class UserGroupDto {
    public Long id;

    @NotBlank(message = "Name is required")
    public String name;
}
