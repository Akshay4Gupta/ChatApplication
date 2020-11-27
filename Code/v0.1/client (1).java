package chatapp;


import java.io.*; 
import java.net.*; 

class TCPClient { 

    
    public static void main(String argv[]) throws Exception 
    { 
        String sentence; 
        String modifiedSentence; 
        System.out.println("client started.\n--------------------\n");
      
        BufferedReader inFromUser = 
          new BufferedReader(new InputStreamReader(System.in)); 

        Socket clientSocketSend = new Socket("localhost", 6789); 

        DataOutputStream outToServerSend = 
          new DataOutputStream(clientSocketSend.getOutputStream()); 

        
//        BufferedReader inFromServerSend = 
//          new BufferedReader(new
//          InputStreamReader(clientSocketSend.getInputStream())); 
        
        Socket clientSocketRecv = new Socket("localhost", 6790); 

//        DataOutputStream outToServerRecv = 
//          new DataOutputStream(clientSocketRecv.getOutputStream()); 

        
        BufferedReader inFromServerRecv = 
          new BufferedReader(new
          InputStreamReader(clientSocketRecv.getInputStream())); 
        
        while(true) {
             //send part
             sentence = inFromUser.readLine(); 
             outToServerSend.writeBytes(sentence + '\n'); 
             
             //recv part
             modifiedSentence = inFromServerRecv.readLine(); 
             System.out.println("FROM SERVER: " + modifiedSentence); 
            
        }

//        clientSocket.close(); 
                   
    } 
} 

