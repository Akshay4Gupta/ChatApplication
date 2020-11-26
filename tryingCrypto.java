import java.io.*;
import java.util.*;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class tryingCrypto{
	private static final String ALGORITHM = "RSA";
	public static byte[] encrypt(byte[] publickey, byte[] data) throws Exception{
		PublicKey key = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publickey));
		System.out.println("here is the publicKey :<>: "+key.toString());
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		System.out.println("here is the cipher :<>: " + cipher);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] encryptedBytes = cipher.doFinal(data);
		System.out.println(data);
        return encryptedBytes;
	}
	public static KeyPair generateKeyPairs() throws NoSuchAlgorithmException, NoSuchProviderException{
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
		System.out.println("here is the keyGen getting instance of the RSA :<>: "+ keyGen.toString());
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		System.out.println("here is the random no. :<>: "+ random.toString());
		keyGen.initialize(512, random);
		System.out.println("here is the keyGen after initialize :<>: "+ keyGen.toString());
		KeyPair generateKeyPair = keyGen.generateKeyPair();
		System.out.println("here is the keyGen after initialize :<>: "+ generateKeyPair.toString());
        return generateKeyPair;
	}
	public static void main(String[] args)	throws Exception{
		KeyPair genkeypair = generateKeyPairs();
		System.out.println("here is the public key :<>: "+genkeypair);
		byte[] publicKey = genkeypair.getPublic().getEncoded();
		System.out.println("here is the public key :<>: "+publicKey);
		byte[] privateKey = genkeypair.getPrivate().getEncoded();
		System.out.println("here is the private key :<>: "+privateKey);
		Scanner input = new Scanner(System.in);
		String x = input.nextLine();
		byte[] encrypted = encrypt(publicKey,x.getBytes());
		System.out.println(encrypted.toString());
	}
}