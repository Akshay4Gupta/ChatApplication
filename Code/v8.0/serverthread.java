
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class ClientInfo {

    private String userName;
    private Socket connectionSocketSend;
    private Socket connectionSocketRecv;
    private String publicKey;
    private int mode = 1; //UNENCRYPTED = 1, ENCRYPTED = 2, ENCRYPTEDWITHSIG = 3;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Socket getConnectionSocketSend() {
        return connectionSocketSend;
    }

    public void setConnectionSocketSend(Socket connectionSocketSend) {
        this.connectionSocketSend = connectionSocketSend;
    }

    public Socket getConnectionSocketRecv() {
        return connectionSocketRecv;
    }

    public void setConnectionSocketRecv(Socket connectionSocketRecv) {
        this.connectionSocketRecv = connectionSocketRecv;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

}

class TCPServer {

    public static ArrayList<ClientInfo> clientInfoList = new ArrayList();

    public static void main(String argv[]) throws Exception {

        ServerSocket welcomeSocketRecv = new ServerSocket(6789);
        ServerSocket welcomeSocketSend = new ServerSocket(6790);
        System.out.println("Server started.  \n-------------------------------\n");
        while (true) {

            Socket connectionSocketSend = welcomeSocketSend.accept();
            Socket connectionSocketRecv = welcomeSocketRecv.accept();

            DataInputStream inFromClientRecv = new DataInputStream(connectionSocketRecv.getInputStream());

            DataOutputStream outToClientRecv = new DataOutputStream(connectionSocketRecv.getOutputStream());

            DataInputStream inFromClientSend = new DataInputStream(connectionSocketSend.getInputStream());

            DataOutputStream outToClientSend = new DataOutputStream(connectionSocketSend.getOutputStream());

            System.out.println("SendServerSocketThreads initiated for client.");
            ServerSocketThreadSend socketThreadSend = new ServerSocketThreadSend(connectionSocketSend, connectionSocketRecv, inFromClientRecv, outToClientRecv);
            Thread threadSend = new Thread(socketThreadSend);
            threadSend.start();

            System.out.println("RecvServerSocketThreads initiated for client.");
            ServerSocketThreadRecv socketThreadRecv = new ServerSocketThreadRecv(connectionSocketSend, connectionSocketRecv, inFromClientSend, outToClientSend);
            Thread threadRecv = new Thread(socketThreadRecv);
            threadRecv.start();

        }
    }
}

class ServerSocketThreadSend implements Runnable {

    Socket connectionSocketSend;
    Socket connectionSocketRecv;
    DataInputStream inFromClient;
    DataOutputStream outToClient;

    ServerSocketThreadSend(Socket connectionSocketSend, Socket connectionSocketRecv, DataInputStream inFromClient, DataOutputStream outToClient) {
        this.connectionSocketSend = connectionSocketSend;
        this.connectionSocketRecv = connectionSocketRecv;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
    }

    public void run() {
        String clientName = "";
        while (true) {
            try {
                String responseSentence = "";
                String clientSentence = "";
                clientSentence = inFromClient.readUTF().trim();
                String[] ar = clientSentence.split("\n");
                outToClient.flush();
                if (clientSentence != null) {
                    String header1 = ar[0].trim();
                    if (header1.startsWith("REGISTER TOSEND ")) {
                        clientName = clientSentence.substring(16, clientSentence.length()).trim();
                        if (isUserNameValid(clientName)) {
                            System.out.println(clientName + " registered to send at port " + connectionSocketRecv.getPort());
                            responseSentence = "REGISTERED TOSEND " + clientName + "\n\n";
                        } else {
                            responseSentence = "ERROR 100 Malformed username " + clientName + "\n\n";
                        }
                        outToClient.writeUTF(responseSentence);
                        outToClient.flush();
                    } else if (header1.startsWith("SEND ") || header1.startsWith("FETCHKEY")) {
                        String recipient;
                        if (header1.startsWith("SEND ")) {
                            recipient = header1.substring(4, header1.length()).trim();
                        } else {
                            recipient = header1.substring(8, header1.length()).trim();
                        }
                        if (isUserNameWellFormed(recipient)) {
                            boolean isUserRegistered = false;
                            for (int i = 0; i < TCPServer.clientInfoList.size(); i++) {
                                ClientInfo objClientInfo = TCPServer.clientInfoList.get(i);
                                if (objClientInfo.getUserName().equalsIgnoreCase(recipient)) {
                                    isUserRegistered = true;
                                    if (header1.startsWith("SEND ")) {
                                        Socket recipientSocket = objClientInfo.getConnectionSocketRecv();

                                        DataOutputStream outToRecipient = new DataOutputStream(recipientSocket.getOutputStream());
                                        DataInputStream inFromRecipient1 = new DataInputStream(recipientSocket.getInputStream());

                                        String msgSentence = "";
                                        String contentLengthSentence = ar[1];
                                        if (contentLengthSentence.startsWith("Content-length:")) {
                                            String contLenStr = contentLengthSentence.substring(15, contentLengthSentence.length()).trim();
                                            try {
                                                int contLength = Integer.parseInt(contLenStr);
                                                for (int it = 2; it < ar.length; it++) {
                                                    msgSentence += ar[it] + "\n";
                                                }
                                                msgSentence = msgSentence.trim();
                                                String recipientSentence = "FORWARD " + clientName + "\n" + "Content-length:" + contLength + "\n\n" + msgSentence;
                                                outToRecipient.writeUTF(recipientSentence);
                                                outToRecipient.flush();
                                                if (objClientInfo.getMode() == 3) {
//                                                    String responseFromRecipient = inFromRecipient1.readUTF();
                                                    for (int j = 0; j < TCPServer.clientInfoList.size(); j++) {
                                                        ClientInfo objSenderInfo = TCPServer.clientInfoList.get(j);
                                                        if (objSenderInfo.getUserName().equalsIgnoreCase(clientName)) {
                                                            String publicKeyc = objSenderInfo.getPublicKey();
                                                            outToRecipient.writeUTF(publicKeyc);
                                                            outToRecipient.flush();
                                                        }
                                                    }
                                                }
                                                String responseFromRecipient = inFromRecipient1.readUTF();
                                                System.out.println("responseFromRecipient:" + responseFromRecipient);
                                                if (responseFromRecipient != null) {
                                                    if (responseFromRecipient.startsWith("RECEIVED ")) {
                                                        responseSentence = "SENT " + recipient + "\n\n";
                                                    } else {
                                                        responseSentence = "ERROR 102 Unable to send" + "\n\n";
                                                    }
                                                }
                                                outToClient.writeUTF(responseSentence);
                                                outToClient.flush();
                                            } catch (NumberFormatException nfe) {
                                                responseSentence = "ERROR 103 Header incomplete" + "\n\n";
                                                outToClient.writeUTF(responseSentence);
                                                outToClient.flush();
                                            }
                                        }
                                    } else {

                                        String publicKey = objClientInfo.getPublicKey();
                                        outToClient.writeUTF(publicKey);
                                        outToClient.flush();
                                    }

                                }
                            }
                            if (isUserRegistered == false) {
                                responseSentence = "ERROR 102 S Unable to send" + "\n\n";
                                outToClient.writeUTF(responseSentence);
                                outToClient.flush();
                            }
                        } else {
                            responseSentence = "Username Invalid" + "\n\n";
                            outToClient.writeUTF(responseSentence);
                            outToClient.flush();
                        }
                    } else {
                        responseSentence = "ERROR 102 S Unable to send" + "\n\n";
                        outToClient.writeUTF(responseSentence);
                        outToClient.flush();
                    }
                }
            } catch (Exception e) {
                try {
                    System.out.println("The client " + clientName + " is closed.");
                    for (int i = 0; i < TCPServer.clientInfoList.size(); i++) {
                        ClientInfo objClientInfo = TCPServer.clientInfoList.get(i);
                        if (objClientInfo.getUserName().equalsIgnoreCase(clientName)) {
                            TCPServer.clientInfoList.remove(i);
                        }
                    }
                    connectionSocketRecv.close();
                    connectionSocketSend.close();
                } catch (Exception ee) {
                }
                break;

            }
        }
    }

    private boolean isUserNameWellFormed(String clientName) {
        return clientName.matches("[a-zA-Z][a-zA-Z0-9]*");
    }

    private boolean isUserNameValid(String clientName) {
        for (int i = 0; i < TCPServer.clientInfoList.size(); i++) {
            ClientInfo objClientInfo = TCPServer.clientInfoList.get(i);
            if (objClientInfo.getUserName().equalsIgnoreCase(clientName)) {
                return false;
            }
        }
        return true;

    }
}

class ServerSocketThreadRecv implements Runnable {

    Socket connectionSocketSend;
    Socket connectionSocketRecv;
    DataInputStream inFromClient;
    DataOutputStream outToClient;

    ServerSocketThreadRecv(Socket connectionSocketSend, Socket connectionSocketRecv, DataInputStream inFromClient, DataOutputStream outToClient) {
        this.connectionSocketSend = connectionSocketSend;
        this.connectionSocketRecv = connectionSocketRecv;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
    }

    public void run() {
        String clientName = "";
        try {
            String responseSentence = "";
            String clientSentence = inFromClient.readUTF();
            String[] ar = clientSentence.split("\n");
            String header1 = ar[0].trim();
            if (header1.startsWith("REGISTER TORECV ")) {
                clientName = header1.substring(16, header1.length()).trim();

                if (isUserNameWellFormed(clientName)) {
                    ClientInfo objClientInfo = new ClientInfo();

                    if (ar.length > 1) {
                        objClientInfo.setPublicKey(ar[1].trim());
                        objClientInfo.setMode(Integer.parseInt(ar[2].trim()));
                    }
                    objClientInfo.setUserName(clientName);
                    objClientInfo.setConnectionSocketSend(connectionSocketRecv);
                    objClientInfo.setConnectionSocketRecv(connectionSocketSend);
                    TCPServer.clientInfoList.add(objClientInfo);

                    System.out.println(clientName + " registered to receive at port " + connectionSocketSend.getPort() + " for sending at port:" + connectionSocketRecv.getPort());
                    responseSentence = "REGISTERED TORECV " + clientName + "\n\n";

                    outToClient.writeUTF(responseSentence);
                    outToClient.flush();
                    printClientInfoList(TCPServer.clientInfoList);
                } else {
                    throw new Exception("ERROR 100 Malformed username " + clientName + "\n\n");
                }
            } else {
                throw new Exception("ERROR 101 No user registered" + "\n\n");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private boolean isUserNameWellFormed(String clientName) {
        return clientName.matches("[a-zA-Z0-9]+");
    }

    private void printClientInfoList(ArrayList<ClientInfo> clientInfoList) {
        for (int i = 0; i < clientInfoList.size(); i++) {
            ClientInfo objClientInfo = clientInfoList.get(i);
            System.out.println("Client:" + objClientInfo.getUserName() + " SendPort:" + objClientInfo.getConnectionSocketSend().getPort() + "  RecvPort:" + objClientInfo.getConnectionSocketRecv().getPort() + " Public Key:" + objClientInfo.getPublicKey());

        }
    }

}
