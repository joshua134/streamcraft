package live.streamcraft.exception;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AccountLockedException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import live.streamcraft.model.response.AppResponse;
import lombok.extern.slf4j.Slf4j;


@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler  {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<AppResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
    	 System.out.println("=== MethodArgumentNotValidException HANDLER CALLED ===");
    	 log.error("Handler invoked for path: {}", ex.getParameter().getExecutable().toString());
    	 
    	 Map<String, String> errors = new HashMap<>();
    	 ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    	 log.error("Validation failed: {}", errors);

    	 String firstMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

    	 return ResponseEntity.badRequest().body(AppResponse.error(firstMessage, errors));
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<AppResponse<String>> handleJakartaConstraintViolationException(ConstraintViolationException ex) {
        
        String message = ex.getConstraintViolations().iterator().next().getMessage();
        log.error("Entity validation failed: {}", message);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AppResponse.error(message));
    }
 
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<AppResponse<String>> handleNotFoundException(NotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<AppResponse<String>> handleBadCredentialsException(BadCredentialsException ex){
    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<AppResponse<String>> handleUnauthorizedException(UnauthorizedException ex){
    	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AppResponse.error(ex.getMessage()));
    }
    
    
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<AppResponse<String>> handlePaymentFailedException(PaymentFailedException ex){
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<AppResponse<String>> handleInvalidCredentialsException(InvalidCredentialsException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<AppResponse<String>> handleDuplicateException(DuplicateException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<AppResponse<String>> handleInvalidTokenException(InvalidTokenException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(AccountHideMessageException.class)
    public ResponseEntity<AppResponse<String>> handleAccountHideMessageException(AccountHideMessageException ex){
        return ResponseEntity.status(HttpStatus.OK).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<AppResponse<String>> handleAccountLockedException(AccountLockedException ex){
        return ResponseEntity.status(HttpStatus.LOCKED).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<AppResponse<String>> handleLockedException(LockedException ex){
        return ResponseEntity.status(HttpStatus.LOCKED).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<AppResponse<String>> handleEmailNotVerifiedException(EmailNotVerifiedException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(StreamLimitExceededException.class)
    public ResponseEntity<AppResponse<String>> handleStreamLimitExceededException(StreamLimitExceededException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<AppResponse<String>> handleUserBannedException(UserBannedException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(UserMutedException.class)
    public ResponseEntity<AppResponse<String>> handleUserMutedException(UserMutedException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AppResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AppResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(AppResponse.error("You do not have permission to access this resource"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppResponse<String>> handleInternalServerException(Exception ex){
    	log.error("Unexpected error", ex);
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AppResponse.error("An unexpected error occurred"));
    }
}
