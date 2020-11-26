package chatapp;

import java.io.*; 
import static java.lang.Thread.sleep;
import java.net.*; 
import java.util.ArrayList;

class ClientInfo{
    private String userName;
    private Socket connectionSocketSend;
    private Socket connectionSocketRecv;
    private String publicKey;
    private String pvtKey;

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

    public String getPvtKey() {
        return pvtKey;
    }

    public void setPvtKey(String pvtKey) {
        this.pvtKey = pvtKey;
    }
}

class TCPServer {
    public static ArrayList<ClientInfo> clientInfoList=new ArrayList();

    public static void main(String argv[]) throws Exception { 

        ServerSocket welcomeSocketRecv = new ServerSocket(6789);
        ServerSocket welcomeSocketSend = new ServerSocket(6790);
        System.out.println("Server started.  \n--------------------\n");
        while(true) { 

            Socket connectionSocketSend = welcomeSocketSend.accept(); 
            Socket connectionSocketRecv = welcomeSocketRecv.accept();

            BufferedReader inFromClientRecv = new BufferedReader(new InputStreamReader(connectionSocketRecv.getInputStream())); 

            DataOutputStream outToClientRecv = new DataOutputStream(connectionSocketRecv.getOutputStream()); 

            BufferedReader inFromClientSend = new BufferedReader(new InputStreamReader(connectionSocketSend.getInputStream())); 

            DataOutputStream outToClientSend = new DataOutputStream(connectionSocketSend.getOutputStream()); 


            System.out.println("SendServerSocketThreads initiated for client.");
            ServerSocketThreadSend socketThreadSend = new ServerSocketThreadSend(connectionSocketSend,connectionSocketRecv, inFromClientRecv, outToClientRecv);
            Thread threadSend = new Thread(socketThreadSend);
            threadSend.start();  

            // System.out.println("RecvServerSocketThreads initiated for client.");
            // ServerSocketThreadRecv socketThreadRecv = new ServerSocketThreadRecv(connectionSocketSend,connectionSocketRecv, inFromClientSend, outToClientSend);
            // Thread threadRecv = new Thread(socketThreadRecv);
            // threadRecv.start();  

        }
    } 
} 

class ServerSocketThreadSend implements Runnable {

    Socket connectionSocketSend;
    Socket connectionSocketRecv;
    BufferedReader inFromClient;
    DataOutputStream outToClient;

    ServerSocketThreadSend (Socket connectionSocketSend,Socket connectionSocketRecv, BufferedReader inFromClient, DataOutputStream outToClient) {
        this.connectionSocketSend = connectionSocketSend;
        this.connectionSocketRecv = connectionSocketRecv;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
    } 

    public void run() {
        String clientName="";
        while(true) {
            String responseSentence="";
            String clientSentence ="";
            while((clientSentence = inFromClient.readLine()).isEmpty());
            outToClient.flush();
            if(clientSentence!=null ) {
                if(clientSentence.trim().startsWith("REGISTER TOSEND ")){
                    clientName=clientSentence.substring(15, clientSentence.length());
                    if(isUserNameWellFormed(clientName)){
                        System.out.println(clientName+ " registered to send at port "+connectionSocketRecv.getPort());
                        responseSentence="\n"+"REGISTERED TOSEND "+clientName+"\n\n";
                    }else{
                        responseSentence="\n"+"ERROR 100 Malformed username "+clientName+"\n\n";
                    }
                    outToClient.writeBytes(responseSentence); 
                    outToClient.flush();
                }
            }else if(clientSentence.trim().startsWith("SEND ")){
                
            }
        }
    }
    private boolean isUserNameWellFormed(String clientName) {
        return clientName.trim().matches("[a-zA-Z0-9]+");
    }
}