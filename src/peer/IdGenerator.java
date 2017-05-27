package peer;
import java.security.SecureRandom;
import java.math.BigInteger;

public final class IdGenerator {
	private static SecureRandom random = new SecureRandom();

	public static String nextId() {
		return new BigInteger(130, random).toString(32);
	}
}