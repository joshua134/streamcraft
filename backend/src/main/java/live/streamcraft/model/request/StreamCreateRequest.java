package live.streamcraft.model.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StreamCreateRequest {
	@NotBlank(message = "Title is required")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    private String categoryId;
    private LocalDateTime scheduledStartTime;
    private Integer durationMinutes;
}
