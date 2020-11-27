
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.*;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Scanner;

class TCPClient {

    final static int UNENCRYPTED = 1, ENCRYPTED = 2, ENCRYPTEDWITHSIG = 3;
    public static final String ALGORITHM = "RSA";
    public static int mode = UNENCRYPTED;
    private static byte[] privateKeyba = null;
    private static byte[] publicKeyba;
    private static PrivateKey privateKey;
    private static String publicKey = "";
    private static Signature sr;
    public static void main(String argv[]) throws Exception {
        Scanner input = new Scanner(System.in);
        System.out.println("Client Application started.\n----------------------------------\n");
        String userName;
        PublicKey publicKeys;
        boolean flag = false;
        do {
            if (flag) {
                System.out.println("ERROR 101(1) Username Incorrect");
            }
            System.out.print("Enter the UserName:");
            userName = input.next();
            flag = true;
        } while (!isUserNameWellFormed(userName));
        String serverIP = "localhost";
        System.out.println("Enter the mode you want to encrypt your chat(write the corrosponding code)::\n1) UNENCRYPTED\n2) ENCRYPTED\n3) ENCRYPTED WITH SIGNATURE\n");
        mode = input.nextInt();
        if (mode != UNENCRYPTED) {
            KeyPair genkeypair = generateKeyPairs();
            publicKeyba = genkeypair.getPublic().getEncoded();
            privateKeyba = genkeypair.getPrivate().getEncoded();
            publicKey = Base64.getEncoder().encodeToString(publicKeyba);
            publicKeys = genkeypair.getPublic();
            privateKey = genkeypair.getPrivate();
        }if(mode==ENCRYPTEDWITHSIG){
            sr = Signature.getInstance("SHA256WithRSA");
        }
        int serverPortSend = 6789;
        int serverPortRecv = 6790;

        System.out.println("UserName:" + userName);
        try {
            Socket clientSocketSend = new Socket(serverIP, serverPortSend);
            DataOutputStream outToServerSend = new DataOutputStream(clientSocketSend.getOutputStream());
            DataInputStream inFromServerSend = new DataInputStream(clientSocketSend.getInputStream());

            Socket clientSocketRecv = new Socket(serverIP, serverPortRecv);
            DataOutputStream outToServerRecv = new DataOutputStream(clientSocketRecv.getOutputStream());
            DataInputStream inFromServerRecv = new DataInputStream(clientSocketRecv.getInputStream());

            System.out.println("Sending registration msg..");

            String sendRegSentence;
            sendRegSentence = "REGISTER TOSEND " + userName + "\n\n";
            outToServerSend.writeUTF(sendRegSentence);
            outToServerSend.flush();
            String responseSendRegSentence = inFromServerSend.readUTF();
            System.out.println("FROM SERVER(reg send response):\t" + responseSendRegSentence);

            String recvRegSentence;
            if (mode == UNENCRYPTED) {
                recvRegSentence = "REGISTER TORECV " + userName + "\n\n";
            } else {
                recvRegSentence = "REGISTER TORECV " + userName + "\n" + publicKey + "\n" + mode + "\n\n";
            }
            outToServerRecv.writeUTF(recvRegSentence);
            outToServerRecv.flush();
            String responseRecvRegSentence = inFromServerRecv.readUTF();
            System.out.println("FROM SERVER(reg recv response):\t" + responseRecvRegSentence);

            try {
                if (responseSendRegSentence.startsWith("REGISTERED TOSEND ")) {
                    CLSocketThreadSend socketThreadSend;
                    if (mode != ENCRYPTEDWITHSIG) {
                        socketThreadSend = new CLSocketThreadSend(userName, clientSocketSend, inFromServerSend, outToServerSend);
                    } else {
                        socketThreadSend = new CLSocketThreadSend(userName, clientSocketSend, inFromServerSend, outToServerSend, privateKeyba);
                    }
                    Thread thread1 = new Thread(socketThreadSend);
                    thread1.start();
                } else {
                    throw new Exception("User Can't be registered to send\n");
                }
                if (responseRecvRegSentence.startsWith("REGISTERED TORECV")) {
                    CLSocketThreadRead socketThreadRecv;
                    if (mode != UNENCRYPTED) {
                        socketThreadRecv = new CLSocketThreadRead(userName, clientSocketRecv, inFromServerRecv, outToServerRecv, privateKeyba);
                    } else {
                        socketThreadRecv = new CLSocketThreadRead(userName, clientSocketRecv, inFromServerRecv, outToServerRecv);
                    }
                    Thread thread2 = new Thread(socketThreadRecv);
                    thread2.start();
                } else {
                    throw new Exception("User can't be registered to recieve\n");
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        } catch (IOException ee) {
            System.out.println("Error:" + ee.getMessage());
            System.out.println("Unable to connect with server, \nKindly check whether Server is running.");
        }
    }

    public static KeyPair generateKeyPairs() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(1024, random);
        KeyPair generateKeyPair = keyGen.generateKeyPair();
        return generateKeyPair;
    }

    private static boolean isUserNameWellFormed(String clientName) {
        return clientName.matches("[a-zA-Z][a-zA-Z0-9]*");
    }

    public static int i = 0;

    public static String sig(String text) {
        try {
            byte[] data = text.getBytes();

            sr.initSign(privateKey);
            sr.update(data);
            byte[] bytes = sr.sign();
            return Base64.getEncoder().encodeToString(bytes);
        }
        catch (SignatureException | InvalidKeyException e) {
            System.out.println(e);
            return "Exception";
        }
    }

    public static boolean verifysig(byte[] data, String sign, String publicKey){
        try{
            byte[] bytes = Base64.getDecoder().decode(sign);
            PublicKey publicKeys = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
            sr.initVerify(publicKeys);
            sr.update(data);
            return sr.verify(bytes);
        }catch(InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException e){
            System.out.println(e);
            return false;
        }
    }
}

class CLSocketThreadSend implements Runnable {

    String userName;
    Socket connectionSocketSend;
    Socket connectionSocketRecv;
    DataInputStream inFromServer;
    DataOutputStream outToServer;
    byte[] privateKey;

    CLSocketThreadSend(String userName, Socket connectionSocketSend, DataInputStream inFromServer, DataOutputStream outToServer) {
        this.userName = userName;
        this.connectionSocketSend = connectionSocketSend;
        this.inFromServer = inFromServer;
        this.outToServer = outToServer;
    }

    CLSocketThreadSend(String userName, Socket connectionSocketSend, DataInputStream inFromServer, DataOutputStream outToServer, byte[] privateKey) {
        this.userName = userName;
        this.connectionSocketSend = connectionSocketSend;
        this.inFromServer = inFromServer;
        this.outToServer = outToServer;
        this.privateKey = privateKey;
    }

    public void run() {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String inSentence, outSentence , userInput = "", text;
                while (!((text = inFromUser.readLine()).isEmpty())) {
                    userInput += text + "\n";
                }
                userInput = userInput.trim();
                if (userInput.startsWith("@") && userInput.contains(":")) {
                    String msg = userInput.substring(userInput.indexOf(":") + 1, userInput.length()).trim();
                    String recipientName = userInput.substring(userInput.indexOf("@") + 1, userInput.indexOf(":"));
                    if (TCPClient.mode != TCPClient.UNENCRYPTED) {
                        String fetchKey = "FETCHKEY " + recipientName + "\n\n";
                        outToServer.writeUTF(fetchKey);
                        outToServer.flush();
                        String modeKey = inFromServer.readUTF();
                        String encMessage = Base64.getEncoder().encodeToString(encrypt(Base64.getDecoder().decode(modeKey), msg.getBytes()));
                        if (TCPClient.mode == TCPClient.ENCRYPTEDWITHSIG) {
                            String signature = TCPClient.sig(msg);
                            outSentence = "SEND " + recipientName + "\n" + "Content-length: " + encMessage.length() + "\n\n" + signature + "\n" + encMessage;
                        } else {
                            outSentence = "SEND " + recipientName + "\n" + "Content-length: " + encMessage.length() + "\n\n" + encMessage;
                        }

                    } else {
                        outSentence = "SEND " + recipientName + "\n" + "Content-length: " + msg.length() + "\n\n" + msg;
                    }
                    outToServer.writeUTF(outSentence);
                    outToServer.flush();

                    inSentence = inFromServer.readUTF().trim();
                    if (inSentence.startsWith("SENT ") || inSentence.startsWith("ERROR 103 ")) {
                        System.out.println(inSentence);
                    } else {
                        System.out.println("ERROR 102 C Unable to send\n\n");
                    }
                } else {
                    System.out.println("TYPE AGAIN. Correct Message format is @[recipient username]:[message]");
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private static byte[] encrypt(byte[] publickey, byte[] data) throws Exception {
        PublicKey key = KeyFactory.getInstance(TCPClient.ALGORITHM).generatePublic(new X509EncodedKeySpec(publickey));
        Cipher cipher = Cipher.getInstance(TCPClient.ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(data);
        return encryptedBytes;
    }
}

class CLSocketThreadRead implements Runnable {
    String userName;
    Socket connectionSocketSend;
    Socket connectionSocketRecv;
    DataInputStream inFromServer;
    DataOutputStream outToServer;
    byte[] privateKey;

    CLSocketThreadRead(String userName, Socket connectionSocketRecv, DataInputStream inFromServer, DataOutputStream outToServer) {
        this.userName = userName;
        this.connectionSocketRecv = connectionSocketRecv;
        this.inFromServer = inFromServer;
        this.outToServer = outToServer;
    }

    CLSocketThreadRead(String userName, Socket connectionSocketRecv, DataInputStream inFromServer, DataOutputStream outToServer, byte[] privateKey) {
        this.userName = userName;
        this.connectionSocketRecv = connectionSocketRecv;
        this.inFromServer = inFromServer;
        this.outToServer = outToServer;
        this.privateKey = privateKey;
    }

    public void run() {
        while (true) {
            try {
                String inSentence;
                String outSentence;
                inSentence = inFromServer.readUTF();
                String[] ar = inSentence.split("\n");
                outToServer.flush();
                if (inSentence != null) {
                    if (ar[0].trim().startsWith("FORWARD")) {
                        String senderName = ar[0].substring(7, ar[0].length()).trim();
                        String msgSentence = "";
                        String contentLengthSentence = ar[1];
                        if (contentLengthSentence.startsWith("Content-length:")) {
                            String contLenStr = contentLengthSentence.substring(15, contentLengthSentence.length());
                            try {
                                int contLength = Integer.parseInt(contLenStr);
                                String hashmsg = "";
                                if (TCPClient.mode == TCPClient.ENCRYPTEDWITHSIG) {
                                    int it = 2;
                                    while (ar[it].trim().isEmpty()) {
                                        it++;
                                    }
                                    hashmsg = ar[it];
                                    it++;
                                    for (; it < ar.length; it++) {
                                        msgSentence += ar[it] + "\n";
                                    }
                                } else {
                                    for (int it = 2; it < ar.length; it++) {
                                        msgSentence += ar[it] + "\n";
                                    }
                                }
                                msgSentence = msgSentence.trim();
                                if (TCPClient.mode == TCPClient.ENCRYPTED) {
                                    byte[] msgs = decrypt(privateKey, Base64.getDecoder().decode(msgSentence));
                                    System.out.println("#" + senderName + ":" + new String(msgs) + "\n");
                                } else if (TCPClient.mode == TCPClient.ENCRYPTEDWITHSIG) {
//                                    String fetchKey = "FETCHKEY " + senderName + "\n\n";
//                                    outToServer.writeUTF(fetchKey);
//                                    outToServer.flush();
                                    String modeKey = inFromServer.readUTF();

                                    byte[] msgs = decrypt(privateKey, Base64.getDecoder().decode(msgSentence));
                                    if(TCPClient.verifysig(msgs, hashmsg, modeKey)){
                                        System.out.println("everything alright");
                                    }else{
                                        System.out.println("something wrong");
                                    }
                                    System.out.println("#" + senderName + ":" + new String(msgs) + "\n");
                                } else {
                                    System.out.println("#" + senderName + ":" + msgSentence + "\n");
                                }
                                outSentence = "RECEIVED " + userName + "\n\n";
                                outToServer.writeUTF(outSentence);
                                outToServer.flush();
                            } catch (NumberFormatException nfe) {
                                outSentence = "ERROR 103 Header incomplete" + "\n\n";
                                outToServer.writeUTF(outSentence);
                                outToServer.flush();
                            }
                        } else {
                            outSentence = "ERROR 103 Header incomplete" + "\n\n";
                            outToServer.writeUTF(outSentence);
                            outToServer.flush();
                        }
                    } else if (inSentence.trim().startsWith("ERROR")) {
                        System.out.println("ERROR MSG FROM SERVER:" + inSentence);
                    }
                }
            } catch (Exception e) {
                try {
                    System.out.println("Exception in handling forward data:" + e);
                    break;
                } catch (Exception ee) {
                    System.out.println(ee);
                }

            }
        }
    }//end run

    public static byte[] decrypt(byte[] privateKey, byte[] inputData) throws Exception {

        PrivateKey key = KeyFactory.getInstance(TCPClient.ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKey));
        Cipher cipher = Cipher.getInstance(TCPClient.ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(inputData);
        return decryptedBytes;
    }

}
