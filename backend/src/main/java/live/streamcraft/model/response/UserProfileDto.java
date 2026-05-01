package live.streamcraft.model.response;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Builder
public class UserProfileDto {
	private String id;
    private String uname;
    private String email;
    private String avatarUrl;
    private boolean enabled;
    private boolean locked;
    private boolean subscribed;
    private LocalDateTime subscriptionEndDate;
    private int streamsCreatedCount;
    private int remainingFreeStreams;
    private boolean canCreateStream;
    private String role;
    private LocalDateTime createdAt;
}
