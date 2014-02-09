package gravestone;

public final class NoGraveException extends Throwable {
	public NoGraveException(String message)
	{
		super(message, null, false, false);
	}
}
