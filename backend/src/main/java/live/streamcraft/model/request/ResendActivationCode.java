package live.streamcraft.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendActivationCode {
	@NotNull(message="Email is required.")
	@Email(message="Use a valid email address")
	private String email;
}
