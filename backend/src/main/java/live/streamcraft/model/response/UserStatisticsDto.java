package live.streamcraft.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDto {
	 // Total counts
	private long totalUsers;
    private long enabledUsers;
    private long disabledUsers;
    private long lockedUsers;
    private long unverifiedUsers;
    private long subscribedUsers;
    
    // Role-based counts
    private long adminUsers;
    private long moderatorUsers;
    private long regularUsers;
    
    // Time-based counts
    private long usersJoinedThisMonth;
    private long usersJoinedLastWeek;
    
    // Optional: percentages (calculated fields)
    @Builder.Default
    private double enabledPercentage = 0.0;
    @Builder.Default
    private double lockedPercentage = 0.0;
    @Builder.Default
    private double subscribedPercentage = 0.0;
    
    /**
     * Helper method to calculate percentages after building
     */
    public UserStatisticsDto calculatePercentages() {
        if (totalUsers > 0) {
            this.enabledPercentage = (enabledUsers * 100.0) / totalUsers;
            this.lockedPercentage = (lockedUsers * 100.0) / totalUsers;
            this.subscribedPercentage = (subscribedUsers * 100.0) / totalUsers;
        }
        return this;
    }
}
