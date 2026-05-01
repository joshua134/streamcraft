package live.streamcraft.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
	@NotNull(message="Email is required.")
	private String email;
	@Size(min=8, message="Password cannot be less than 8 characters.")
	private String password;
}
