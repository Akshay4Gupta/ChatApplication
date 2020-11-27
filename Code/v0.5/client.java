

import java.io.*; 
import static java.lang.Thread.sleep;
import java.net.*;
import java.util.*;

class TCPClient { 

    static int UNENCRYPTED=1,ENCRYPTED=2,ENCRYPTEDWITHSIG=3;
        
    public static void main(String argv[]) throws Exception {
        Scanner input = new Scanner(System.in);
        System.out.println("Client Application started.\n----------------------------------\nEnter the UserName:");
        String userName=input.next();
        String serverIP="localhost";
        System.out.println("Enter the mode you want to encrypt your chat(write the corrosponding code)::\n1) UNENCRYPTED\n2) ENCRYPTED\n3) ENCRYPTED WITH SIGNATURE\n::");
        int mode=input.nextInt();
        
        int serverPortSend=6789;
        int serverPortRecv=6790;

        System.out.println("UserName:"+userName);

        Socket clientSocketSend = new Socket(serverIP, serverPortSend); 
        DataOutputStream outToServerSend = new DataOutputStream(clientSocketSend.getOutputStream()); 
        DataInputStream inFromServerSend = new DataInputStream(clientSocketSend.getInputStream()); 
        
        
        Socket clientSocketRecv = new Socket(serverIP, serverPortRecv); 
        DataOutputStream outToServerRecv = new DataOutputStream(clientSocketRecv.getOutputStream()); 
        DataInputStream inFromServerRecv = new DataInputStream(clientSocketRecv.getInputStream()); 
        
        System.out.println("Sending registration msg.." );

        String sendRegSentence = "REGISTER TOSEND "+userName+"\n\n";
        outToServerSend.writeUTF(sendRegSentence); 
        outToServerSend.flush();
        String responseSendRegSentence = inFromServerSend.readUTF(); 
        System.out.println("FROM SERVER(reg send response):\t" + responseSendRegSentence);
        
        String recvRegSentence = "REGISTER TORECV "+userName+"\n\n"; 
        outToServerRecv.writeUTF(recvRegSentence); 
        outToServerRecv.flush();
        String responseRecvRegSentence = inFromServerRecv.readUTF(); 
        System.out.println("FROM SERVER(reg recv response):\t" + responseRecvRegSentence);
        
        try{
            if(responseSendRegSentence.startsWith("REGISTERED TOSEND ")){
                CLSocketThreadSend  socketThreadSend= new CLSocketThreadSend(userName,clientSocketSend, inFromServerSend, outToServerSend);
                Thread thread1 = new Thread(socketThreadSend);
                thread1.start();
            }else{
                throw new Exception("User Can't be registered to send\n");
            }
            if(responseRecvRegSentence.startsWith("REGISTERED TORECV")){
                CLSocketThreadRead socketThreadRecv = new CLSocketThreadRead(userName,clientSocketRecv, inFromServerRecv, outToServerRecv, inFromServerSend, outToServerSend);
                Thread thread2 = new Thread(socketThreadRecv);
                thread2.start(); 
            }else{
                throw new Exception("User can't be registered to recieve\n");
            }
        }catch(Exception e){
            System.out.println(e);
        }
//        clientSocket.close(); 
    } //end main
    
} //end class TCPClient


class CLSocketThreadSend implements Runnable {
    String userName;
    Socket connectionSocketSend;
    Socket connectionSocketRecv;
    DataInputStream inFromServer;
    DataOutputStream outToServer;
   
    CLSocketThreadSend (String userName,Socket connectionSocketSend, DataInputStream inFromServer, DataOutputStream outToServer) {
        this.userName=userName;
        this.connectionSocketSend = connectionSocketSend;
        this.inFromServer = inFromServer;
        this.outToServer = outToServer;
    } 

    public void run() {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                String inSentence="", outSentence="", userInput = "", text;
                while(!((text = inFromUser.readLine()).isEmpty())){
                    userInput += text+"\n";
                }
                userInput = userInput.trim();
                if(userInput.startsWith("@") && userInput.contains(":") ){
                    String msg=userInput.substring(userInput.indexOf(":")+1, userInput.length()).trim();
                    int contentLength=msg.length();
                    String recipientName=userInput.substring(userInput.indexOf("@")+1, userInput.indexOf(":"));
                    outSentence="SEND "+recipientName+"\n"+"Content-length: "+contentLength+"\n\n"+msg;
//                    System.out.println("Send Sentence:"+outSentence);
                    outToServer.writeUTF(outSentence); 
                    outToServer.flush();
                    inSentence = inFromServer.readUTF().trim();
//                    System.out.println("ack recieved\n");
                    if(inSentence.startsWith("SENT ") || inSentence.startsWith("ERROR 103 ")){
                        System.out.println(inSentence);
                    }else{
                        System.out.println("ERROR 102 Unable to send\n\n");
                    }
                }else {
                    System.out.println("TYPE AGAIN. Correct Message format is @[recipient username]:[message]");
                }
            }catch(Exception e){
                System.out.println(e);
            }
        }
    }//end run
}//end class

class CLSocketThreadRead implements Runnable {
    String userName;
    Socket connectionSocketSend;
    Socket connectionSocketRecv;
    DataInputStream inFromServer;
    DataOutputStream outToServer;
    DataInputStream inFromServer1;
    DataOutputStream outToServer1;

    CLSocketThreadRead (String userName, Socket connectionSocketRecv, DataInputStream inFromServer, DataOutputStream outToServer,DataInputStream inFromServerSend,DataOutputStream outToServerSend) {
        this.userName=userName;
        this.connectionSocketRecv = connectionSocketRecv;
        this.inFromServer = inFromServer;
        this.outToServer = outToServer;
        this.inFromServer1 = inFromServerSend;
        this.outToServer1 = outToServerSend;
    } 

    public void run() {
        while(true) {
            try {
                String inSentence="";
                String outSentence="";
                inSentence = inFromServer.readUTF();
//                System.out.println("insentence::"+inSentence);
                String[] ar=inSentence.split("\n");
                outToServer.flush();
                if(inSentence!=null){
                    if(ar[0].trim().startsWith("FORWARD")){
                        String senderName=ar[0].substring(7, ar[0].length()).trim();
                        String msgSentence="";
                        String contentLengthSentence = ar[1];
                        if(contentLengthSentence.startsWith("Content-length:")){
                            String contLenStr=contentLengthSentence.substring(15, contentLengthSentence.length());
                            try{
                                int contLength=Integer.parseInt(contLenStr);
                                for(int it = 2; it<ar.length;it++){
                                    msgSentence += ar[it]+"\n";
                                }
                                msgSentence = msgSentence.trim();
                                System.out.println("#"+senderName+":"+msgSentence+"\n");
                                outSentence="RECEIVED "+userName+"\n\n";
//                                System.out.println(":<1?>:"+outSentence+"\n");
                                outToServer.writeUTF(outSentence); 
//                                System.out.println(":<2?>:\n");
                                outToServer.flush();
                            }catch(NumberFormatException nfe){
                                outSentence="ERROR 103 Header incomplete"+"\n\n";
                                outToServer.writeUTF(outSentence); 
                                outToServer.flush();
                            }
                        }else{
                            outSentence="ERROR 103 Header incomplete"+"\n\n";
                            outToServer.writeUTF(outSentence); 
                            outToServer.flush();
                        }
                    }else if(inSentence.trim().startsWith("ERROR")){
                        System.out.println("ERROR MSG FROM SERVER:"+inSentence);
                    }
                }
            }catch(Exception e){
                try {
                    System.out.println("Exception in handling forward data:"+e.getCause());
                    break;
                }catch(Exception ee){
                    System.out.println(ee);
                }
                
            }
        }
    }//end run



     
}//end class
