package live.streamcraft.util;

import java.security.SecureRandom;

import org.apache.commons.codec.digest.DigestUtils;

public final class CodeGenerator {
	private static final SecureRandom secureRandom = new SecureRandom();
	public static String code() {
		int number = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(number);
	}
	
	public static String hashToken(String token) {
		return DigestUtils.sha256Hex(token);
	}

}
