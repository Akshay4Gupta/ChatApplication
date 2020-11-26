import java.security.*; 
import java.util.*; 
import sun.misc.BASE64Encoder; 
  
public class sig { 
    public static void main(String[] argv) throws Exception 
    { 
        try { 
            KeyPair keyPair = getKeyPair();
            byte[] data = "test".getBytes("UTF8"); 
            Signature sr = Signature.getInstance("SHA1WithRSA"); 
            sr.initSign(keyPair.getPrivate()); 
            sr.update(data);
            byte[] bytes = sr.sign(); 
  			sr.initVerify(keyPair.getPublic());
  			sr.update(data);
            System.out.println("Signature:" + (sr.verify(bytes)));
            System.out.println(":::") ;
        } 
  
        catch (NoSuchAlgorithmException e) { 
  
            System.out.println("Exception thrown : " + e); 
        } 
        catch (SignatureException e) { 
  
            System.out.println("Exception thrown : " + e); 
        } 
    } 
  
    // defining getKeyPair method 
    private static KeyPair getKeyPair() throws NoSuchAlgorithmException 
    { 
  
        // creating the object of KeyPairGenerator 
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA"); 
  
        // initializing with 1024 
        kpg.initialize(1024); 
  
        // returning the key pairs 
        return kpg.genKeyPair(); 
    } 
} 