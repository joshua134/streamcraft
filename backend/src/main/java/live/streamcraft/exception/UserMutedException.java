package live.streamcraft.exception;

public class UserMutedException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public UserMutedException(String msg) {
		super(msg);
	}
	
	public UserMutedException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
