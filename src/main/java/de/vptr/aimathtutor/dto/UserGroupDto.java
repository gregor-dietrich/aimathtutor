package de.vptr.aimathtutor.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object for user groups.
 * Contains user group information including identifier and name.
 */
@SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "DTO used for JSON mapping and UI binding; public fields are intentional")
public class UserGroupDto {
    public Long id;

    @NotBlank(message = "Name is required")
    public String name;
}
