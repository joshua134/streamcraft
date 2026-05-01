package live.streamcraft.v1.controller;

import org.springframework.http.ResponseEntity;
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
import live.streamcraft.model.request.ChangePasswordRequest;
import live.streamcraft.model.request.UpdateProfileRequest;
import live.streamcraft.model.response.AppResponse;
import live.streamcraft.model.response.UserProfileDto;
import live.streamcraft.security.UserPrincipal;
import live.streamcraft.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {
	private final UserService userService;
	
	@GetMapping("/me")
    public ResponseEntity<AppResponse<UserProfileDto>> getCurrentProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        
        UserProfileDto profile = userService.toUserProfileDto(userService.findById(currentUser.getUser().getId()));
        return ResponseEntity.ok(AppResponse.success("Profile retrieved", profile));
    }
	
	@GetMapping("/{userId}")
	public ResponseEntity<AppResponse<UserProfileDto>> getUserById( @PathVariable String userId) {

		UserProfileDto profile = userService.toUserProfileDto(userService.findById(userId));
		return ResponseEntity.ok(AppResponse.success("User found", profile));
	}
	
	@GetMapping("/email/{email}")
    public ResponseEntity<AppResponse<UserProfileDto>> getUserByEmail(@PathVariable String email) {
        
        UserProfileDto profile = userService.toUserProfileDto(userService.findByEmail(email));
        return ResponseEntity.ok(AppResponse.success("User found", profile));
    }
	
	@GetMapping("/username/{uname}")
    public ResponseEntity<AppResponse<UserProfileDto>> getUserByUname(@PathVariable String uname) {
        
        UserProfileDto profile = userService.toUserProfileDto(userService.findByUname(uname));
        return ResponseEntity.ok(AppResponse.success("User found", profile));
    }
	
	 @PutMapping("/me")
	 public ResponseEntity<AppResponse<UserProfileDto>> updateProfile( @AuthenticationPrincipal UserPrincipal currentUser,
			 @Valid @RequestBody UpdateProfileRequest request) {

		 UserProfileDto updated = userService.updateProfile(currentUser.getUser().getId(), request);
		 return ResponseEntity.ok(AppResponse.success("Profile updated successfully", updated));
	 }
	 
	 @PostMapping("/me/change-password")
	 public ResponseEntity<AppResponse<Void>> changePassword(@AuthenticationPrincipal UserPrincipal currentUser,
			 @Valid @RequestBody ChangePasswordRequest request) {

		 userService.changePassword(currentUser.getUser().getId(), request);
		 
		 return ResponseEntity.ok(AppResponse.success("Password changed successfully"));
	 }
	 
	 @DeleteMapping("/me")
	 public ResponseEntity<AppResponse<Void>> deleteAccount(@AuthenticationPrincipal UserPrincipal currentUser) {

		 userService.deleteAccount(currentUser.getUser().getId());
		 return ResponseEntity.ok(AppResponse.success("Account deleted successfully"));
	 }
}
