package whiteboard.core.exceptions;

@SuppressWarnings("serial")
public class InvalidPasswordException extends Exception {
	public InvalidPasswordException(String msg) {
		super(msg);
	}
}
