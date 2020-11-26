package chatapp;


import java.io.*; 
import static java.lang.Thread.sleep;
import java.net.*; 

class TCPClient { 

    static int UNENCRYPTED=1,ENCRYPTED=2,ENCRYPTEDWITHSIG=3;
        
    public static void main(String argv[]) throws Exception 
    { 
        String userName="pks";
        String serverIP="localhost";
        int mode=UNENCRYPTED;
        
        int serverPortSend=6789;
        int serverPortRecv=6790;
                
//        String sentence; 
//        String modifiedSentence; 
        System.out.println("Client Application started.\n--------------------\n");
        System.out.println("UserName:"+userName);
        
//        BufferedReader inFromUser = 
//          new BufferedReader(new InputStreamReader(System.in)); 

        Socket clientSocketSend = new Socket(serverIP, serverPortSend); 
        DataOutputStream outToServerSend = 
          new DataOutputStream(clientSocketSend.getOutputStream()); 
        DataInputStream inFromServerSend = 
          new DataInputStream(clientSocketSend.getInputStream()); 
        
        
        Socket clientSocketRecv = new Socket(serverIP, serverPortRecv); 
        DataOutputStream outToServerRecv = 
          new DataOutputStream(clientSocketRecv.getOutputStream()); 
        DataInputStream inFromServerRecv = 
          new DataInputStream(clientSocketRecv.getInputStream()); 
        
        //while(true) {
             
             System.out.println("Sending registration msg.." );
        
              //reg send part
             String sendRegSentence = "REGISTER TOSEND "+userName+"\n"+"\n\n"; 
             //System.out.println("before sendRegSentence:"+sendRegSentence);
             outToServerSend.writeUTF(sendRegSentence); 
             outToServerSend.flush();
             String responseSendRegSentence = inFromServerSend.readUTF(); 
             System.out.println("FROM SERVER(reg send response ): " + responseSendRegSentence);
             
             sleep(1000);
             
             //reg recv part
             String recvRegSentence = "REGISTER TORECV "+userName+"\n"+"\n\n"; 
             outToServerRecv.writeUTF(recvRegSentence); 
             outToServerRecv.flush();
             String responseRecvRegSentence = inFromServerRecv.readUTF(); 
             System.out.println("FROM SERVER(reg recv response ): " + responseRecvRegSentence);
        
             
             
//             sentence = inFromUser.readLine(); 
//             outToServerSend.writeBytes(sentence); 
//             String responseRecvRegSentence = inFromServerRecv.readLine();
//             outToServerSend.flush();
       // }

        

        if(responseRecvRegSentence.startsWith("REGISTERED TORECV")){
              System.out.println("Opening socketThreadSend");
              CLSocketThreadSend  socketThreadSend= new CLSocketThreadSend(userName,clientSocketSend, inFromServerSend, outToServerSend);
              Thread thread1 = new Thread(socketThreadSend);
              thread1.start(); 

              System.out.println("Opening socketThreadRecv");
              CLSocketThreadRead socketThreadRecv = new CLSocketThreadRead(userName,clientSocketRecv, inFromServerRecv, outToServerRecv);
              Thread thread2 = new Thread(socketThreadRecv);
              thread2.start(); 
        }
        
        //        clientSocket.close(); 
    } //end main
    
} //end class TCPClient


class CLSocketThreadSend implements Runnable {
    String userName;
//     String inSentence; 
//     String outSentence; 
     Socket connectionSocketSend;
     //Socket connectionSocketRecv;
     DataInputStream inFromServer;
     DataOutputStream outToServer;
   
     CLSocketThreadSend (String userName,Socket connectionSocketSend, DataInputStream inFromServer, DataOutputStream outToServer) {
	this.userName=userName;
        this.connectionSocketSend = connectionSocketSend;
        //this.connectionSocketRecv = connectionSocketRecv;
        this.inFromServer = inFromServer;
        this.outToServer = outToServer;
     } 

     public void run() {
         BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
         while(true) { 
	   try {
                     String inSentence=""; 
                     String outSentence="";
                     String userInput = "", text;
                     while(!((text = inFromUser.readLine()).isEmpty())){
                        userInput += text+"\n";
                    }
//                   System.out.println("userinput::"+userInput);
                    outToServer.flush();
//                   // buffer for storing file contents in memory
//                    StringBuffer stringBuffer = new StringBuffer("");
//                    // for reading one line
//                    String line = null;
//                    // keep reading till readLine returns null
//                    while ((line = inFromUser.readLine()) != null) {
//                        // keep appending last line read to buffer
//                        stringBuffer.append(line+"\n");
//                    }
//                   String userInput =stringBuffer.toString();
                   
                   if(userInput.trim().startsWith("@") && userInput.trim().contains(":") ){
//                       System.out.println("if condition entered\n");
                        String msg=userInput.trim().substring(userInput.indexOf(":")+1, userInput.trim().length());
                        int contentLength=msg.length();
                        String recipientName=userInput.substring(userInput.indexOf("@")+1, userInput.indexOf(":"));
                        outSentence="SEND "+recipientName+"\n"+"Content-length:"+contentLength+"\n\n"+msg;
                        System.out.println("Send Sentence:"+outSentence);
                        outToServer.writeUTF(outSentence); 
                        outToServer.flush();
                        inSentence = inFromServer.readUTF(); 
                        System.out.println("FROM SERVER:"+inSentence);
                        
                    }else {
                       System.out.println("TYPE AGAIN. Correct Message format is @[recipient username][message]");
                   }
                   
  	           //responseSentence = clientSentence.toUpperCase() + '\n'; 

             }catch(Exception e) {
		try {
                        System.out.println("Exception in send data:"+e.getCause());
			//connectionSocketRecv.close();
                        //connectionSocketSend.close();
		} catch(Exception ee) { }
		break;
	   }//end try-catch
        }//end while
     }//end run
}//end class

class CLSocketThreadRead implements Runnable {
    String userName;
//     String inSentence; 
//     String outSentence; 
     //Socket connectionSocketSend;
     Socket connectionSocketRecv;
     DataInputStream inFromServer;
     DataOutputStream outToServer;
   
     CLSocketThreadRead (String userName, Socket connectionSocketRecv, DataInputStream inFromServer, DataOutputStream outToServer) {
            this.userName=userName;
         //this.connectionSocketSend = connectionSocketSend;
        this.connectionSocketRecv = connectionSocketRecv;
        this.inFromServer = inFromServer;
        this.outToServer = outToServer;
     } 

     public void run() {
       while(true) { 
	   try {

                    String inSentence=""; 
                    String outSentence=""; 
	           inSentence = inFromServer.readUTF(); 
                   System.out.println("insentence::"+inSentence);
                   String[] ar=inSentence.split("\n");
                    outToServer.flush();
                   //System.out.println("In CLSocketThreadRead:"+inSentence);
                   if(inSentence!=null){
                        if(ar[0].trim().startsWith("FORWARD")){
                             String senderName=ar[0].substring(7, ar[0].length());
                             String msgSentence="";
                             String contentLengthSentence = ar[1]; 
                             if(contentLengthSentence.startsWith("Content-length:")){
                                String contLenStr=contentLengthSentence.substring(15, contentLengthSentence.length());
                                try{
                                    int contLength=Integer.parseInt(contLenStr);
                                    System.out.println("sontent length::"+contLength);
                                     msgSentence=ar[3];
                                     System.out.println("#"+senderName.trim()+":"+msgSentence);

                                     outSentence="RECEIVED "+userName+"\n"+"\n\n";
                                     //System.out.println("outSentence:"+outSentence);
                                     outToServer.writeUTF(outSentence); 
                                     outToServer.flush();

                                }catch(NumberFormatException nfe){
                                     outSentence="ERROR 103 Header incomplete"+"\n"+"\n\n";
                                     outToServer.writeUTF(outSentence); 
                                     outToServer.flush();
                                }

                             }else{
                                 outSentence="ERROR 103 Header incomplete"+"\n"+"\n\n";
                                 outToServer.writeUTF(outSentence); 
                                 outToServer.flush();
                             }

                        }else if(inSentence.trim().startsWith("ERROR")){
                            //do nothing print server error msg
                            System.out.println("ERROR MSG FROM SERVER:"+inSentence);
                        }
                   }//end if
             }catch(Exception e) {
		try {
                        System.out.println("Exception in handling forward data:"+e.getCause());
			//connectionSocketRecv.close();
                       // connectionSocketSend.close();
		} catch(Exception ee) { }
		break;
	   }
        }//end while
     }//end run
     
     
     
     
}//end class
