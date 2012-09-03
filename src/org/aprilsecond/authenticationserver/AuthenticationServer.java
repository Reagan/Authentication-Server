package org.aprilsecond.authenticationserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * <p>
 * This class creates a minimal server application that 
 * waits for an authorization code from the Google Calendar servers.
 * It then passes this code to the application to complete the 
 * authorization process if the user grants access.
 * </p>
 * @author Reagan Mbitiru <reaganmbitiru@gmail.com>
 */
public class AuthenticationServer {

    /**
     * stores instance of the current server
     */
    private static AuthenticationServer thisInstance ;
    
    /**
     * stores PORT for server
     */
    private int serverPort ;
    
    /**
     * stores the server socket object
     */
    private ServerSocket server ;
    
    /**
     * stores the number of clients that can connect to the
     * server
     */
    private static final int QUEUE_LENGTH = 1 ;
    
    /**
     * stores the connection to the client
     */
    private Socket connection; 
    
    /**
     * stores number of clients connected to the server
     */
    private int counter = 1; 

    /**
     * stores output stream to client
     */
    private DataOutputStream output; // output stream to client
    /**
     * stores input stream from client
     */
    private BufferedReader input; // input stream from client
    
    /**
     * stores whether or not a connection has been processed
     */
    private boolean isConnectionProcessed = false; 
    
    /**
     * stores the authorization code
     */
    public String authorizationCode ;
    
    /**
     * constructor starts server at a specific port
     */
    private AuthenticationServer() {
        
    }
    
    /**
     * uses singleton design to initialize and start the server
     */
    public static AuthenticationServer getInstance() {
        
        // create instance of server
        thisInstance = new AuthenticationServer() ;
        
        return thisInstance ;
    }
    
    /**
     * starts the server
     * @param port
     * @return 
     */
    public boolean startServer(int port) {
        
        // stores whether the server has been 
        // started or not
        boolean isServerStarted = false ; 
        
        // sets the server port
        serverPort = port ;
        
        try {
            // initialize the server component
            server = new ServerSocket(serverPort, QUEUE_LENGTH) ;
            
        } catch (IOException ex) {
           System.out.println("Error starting the server : "
                   + ex.toString());
           return isServerStarted ;
        }
        
        // start the server
        try // set up server to receive connections; process connections
        {
            while (!isConnectionProcessed) {
                try {
                    waitForConnection(); // wait for a connection
                    getStreams(); // get input & output streams
                    processConnection(); // process connection
                } // end try
                catch (EOFException eofException) {
                    System.out.println("\nServer terminated connection : "
                            + eofException.toString());
                } 
                finally {
                    stopServer(); // close connection
                    counter++;
                } 
            } 
        } 
        catch (IOException ioException) {
            System.out.println("Error waiting for streams : "
                    + ioException.toString()) ;
        } 
        
        // return start state
        return isServerStarted ;
    }
    
    /**
     * waits for connection to arrive, then display connection info
     */    
    private void waitForConnection() throws IOException {
        System.out.println("Waiting for connection\n");
        connection = server.accept() ;
        System.out.println("Connection " + counter + " received from: " +
                connection.getInetAddress().getHostName());
    } 
    
    /**
     * get streams to send and receive data
     */
    private void getStreams() {
        try {
            // set up output stream to client
            output = new DataOutputStream(connection.getOutputStream()) ;
        } catch (IOException ex) {
            System.out.println("Error setting up output stream : " 
                    +ex.toString());
        }
        
        // set up input stream from client
        try {           
            input = new BufferedReader(new InputStreamReader(connection.getInputStream()));            
        } catch (IOException ex) {
            System.out.println("Error setting up the input stream :" +
                    ex.toString());
        }
    } 

    /**
     * <p>
     * processes connection with client and extracts the 
     * authorization code from the received URL.
     * </p>
     * <p>
     * Although a number of headers are submitted by the browser, 
     * the header of concern has a structure similar to  
     * <pre>GET /?code=12389 HTTP/1.1</pre>. 
     * This is stripped to obtain just the authorization code from it
     * </p>
     */
    private void processConnection() throws IOException {
        String message = "ASRemind successfully received authorization [Auth Code :";
        
        // extract the authorization code
        String authCodeString  ;
        
        while((authCodeString = input.readLine()) != null) {
            if (authCodeString.length() == 0) break ;
            
           if(authCodeString.startsWith("GET /?code=")) {
                // get the section after the '='
                String[] result = authCodeString.split("=") ;
                
                // get the section after the 'HTTP'
                String [] re1 = result[1].split("HTTP") ;
                
                // return the message string
                authorizationCode = re1[0] ;
                message += re1[0];
           }
        }
        
        message += "].\nPlease close this browser window :)" ;
        sendData(message); // send connection successful message
    }

    // close streams and socket
    private void stopServer() {
        System.out.println("\nTerminating connection");
        
        try {
            output.close();
            input.close();
            connection.close();
        } 
        catch (IOException ioException) {
            System.out.println("Error occured closing the "
                    + "server : "  + ioException.toString());
        } 
    } 

    /**
     * sends message to browser
     * @param message 
     */
    private void sendData(String message) {
        try {
            
            // send the headers
            output.writeBytes("HTTP/1.0 200 \n");
            output.writeBytes("Content-Type: text/html\n");
            output.writeBytes("\n");
            output.flush();
            
            // send the information to the browser
            output.writeBytes("<html><head><title>"
                    + "Authorization Granted</title></head>"
                    + "<body>") ;
            output.writeBytes("SERVER >> "  + message
                    + "</body></html>");
            output.writeBytes("\n");
            output.flush();            
            
            // set state to show that a connection
            // has been processed
            isConnectionProcessed = true ;
            
        } catch (IOException ioException) {
            System.out.println("\nError writing object to the "
                    + "browser:" 
                    + ioException.toString());
        } 
    }
    
    /**
     * tests the server
     */
    public static void main(String[] args) {
        int port = 65500 ;
        AuthenticationServer server 
                = AuthenticationServer.getInstance();
        server.startServer(port) ;
    }
}