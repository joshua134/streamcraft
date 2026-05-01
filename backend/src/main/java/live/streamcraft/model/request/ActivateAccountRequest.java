package live.streamcraft.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivateAccountRequest {
	@NotBlank(message="Activation code is required.")
	@Size(min=6,max=6, message="Activation code must be 6 digit only.")
    private String code;
}
