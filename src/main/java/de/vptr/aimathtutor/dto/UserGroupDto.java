package de.vptr.aimathtutor.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object for user groups.
 * Contains user group information including identifier and name.
 */
public class UserGroupDto {
    public String publicId;

    @NotBlank(message = "Name is required")
    public String name;
}
