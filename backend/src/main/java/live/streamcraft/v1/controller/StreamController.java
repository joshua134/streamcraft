package live.streamcraft.v1.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import live.streamcraft.model.request.StreamCreateRequest;
import live.streamcraft.model.request.StreamUpdateRequest;
import live.streamcraft.model.response.AntMediaBroadcast;
import live.streamcraft.model.response.AppResponse;
import live.streamcraft.model.response.PageDto;
import live.streamcraft.model.response.StreamDto;
import live.streamcraft.model.response.StreamSummaryDto;
import live.streamcraft.security.UserPrincipal;
import live.streamcraft.service.StreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/streams")
@RequiredArgsConstructor
@Slf4j
@Validated
public class StreamController {

    private final StreamService streamService;

    @PostMapping
    public ResponseEntity<AppResponse<StreamDto>> createStream(@AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody StreamCreateRequest request) {
    	
        return ResponseEntity.ok(AppResponse.success("Stream created",streamService.createStream(user.getUser().getId(), request)
        ));
    }
    
    @GetMapping("/my-streams")
    public ResponseEntity<AppResponse<PageDto<StreamSummaryDto>>> getMyStreams(@AuthenticationPrincipal UserPrincipal user,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<StreamSummaryDto> page = streamService.getUserStreams(user.getUser().getId(), pageable);
        return ResponseEntity.ok(AppResponse.success("My streams", PageDto.from(page)));
    }
    
    @GetMapping("/my-streams/active")
    public ResponseEntity<AppResponse<List<StreamSummaryDto>>> getMyActiveStreams(@AuthenticationPrincipal UserPrincipal user) {

        List<StreamSummaryDto> streams = streamService.getActiveStreamsByStreamer(user.getUser().getId());
        return ResponseEntity.ok(AppResponse.success("Your active streams retrieved", streams));
    }
    
    @GetMapping("/my-streams/ended")
    public ResponseEntity<AppResponse<PageDto<StreamSummaryDto>>> getMyEndedStreams(
            @AuthenticationPrincipal UserPrincipal user,
            @PageableDefault(size = 20, sort = "actualEndTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<StreamSummaryDto> page = streamService.getEndedStreamsByStreamer(user.getUser().getId(), pageable);
        return ResponseEntity.ok(AppResponse.success("Your ended streams retrieved", PageDto.from(page)));
    }
    
    @PutMapping("/{streamId}/toggle-chat")
    public ResponseEntity<AppResponse<StreamDto>> toggleChat(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable String streamId) {
        StreamDto stream = streamService.toggleChat(user.getUser().getId(), streamId);
        return ResponseEntity.ok(AppResponse.success("Chat toggled successfully", stream));
    }

    @PostMapping("/{streamId}/start")
    public ResponseEntity<AppResponse<StreamDto>> startStream( @AuthenticationPrincipal UserPrincipal user,
            @PathVariable String streamId) {

        return ResponseEntity.ok(AppResponse.success("Stream started",streamService.startStream(user.getUser().getId(), streamId)
        ));
    }

    @PostMapping("/{streamId}/cancel")
    public ResponseEntity<AppResponse<StreamDto>> cancelStream(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable String streamId) {

        return ResponseEntity.ok(AppResponse.success("Stream cancelled",
        		streamService.cancelStream(user.getUser().getId(), streamId)
        ));
    }
    
    @PostMapping("/{streamId}/end")
    public ResponseEntity<AppResponse<StreamDto>> endStream(@AuthenticationPrincipal UserPrincipal user,
    		@PathVariable String streamId) {

        return ResponseEntity.ok(AppResponse.success("Stream ended",streamService.endStream(user.getUser().getId(), streamId)
        ));
    }

    @GetMapping("/{streamId}")
    public ResponseEntity<AppResponse<StreamDto>> getStream(@PathVariable String streamId) {
        return ResponseEntity.ok(AppResponse.success("Stream retrieved",streamService.getStreamById(streamId)));
    }

    @GetMapping("/live")
    public ResponseEntity<AppResponse<PageDto<StreamSummaryDto>>> getLiveStreams(@PageableDefault(size = 20) Pageable pageable) {

        Page<StreamSummaryDto> page = streamService.getLiveStreams(pageable);
        return ResponseEntity.ok(AppResponse.success("Live streams", PageDto.from(page)));
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<AppResponse<PageDto<StreamSummaryDto>>> getUpcomingStreams(
            @PageableDefault(size = 20, sort = "scheduledStartTime", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<StreamSummaryDto> page = streamService.getUpcomingStreams(pageable);
        return ResponseEntity.ok(AppResponse.success("Upcoming streams retrieved", PageDto.from(page)));
    }
    
    @PutMapping("/{streamId}")
    public ResponseEntity<AppResponse<StreamDto>> updateStream(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable String streamId,@Valid @RequestBody StreamUpdateRequest request) {
        StreamDto stream = streamService.updateStream(user.getUser().getId(), streamId, request);
        return ResponseEntity.ok(AppResponse.success("Stream updated successfully", stream));
    }
    
    @PostMapping("/{streamId}/mute/{targetUserId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR') or @streamService.isStreamOwner(#user.getId(), #streamId)")
    public ResponseEntity<AppResponse<StreamDto>> muteUser(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable String streamId,@PathVariable String targetUserId) {

        StreamDto stream = streamService.muteUserInStream(user.getUser().getId(), streamId, targetUserId);
        return ResponseEntity.ok(AppResponse.success("User muted successfully", stream));
    }
    
    @PostMapping("/{streamId}/unmute/{targetUserId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR') or @streamService.isStreamOwner(#user.getId(), #streamId)")
    public ResponseEntity<AppResponse<StreamDto>> unmuteUser(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable String streamId,@PathVariable String targetUserId) {

        StreamDto stream = streamService.unmuteUserInStream(user.getUser().getId(), streamId, targetUserId);
        return ResponseEntity.ok(AppResponse.success("User unmuted successfully", stream));
    }
    
    @PostMapping("/{streamId}/ban/{targetUserId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR') or @streamService.isStreamOwner(#user.getId(), #streamId)")
    public ResponseEntity<AppResponse<StreamDto>> banUser(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable String streamId,@PathVariable String targetUserId) {

        StreamDto stream = streamService.banUserFromStream(user.getUser().getId(), streamId, targetUserId);
        return ResponseEntity.ok(AppResponse.success("User banned successfully", stream));
    }
    
    @PostMapping("/{streamId}/unban/{targetUserId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR') or @streamService.isStreamOwner(#user.getId(), #streamId)")
    public ResponseEntity<AppResponse<StreamDto>> unbanUser(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable String streamId, @PathVariable String targetUserId) {

        StreamDto stream = streamService.unbanUserFromStream(user.getUser().getId(), streamId, targetUserId);
        return ResponseEntity.ok(AppResponse.success("User unbanned successfully", stream));
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<AppResponse<PageDto<StreamSummaryDto>>> getStreamsByCategory(@PathVariable String categoryId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<StreamSummaryDto> page = streamService.getStreamsByCategory(categoryId, pageable);
        return ResponseEntity.ok(AppResponse.success("Streams by category retrieved", PageDto.from(page)));
    }

    @GetMapping("/{streamId}/broadcast-info")
    public ResponseEntity<AppResponse<AntMediaBroadcast>> getBroadcastInfo(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable String streamId) {

        return ResponseEntity.ok(AppResponse.success( "Broadcast info",streamService.getBroadcastInfo(user.getUser().getId(), streamId)
        ));
    }
    
    @DeleteMapping("/{streamId}")
    public ResponseEntity<AppResponse<Void>> deleteStream(@AuthenticationPrincipal UserPrincipal user,
    		@PathVariable String streamId) {

        streamService.deleteStream(user.getUser().getId(), streamId);
        return ResponseEntity.ok(AppResponse.success("Stream deleted successfully"));
    }
}
