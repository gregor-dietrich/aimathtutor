package de.vptr.aimathtutor.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object for user groups. Contains user group information including identifier and name.
 */
public class UserGroupDto {
    @Nullable
    public String publicId;

    @NotBlank(message = "Name is required")
    @Nullable
    public String name;
}
