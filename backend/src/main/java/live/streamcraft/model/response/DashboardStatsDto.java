package live.streamcraft.model.response;


public record DashboardStatsDto(long totalUsers, long activeUsers, long totalStreams, long liveStreams, long totalCategories) {

}
