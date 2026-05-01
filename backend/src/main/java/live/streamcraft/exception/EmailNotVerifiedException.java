package live.streamcraft.exception;

public class EmailNotVerifiedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public EmailNotVerifiedException(String msg) {
		super(msg);
	}
	
	public EmailNotVerifiedException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
