package live.streamcraft.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
	@NotNull(message="Email is required")
	@Email(message="Use a valid email address")
	private String email;
	
	@NotNull(message="Username is required")
	@Size(min=4, message="Username cannot be less than 4 characters")
	private String uname;
	
	@NotNull(message="Password is required")
	@Size(min=8, message="Password cannot be less than 8 characters.")
	private String password;
	
	@NotNull(message="Confirm password is required")
	@Size(min=8, message="Confirm password cannot be less than 8 characters.")
	private String confirmPassword;
}
