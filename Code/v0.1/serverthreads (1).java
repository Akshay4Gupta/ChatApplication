package chatapp;

import java.io.*; 
import java.net.*; 
import java.util.ArrayList;

class ClientInfo{
    private String userName;
    private int recSocketNo;
    private int sendSocketNo;
    private String publicKey;
    private String pvtKey;

    public ClientInfo() {
        this.userName = "";
        this.recSocketNo = 0;
        this.sendSocketNo = 0;
        this.publicKey = "";
        this.pvtKey = "";
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getRecSocketNo() {
        return recSocketNo;
    }

    public void setRecSocketNo(int recSocketNo) {
        this.recSocketNo = recSocketNo;
    }

    public int getSendSocketNo() {
        return sendSocketNo;
    }

    public void setSendSocketNo(int sendSocketNo) {
        this.sendSocketNo = sendSocketNo;
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

          BufferedReader inFromClient = 
           new BufferedReader(new
           InputStreamReader(connectionSocketRecv.getInputStream())); 


          DataOutputStream outToClient = 
           new DataOutputStream(connectionSocketSend.getOutputStream()); 

	  SocketThread socketThread = new SocketThread(connectionSocketSend,connectionSocketRecv, inFromClient, outToClient);
          Thread thread = new Thread(socketThread);
          thread.start();  

      }

    } 
} 
 

class SocketThread implements Runnable {
     String clientSentence; 
     String responseSentence; 
     Socket connectionSocketSend;
     Socket connectionSocketRecv;
     BufferedReader inFromClient;
     DataOutputStream outToClient;
   
     SocketThread (Socket connectionSocketSend,Socket connectionSocketRecv, BufferedReader inFromClient, DataOutputStream outToClient) {
	this.connectionSocketSend = connectionSocketSend;
        this.connectionSocketRecv = connectionSocketRecv;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
     } 

     public void run() {
       while(true) { 
	   try {

	           clientSentence = inFromClient.readLine(); 
                   System.out.println(clientSentence);
                   
                   if(clientSentence.trim().startsWith("REGISTER TOSEND ")){
                       String clientName=clientSentence.substring(15, clientSentence.length());
                       if(isUserNameWellFormed(clientName)){
                           
                           System.out.println(clientName+ " registered to send at port "+connectionSocketRecv.getPort());
                           responseSentence="REGISTERED TOSEND "+clientName+'\n';
                          
                            ClientInfo objClientInfo=new ClientInfo();
                            objClientInfo.setUserName(clientName);
                            objClientInfo.setSendSocketNo(connectionSocketRecv.getPort());
                            TCPServer.clientInfoList.add(objClientInfo);
                           
                       }else{
                           responseSentence="ERROR 100 Malformed username "+clientName+'\n';
                       }
                   }else if(clientSentence.trim().startsWith("REGISTER TORECV ")){
                       String clientName=clientSentence.substring(15, clientSentence.length());
                       if(isUserNameWellFormed(clientName)){
                           boolean isUserExist=false;
                           
                           for(int i=0;i<TCPServer.clientInfoList.size();i++){
                               ClientInfo objClientInfo=TCPServer.clientInfoList.get(i);
                               if(objClientInfo.getUserName().equalsIgnoreCase(clientName)){
                                   isUserExist=true;
                                   System.out.println("Client "+clientName+" already registered for send at port "+objClientInfo.getSendSocketNo());
                                   objClientInfo.setRecSocketNo(connectionSocketSend.getPort());
                                   TCPServer.clientInfoList.set(i, objClientInfo);
                                   break;
                               }
                           }
                           if(isUserExist==false){
                                System.out.println("Client "+clientName+" not registered for send yet. ");
                                ClientInfo objClientInfo=new ClientInfo();
                                objClientInfo.setUserName(clientName);
                                objClientInfo.setRecSocketNo(connectionSocketSend.getPort());
                                TCPServer.clientInfoList.add(objClientInfo);
                           }
                           System.out.println(clientName+ " registered to receive at port "+connectionSocketSend.getPort());
                           responseSentence="REGISTERED TOREC "+clientName+'\n';
                       }else{
                           responseSentence="ERROR 100 Malformed username "+clientName+'\n';
                       }
                   }else if(clientSentence.trim().startsWith("SEND ")){
                       
                       
                   }else if(clientSentence.trim().startsWith("ERROR 103")){
                       //do nothing
                       
                   }else if(clientSentence.trim().startsWith("RECEIVED ")){
                       //do nothing
                       
                   }else {
                       
                       responseSentence="ERROR 101 No user registered \n";
                   }
                   
  	           //responseSentence = clientSentence.toUpperCase() + '\n'; 

        	   outToClient.writeBytes(responseSentence); 
                   clientSentence="";
                   responseSentence="";
                   
                   printClientInfoList(TCPServer.clientInfoList);
	   } catch(Exception e) {
		try {
			connectionSocketRecv.close();
                        connectionSocketSend.close();
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
                System.out.println("Client:"+objClientInfo.getUserName()+" SendPort:"+objClientInfo.getSendSocketNo()+"  RecvPort:"+objClientInfo.getRecSocketNo());
                   
            }
    }
    
}

