package live.streamcraft.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class StreamCategoryImportDto {
	@NotBlank(message="Name is required")
    private String name;
    
    private String description;
    
    @Min(0)
    private int displayOrder;
    
    private String iconUrl;
}
