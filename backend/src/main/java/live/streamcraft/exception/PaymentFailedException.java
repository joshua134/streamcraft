package live.streamcraft.exception;

public class PaymentFailedException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public PaymentFailedException(String msg) {
		super(msg);
	}

	public PaymentFailedException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
