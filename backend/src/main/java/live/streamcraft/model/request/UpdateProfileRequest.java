package live.streamcraft.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@lombok.Getter
@lombok.Setter
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class UpdateProfileRequest {
	 @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
	 private String uname;

	 @Email(message = "Invalid email address")
	 private String email;

}
