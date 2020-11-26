package testing;

import java.io.*;
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
import java.net.*;
import java.util.*;

class TCPClient {

    static int UNENCRYPTED = 1, ENCRYPTED = 2, ENCRYPTEDWITHSIG = 3;
    public static final String ALGORITHM = "RSA";
    public static int mode = UNENCRYPTED;

    public static void main(String argv[]) throws Exception {
        Scanner input = new Scanner(System.in);
        System.out.println("Client Application started.\n----------------------------------\n");
        String userName, publicKey = "", privateKey = "";
        byte[] privateKeyba = null ;
        byte[] publicKeyba;
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
        System.out.println("Enter the mode you want to encrypt your chat(write the corrosponding code)::\n1) UNENCRYPTED\n2) ENCRYPTED\n3) ENCRYPTED WITH SIGNATURE\n::");
        mode = input.nextInt();
        if (mode != UNENCRYPTED) {
            KeyPair genkeypair = generateKeyPairs();
            publicKeyba = genkeypair.getPublic().getEncoded();
            privateKeyba = genkeypair.getPrivate().getEncoded();
            publicKey = Base64.getEncoder().encodeToString(publicKeyba);
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
                    CLSocketThreadSend socketThreadSend = new CLSocketThreadSend(userName, clientSocketSend, inFromServerSend, outToServerSend);
                    Thread thread1 = new Thread(socketThreadSend);
                    thread1.start();
                } else {
                    throw new Exception("User Can't be registered to send\n");
                }
                if (responseRecvRegSentence.startsWith("REGISTERED TORECV")) {
                    CLSocketThreadRead socketThreadRecv;
                    if(mode != UNENCRYPTED){
                        socketThreadRecv = new CLSocketThreadRead(userName, clientSocketRecv, inFromServerRecv, outToServerRecv, privateKeyba);
                    }else{
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
        } catch (Exception ee) {
            System.out.println("Error:" + ee.getMessage());
            System.out.println("Unable to connect with server, \nKindly check whether Server is running.");
        }
//        clientSocket.close(); 
    } //end main

    public static KeyPair generateKeyPairs() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        System.out.println("here is the keyGen getting instance of the RSA :<>: " + keyGen.toString());
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        System.out.println("here is the random no. :<>: " + random.toString());
        keyGen.initialize(512, random);
        System.out.println("here is the keyGen after initialize :<>: " + keyGen.toString());
        KeyPair generateKeyPair = keyGen.generateKeyPair();
        System.out.println("here is the keyGen after initialize :<>: " + generateKeyPair.toString());
        return generateKeyPair;
    }

    private static boolean isUserNameWellFormed(String clientName) {
        return clientName.matches("[a-zA-Z0-9]+");
    }
} //end class TCPClient

class CLSocketThreadSend implements Runnable {

    String userName;
    Socket connectionSocketSend;
    Socket connectionSocketRecv;
    DataInputStream inFromServer;
    DataOutputStream outToServer;

    CLSocketThreadSend(String userName, Socket connectionSocketSend, DataInputStream inFromServer, DataOutputStream outToServer) {
        this.userName = userName;
        this.connectionSocketSend = connectionSocketSend;
        this.inFromServer = inFromServer;
        this.outToServer = outToServer;
    }

    public void run() {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String inSentence = "", outSentence = "", userInput = "", text;
                while (!((text = inFromUser.readLine()).isEmpty())) {
                    userInput += text + "\n";
                }
                userInput = userInput.trim();
                if (userInput.startsWith("@") && userInput.contains(":")) {
                    String msg = userInput.substring(userInput.indexOf(":") + 1, userInput.length()).trim();
//                    int contentLength;
                    String recipientName = userInput.substring(userInput.indexOf("@") + 1, userInput.indexOf(":"));
                    //fetching the key
                    if (TCPClient.mode == TCPClient.ENCRYPTED) {
                        String fetchKey = "FETCHKEY " + recipientName + "\n\n";
                        outToServer.writeUTF(fetchKey);
                        outToServer.flush();
                        String modeKey = inFromServer.readUTF();
//                        System.out.println(":"+modeKey+":");
//                        System.out.println(":?:"+msg+"::");
                        String message=Base64.getEncoder().encodeToString(encrypt(Base64.getDecoder().decode(modeKey), msg.getBytes()));
//                        System.out.println("from sender::"+message+"::");
                        outSentence = "SEND " + recipientName + "\n" + "Content-length: " + message.length() + "\n\n" + message;
                    } else {
                        //end of fetching the key
                        //SENDING THE MESSAGE
                        outSentence = "SEND " + recipientName + "\n" + "Content-length: " + msg.length() + "\n\n" + msg;
                    }
//                    System.out.println("Send Sentence:"+outSentence);
                    outToServer.writeUTF(outSentence);
                    outToServer.flush();

                    inSentence = inFromServer.readUTF().trim();
//                    System.out.println("ack recieved\n");
                    if (inSentence.startsWith("SENT ") || inSentence.startsWith("ERROR 103 ")) {
                        System.out.println(inSentence);
                    } else {
                        System.out.println("ERROR 102 Unable to send\n\n");
                    }
                    //End of sending the message
                } else {
                    System.out.println("TYPE AGAIN. Correct Message format is @[recipient username]:[message]");
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }//end run

    private static byte[] encrypt(byte[] publickey, byte[] data) throws Exception {
        PublicKey key = KeyFactory.getInstance(TCPClient.ALGORITHM).generatePublic(new X509EncodedKeySpec(publickey));
//        System.out.println("here is the publicKey :<>: "+key.toString());
        Cipher cipher = Cipher.getInstance(TCPClient.ALGORITHM);
//        System.out.println("here is the cipher :<>: " + cipher);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(data);
//        System.out.println(data);
        return encryptedBytes;
    }
}//end class

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
                String inSentence = "";
                String outSentence = "";
                inSentence = inFromServer.readUTF();
//                System.out.println("insentence::"+inSentence);
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
                                for (int it = 2; it < ar.length; it++) {
                                    msgSentence += ar[it] + "\n";
                                }
                                msgSentence = msgSentence.trim();
                                //add the decryption part
//                                System.out.println(":<?>:"+msgSentence+":<>:");
                                if(TCPClient.mode!=1){
                                    byte[] msgs = decrypt(privateKey,Base64.getDecoder().decode(msgSentence));
                                    System.out.println("#" + senderName + ":" + new String(msgs) + "\n");
                                }else{
                                    System.out.println("#" + senderName + ":" + msgSentence + "\n");
                                }
                                outSentence = "RECEIVED " + userName + "\n\n";
//                                System.out.println(":<1?>:"+outSentence+"\n");
                                outToServer.writeUTF(outSentence);
//                                System.out.println(":<2?>:\n");
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
                    System.out.println("Exception in handling forward data:" + e.getCause());
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
}//end class
