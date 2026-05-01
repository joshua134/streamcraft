package live.streamcraft.model.request;

@lombok.Setter
@lombok.Getter
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class NotificationUpdateRequest {
	private Boolean read;
    private Boolean dismissed;
}
