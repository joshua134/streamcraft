package live.streamcraft.exception;

public class StreamLimitExceededException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public StreamLimitExceededException(String msg) {
		super(msg);
	}
	
	public StreamLimitExceededException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
