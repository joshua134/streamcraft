package live.streamcraft.v1.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import live.streamcraft.model.request.UpdateProfileRequest;
import live.streamcraft.model.response.AppResponse;
import live.streamcraft.model.response.PageDto;
import live.streamcraft.model.response.UserProfileDto;
import live.streamcraft.model.response.UserStatisticsDto;
import live.streamcraft.security.UserPrincipal;
import live.streamcraft.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<AppResponse<PageDto<UserProfileDto>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(AppResponse.success("Users retrieved successfully",userService.getAllUsers(pageable)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AppResponse<UserProfileDto>> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(AppResponse.success("User retrieved",userService.toUserProfileDto(userService.findById(userId))
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<AppResponse<List<UserProfileDto>>> searchUsers(@RequestParam String searchTerm) {
        return ResponseEntity.ok(AppResponse.success("Search results",userService.searchUsers(searchTerm)));
    }

    @GetMapping("/enabled")
    public ResponseEntity<AppResponse<PageDto<UserProfileDto>>> getEnabledUsers(@RequestParam boolean status,Pageable pageable) {

        return ResponseEntity.ok(AppResponse.success("Filtered users",userService.getUsersByEnabledStatus(status, pageable)));
    }

    @GetMapping("/locked")
    public ResponseEntity<AppResponse<PageDto<UserProfileDto>>> getLockedUsers(@RequestParam boolean status,Pageable pageable) {
        return ResponseEntity.ok(AppResponse.success("Filtered users",userService.getUsersByLockedStatus(status, pageable) ));
    }

    @GetMapping("/role")
    public ResponseEntity<AppResponse<PageDto<UserProfileDto>>> getByRole(@RequestParam String role,Pageable pageable) {
        return ResponseEntity.ok(AppResponse.success( "Users by role",userService.getUsersByRole(role, pageable)));
    }

    @PutMapping("/{userId}/enable")
    public ResponseEntity<AppResponse<UserProfileDto>> enableUser(@AuthenticationPrincipal UserPrincipal admin,
            @PathVariable String userId) {

        return ResponseEntity.ok(AppResponse.success("User enabled", userService.enableUser(admin.getUser().getId(), userId)
        ));
    }

    @PutMapping("/{userId}/disable")
    public ResponseEntity<AppResponse<UserProfileDto>> disableUser(@AuthenticationPrincipal UserPrincipal admin,
            @PathVariable String userId,@RequestParam(required = false) String reason) {

        return ResponseEntity.ok(AppResponse.success("User disabled",userService.disableUser(admin.getUser().getId(), userId, reason)
        ));
    }

    @PutMapping("/{userId}/lock")
    public ResponseEntity<AppResponse<UserProfileDto>> lockUser(@AuthenticationPrincipal UserPrincipal admin,
            @PathVariable String userId,@RequestParam(required = false) String reason) {
        return ResponseEntity.ok(AppResponse.success("User locked",userService.lockUser(admin.getUser().getId(), userId, reason)
        ));
    }

    @PutMapping("/{userId}/unlock")
    public ResponseEntity<AppResponse<UserProfileDto>> unlockUser(@AuthenticationPrincipal UserPrincipal admin,
            @PathVariable String userId) {

        return ResponseEntity.ok(AppResponse.success("User unlocked",userService.unlockUser(admin.getUser().getId(), userId)));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<AppResponse<UserProfileDto>> changeRole(@AuthenticationPrincipal UserPrincipal admin,
            @PathVariable String userId, @RequestParam String roleName) {

        return ResponseEntity.ok(AppResponse.success("Role updated",userService.changeUserRole(admin.getUser().getId(), 
        		userId, roleName)));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<AppResponse<UserProfileDto>> updateUser( @PathVariable String userId,
    		@Valid @RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(AppResponse.success("User updated",userService.updateProfile(userId, request)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<AppResponse<Void>> deleteUser(@PathVariable String userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.ok(AppResponse.success("User deleted"));
    }

    @PutMapping("/bulk/enable")
    public ResponseEntity<AppResponse<Integer>> bulkEnable(@AuthenticationPrincipal UserPrincipal admin,
            @RequestBody List<String> userIds) {
        int count = userService.bulkEnableUsers(admin.getUser().getId(), userIds);
        return ResponseEntity.ok(AppResponse.success("Users enabled",count));
    }

    @PutMapping("/bulk/disable")
    public ResponseEntity<AppResponse<Integer>> bulkDisable(@AuthenticationPrincipal UserPrincipal admin,
            @RequestHeader List<String> userIds,@RequestParam(required = false) String reason) {

        int count = userService.bulkDisableUsers(admin.getUser().getId(), userIds, reason);
        return ResponseEntity.ok(AppResponse.success("Users disabled",count));
    }

    @GetMapping("/stats")
    public ResponseEntity<AppResponse<UserStatisticsDto>> getStats() {
        return ResponseEntity.ok(AppResponse.success("User statistics",userService.getUserStatistics()));
    }
}
