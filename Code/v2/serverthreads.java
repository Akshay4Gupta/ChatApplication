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

  public static void main(String argv[]) throws Exception 
    { 

        ServerSocket welcomeSocketRecv = new ServerSocket(6789);
        ServerSocket welcomeSocketSend = new ServerSocket(6790);
        System.out.println("Server started.  \n--------------------\n");
        while(true) { 

            Socket connectionSocketSend = welcomeSocketSend.accept(); 
            Socket connectionSocketRecv = welcomeSocketRecv.accept(); 

//            BufferedReader inFromClient = 
//             new BufferedReader(new
//             InputStreamReader(connectionSocketRecv.getInputStream())); 

            BufferedReader inFromClientRecv = new BufferedReader(new InputStreamReader(connectionSocketRecv.getInputStream())); 

          DataOutputStream outToClientRecv = new DataOutputStream(connectionSocketRecv.getOutputStream()); 
            
            

//            DataOutputStream outToClient = 
//             new DataOutputStream(connectionSocketSend.getOutputStream()); 
            
            BufferedReader inFromClientSend = 
           new BufferedReader(new
           InputStreamReader(connectionSocketSend.getInputStream())); 

          DataOutputStream outToClientSend = 
           new DataOutputStream(connectionSocketSend.getOutputStream()); 
            
            
          System.out.println("SendServerSocketThreads initiated for client.");
            ServerSocketThreadSend socketThreadSend = new ServerSocketThreadSend(connectionSocketSend,connectionSocketRecv, inFromClientRecv, outToClientRecv);
            Thread threadSend = new Thread(socketThreadSend);
            threadSend.start();  
          
            System.out.println("RecvServerSocketThreads initiated for client.");
            ServerSocketThreadRecv socketThreadRecv = new ServerSocketThreadRecv(connectionSocketSend,connectionSocketRecv, inFromClientSend, outToClientSend);
            Thread threadRecv = new Thread(socketThreadRecv);
            threadRecv.start();  

        }
    } 
} 
 

class ServerSocketThreadSend implements Runnable {
     //String clientSentence; 
     //String responseSentence; 
     Socket connectionSocketSend;
     Socket connectionSocketRecv;
     BufferedReader inFromClient;
     DataOutputStream outToClient;
    //String clientName="";
    
     ServerSocketThreadSend (Socket connectionSocketSend,Socket connectionSocketRecv, BufferedReader inFromClient, DataOutputStream outToClient) {
	this.connectionSocketSend = connectionSocketSend;
        this.connectionSocketRecv = connectionSocketRecv;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
     } 

     public void run() {
       String clientName="";
       while(true) { 
	   try {
                   String responseSentence="";
                   String clientSentence ="";
	           //String clientSentence = inFromClient.readLine(); 
                   
                   while((clientSentence = inFromClient.readLine()).isEmpty()){
                       
                   }
                   outToClient.flush();
                   if(clientSentence!=null )  {      
                        System.out.println("In ServerSocketThreadSend, first clientSentence:"+clientSentence);

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
                            
                        }else if(clientSentence.trim().startsWith("SEND ")){
                            System.out.println("Received send msg from client:"+clientSentence);
                            String recipient=clientSentence.substring(4, clientSentence.length());
                            if(isUserNameWellFormed(recipient)){
                                boolean isUserRegistered=false;
                                for(int i=0;i<TCPServer.clientInfoList.size();i++){
                                     ClientInfo objClientInfo=TCPServer.clientInfoList.get(i);
                                     if(objClientInfo.getUserName().equalsIgnoreCase(recipient)){
                                         isUserRegistered=true;
                                         Socket recipientRecSocket=objClientInfo.getConnectionSocketRecv();
                                         Socket recipientSendSocket=objClientInfo.getConnectionSocketSend();
                                         String msgSentence="";
                                         String contentLengthSentence = inFromClient.readLine(); 
                                         System.out.println("contentLengthSentence:"+contentLengthSentence);
                                         if(contentLengthSentence.startsWith("Content-length:")){
                                            String contLenStr=contentLengthSentence.substring(15, contentLengthSentence.length());
                                            try{
                                                int contLength=Integer.parseInt(contLenStr);
                                                char cbuf[]=new char[contLength+1];
                                                inFromClient.read(cbuf, 0, contLength+1);
                                                msgSentence=String.valueOf(cbuf).trim();
                                                System.out.println("msgSentence:"+msgSentence);
                                                
                                                String recipientSentence="FORWARD "+clientName+"\n"+"Content-length:"+contLength+"\n\n"+msgSentence;
                                                System.out.println("recipientSentence:"+recipientSentence);
                                                DataOutputStream outToRecipient = new DataOutputStream(recipientRecSocket.getOutputStream()); 
                                                BufferedReader inFromRecipient = new BufferedReader(new InputStreamReader(recipientSendSocket.getInputStream()));
                                                //DataInputStream dis=new DataInputStream(recipientSendSocket.getInputStream());
                                                System.out.println("recipientRecSocketNo:"+recipientRecSocket.getPort()+" recipientSendSocketNo:"+recipientSendSocket.getPort()); 
                                                
                                                outToRecipient.writeBytes(recipientSentence);
                                                outToRecipient.flush();
                                                
                                                 String responseFromRecipient=inFromRecipient.readLine();
                                                 //String responseFromRecipient=dis.readUTF();
                                                 System.out.println("responseFromRecipient:"+responseFromRecipient);
                                                 if(responseFromRecipient!=null){
                                                    if(responseFromRecipient.startsWith("RECEIVED ")){
                                                        responseSentence="SENT "+recipient+"\n"+"\n\n";
                                                    }else{
                                                        responseSentence="ERROR 102 Unable to send"+"\n"+"\n\n";
                                                    }
                                                 }

                                                 responseSentence="SENT "+recipient+"\n"+"\n\n";
                                                 outToClient.writeBytes(responseSentence);
                                                 outToClient.flush();
                                            }catch(NumberFormatException nfe){
                                                 responseSentence="ERROR 103 Header incomplete"+"\n"+"\n\n";
                                                 outToClient.writeBytes(responseSentence);
                                                 outToClient.flush();
                                            }
                                           
                                         }else{
                                             responseSentence="ERROR 103 Header incomplete"+"\n"+"\n\n";
                                             outToClient.writeBytes(responseSentence);
                                             outToClient.flush();
                                         }
                                     }
                                 }//end for
                                 if(isUserRegistered==false){
                                     responseSentence="ERROR 102 Unable to send"+"\n"+"\n\n";
                                     outToClient.writeBytes(responseSentence);
                                     outToClient.flush();
                                 }
                            }else{
                                responseSentence="ERROR 102 Unable to send"+"\n"+"\n\n";
                                outToClient.writeBytes(responseSentence);
                                outToClient.flush();
                            }
                            
                        }else if(clientSentence.trim().startsWith("ERROR 103")){
                            //do nothing
                            System.out.println(clientSentence);

                        }else if(clientSentence.trim().startsWith("RECEIVED ")){
                            //do nothing
                            System.out.println(clientSentence);
                        }else {

                            responseSentence="ERROR 101 No user registered"+"\n"+"\n\n";
                            outToClient.writeBytes(responseSentence); 
                            outToClient.flush();
                        }
                   
                   }else{
                       //client goes down
                       System.out.println("Client "+clientName+" goes down. Going to close socket.");
                       connectionSocketRecv.close();
                       connectionSocketSend.close();
                       break;
                   }  
	   } catch(Exception e) {
		try {
			//connectionSocketRecv.close();
                        //connectionSocketSend.close();
		} catch(Exception ee) { }
		break;
	   }
        } 
    }

    private boolean isUserNameWellFormed(String clientName) {
        return clientName.trim().matches("[a-zA-Z0-9]+");
    }
    
    
}

class ServerSocketThreadRecv implements Runnable {
     //String clientSentence; 
     //String responseSentence; 
     Socket connectionSocketSend;
     Socket connectionSocketRecv;
     BufferedReader inFromClient;
     DataOutputStream outToClient;
   
     ServerSocketThreadRecv (Socket connectionSocketSend,Socket connectionSocketRecv, BufferedReader inFromClient, DataOutputStream outToClient) {
	this.connectionSocketSend = connectionSocketSend;
        this.connectionSocketRecv = connectionSocketRecv;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
     } 

     public void run() {
       while(true) { 
           String clientName="";
	   try {

                   String responseSentence="";
	           String clientSentence = inFromClient.readLine(); 
                   
                  
                        System.out.println("in ServerSocketThreadRecv,first clientSentence:"+clientSentence);


                        if(clientSentence.trim().startsWith("REGISTER TORECV ")){
                            clientName=clientSentence.substring(15, clientSentence.length());
                            if(isUserNameWellFormed(clientName)){
                                 ClientInfo objClientInfo=new ClientInfo();
                                  objClientInfo.setUserName(clientName);
                                  objClientInfo.setConnectionSocketSend(connectionSocketRecv);
                                  objClientInfo.setConnectionSocketRecv(connectionSocketSend);
                                TCPServer.clientInfoList.add(objClientInfo);

                                System.out.println(clientName+ " registered to receive at port "+connectionSocketSend.getPort()+" for sending at port:"+connectionSocketRecv.getPort());
                                responseSentence="REGISTERED TORECV "+clientName+"\n"+"\n\n";

                                outToClient.writeBytes(responseSentence); 
                                outToClient.flush();


                                 printClientInfoList(TCPServer.clientInfoList);
                                 break;

                            }else{
                                responseSentence="ERROR 100 Malformed username "+clientName+"\n"+"\n\n";
                            }

                        }else {

                            responseSentence="ERROR 101 No user registered"+"\n"+"\n\n";
                        }
                        outToClient.writeBytes(responseSentence); 
                        outToClient.flush();
                   
	   } catch(Exception e) {
		try {
			//connectionSocketSend.close();
                        //connectionSocketRecv.close();
		} catch(Exception ee) { }
		break;
	   }
        } 
    }

    private boolean isUserNameWellFormed(String clientName) {
        return clientName.trim().matches("[a-zA-Z0-9]+");
    }
    
    private void printClientInfoList(ArrayList<ClientInfo> clientInfoList){
        for(int i=0;i<clientInfoList.size();i++){
                ClientInfo objClientInfo=clientInfoList.get(i);
                System.out.println("Client:"+objClientInfo.getUserName()+" SendPort:"+objClientInfo.getConnectionSocketSend().getPort()+"  RecvPort:"+objClientInfo.getConnectionSocketRecv().getPort());
                   
            }
    }
    
}
