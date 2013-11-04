package es.fjcmz.processor;

public class UnhandledException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2872408374717846635L;

	public UnhandledException() {
		super();
	}
	
	public UnhandledException(String message) {
		super(message);
	}
	
	public UnhandledException(Throwable t) {
		super(t);
	}
	
	public UnhandledException(String message, Throwable t) {
		super(message, t);
	}
	
}
