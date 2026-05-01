package live.streamcraft.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
	@NotBlank(message="Password reset code is required.")
	@Size(min=6,max=6, message="Password reset code must be 6 digit only.")
    private String code;
    
    @NotBlank(message="Password is required.")
    @Size(min = 8, message="Password must have atleast 8 characters.")
    private String password;
    
    @NotBlank(message="Confirm password is required.")
    @Size(min = 8, message="Confirm password must have atleast 8 characters.")
    private String confirmPassword;
}
