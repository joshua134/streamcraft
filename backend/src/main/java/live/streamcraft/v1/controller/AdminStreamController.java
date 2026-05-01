package live.streamcraft.v1.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import live.streamcraft.model.response.AppResponse;
import live.streamcraft.model.response.StreamDto;
import live.streamcraft.security.UserPrincipal;
import live.streamcraft.service.StreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/streams")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminStreamController {

    private final StreamService streamService;

    @GetMapping("/{streamId}")
    public ResponseEntity<AppResponse<StreamDto>> getStream(@PathVariable String streamId) {
        return ResponseEntity.ok(AppResponse.success("Stream retrieved",streamService.getStreamById(streamId)));
    }

    @PostMapping("/{streamId}/force-end")
    public ResponseEntity<AppResponse<StreamDto>> forceEndStream(@PathVariable String streamId) {
        return ResponseEntity.ok(AppResponse.success("Stream forcefully ended",streamService.adminForceEndStream(streamId) ));
    }

    @PutMapping("/{streamId}/feature")
    public ResponseEntity<AppResponse<StreamDto>> featureStream(@PathVariable String streamId) {
        return ResponseEntity.ok(AppResponse.success("Stream featured",streamService.toggleFeature(streamId, true)));
    }

    @PutMapping("/{streamId}/unfeature")
    public ResponseEntity<AppResponse<StreamDto>> unfeatureStream(@PathVariable String streamId) {
        return ResponseEntity.ok(AppResponse.success("Stream unfeatured",streamService.toggleFeature(streamId, false)));
    }

    @PutMapping("/{streamId}/mute-chat")
    public ResponseEntity<AppResponse<StreamDto>> muteChat(@PathVariable String streamId) {
        return ResponseEntity.ok(AppResponse.success("Chat muted",streamService.toggleChatMute(streamId, true)));
    }

    @PutMapping("/{streamId}/unmute-chat")
    public ResponseEntity<AppResponse<StreamDto>> unmuteChat(@PathVariable String streamId) {
        return ResponseEntity.ok(AppResponse.success("Chat unmuted",streamService.toggleChatMute(streamId, false) ));
    }

    @DeleteMapping("/{streamId}")
    public ResponseEntity<AppResponse<Void>> deleteStream(@AuthenticationPrincipal UserPrincipal admin, 
    		@PathVariable String streamId) {
    	streamService.adminDeleteStream(admin.getUser().getId(), streamId);
        return ResponseEntity.ok(AppResponse.success("Stream deleted"));
    }
}
