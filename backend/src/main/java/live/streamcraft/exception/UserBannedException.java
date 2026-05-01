package live.streamcraft.exception;

public class UserBannedException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public UserBannedException(String msg) {
		super(msg);
	}
	
	public UserBannedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
