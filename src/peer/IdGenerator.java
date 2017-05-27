package peer;
import java.security.SecureRandom;
import java.math.BigInteger;

public final class IdGenerator {
	private SecureRandom random = new SecureRandom();

	public String nextId() {
		return new BigInteger(130, random).toString(32);
	}
}