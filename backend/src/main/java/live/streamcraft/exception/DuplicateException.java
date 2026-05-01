package live.streamcraft.exception;

public class DuplicateException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DuplicateException(String msg) {
		super(msg);
	}
	
	public DuplicateException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
