package live.streamcraft.exception;

public class StreamingServerException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public StreamingServerException(String msg) {
		super(msg);
	}
	
	public StreamingServerException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
