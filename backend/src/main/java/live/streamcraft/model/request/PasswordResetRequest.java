package live.streamcraft.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {
	@NotNull(message="Password reset code is required")
	@Size(min=6,max=6,message="Password reset code can only have 6 digits.")
	private String code;
	
	@NotNull(message="Password is required")
	@Size(min=8,message="Password must have 8 characters or more.")
	private String password;
	
	@NotNull(message="Confirm password is required")
	@Size(min=6,max=6,message="Confirm password must have 8 characters or more.")
	private String confirmPassword;
}
