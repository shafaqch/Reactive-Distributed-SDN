/**
 * Registered Agency List (RAL) Client that works with the Floodlight XTalker module
 * 
 * @author Shafaq Chaudhry
 * Last Updated: November 2020

 */


package net.floodlightcontroller.xtalker;

import java.net.Socket;
//import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Scanner;


public class RALClient2{  
	//private String queryResponse = null;
	private String strSenderResponse = null;
	private String strDestinationResponse = null;
	private Scanner in = null;//new Scanner(sock.getInputStream());
	private PrintWriter out = null;//new PrintWriter(sock.getOutputStream(),true);//true means flush and send
	private Socket sock = null;////
	
	
	public static void main(String[] args) throws Exception { //if using command line
   		//RALClient2 client = new RALClient2("localhost",5999,"10.0.0.1","10.0.1.1");//third argument registers host //192.168.1.242
		/*RALClient2 client = new RALClient2(args[0],Integer.parseInt(args[1]),args[2],args[3]);
		System.out.println(client.getSenderResponse());
		System.out.println(client.getDestinationResponse());*/
		try {
			//RALClient2 client = new RALClient2("192.168.1.242",5999,"10.0.0.9","10.0.1.10");
			RALClient2 client = new RALClient2("192.168.1.242",5999);

			for(int i = 0;i< 10; i++) {
				try {
					System.out.println(i+","+client.queryRALServer("10.0.0."+Integer.toString(i)));
				} catch (Exception e) {
					System.out.println("Error: " + e);
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error: " + e);
		}	
		
		
	}

	public RALClient2(String serverName, int serverPort) {////throws Exception{  
		System.out.println("Establishing connection to RAL Server. Please wait ...");
		
		try {
			Socket sock = new Socket(serverName,serverPort);////
			this.sock = sock;////
			System.out.println("Connected to RAL Server: " + sock);
			//Scanner in = new Scanner(sock.getInputStream());
			//PrintWriter out = new PrintWriter(sock.getOutputStream(),true);//true means flush and send
			in = new Scanner(sock.getInputStream());
			out = new PrintWriter(sock.getOutputStream(),true);//true means flush and send			
		} 
		catch (Exception e) {
			System.out.println("Error: " + e);
		}
	}
	
	public RALClient2(String serverName, int serverPort, String strSenderIp, String strReceiverIp) {////throws Exception{  
		
		
		System.out.println("Establishing connection to RAL Server. Please wait ...");
		
		
		try {
			Socket sock = new Socket(serverName,serverPort);////
			this.sock = sock;////
		//try (Socket socket = new Socket("192.168.1.242",5999)){
			System.out.println("Connected to RAL Server: " + sock);
			//Scanner in = new Scanner(sock.getInputStream());
			//PrintWriter out = new PrintWriter(sock.getOutputStream(),true);//true means flush and send
			in = new Scanner(sock.getInputStream());
			out = new PrintWriter(sock.getOutputStream(),true);//true means flush and send			
			if(strSenderIp != null) { //only testing for sender address right now
				out.println(strSenderIp);
				strSenderResponse = in.nextLine();
				//System.out.println(strSenderResponse);
			}
			if(strReceiverIp != null) { //only testing for sender address right now
				out.println(strReceiverIp);
				strDestinationResponse = in.nextLine();
				//System.out.println(strDestinationResponse);
			}			
			/*if(strReceiverIp == null)
				out.println(strSenderIp);
			else
				out.println(strSenderIp+","+strReceiverIp);//send the ip address of sender and receiver
			queryResponse = in.nextLine();
			System.out.println(queryResponse);*/
			////in.close();////
			////out.close();//
		} 
		catch (Exception e) {
			System.out.println("Error: " + e);
		}
		/*////finally {
			try {
				in.close();
				out.close();
				sock.close(); 
				
			} 
			catch (IOException ioe) {
				System.out.println("Error: " + ioe);
			}
			System.out.println("Closed socket: " + sock);
		}*////
	}

	
	public String getSenderResponse() {
		return strSenderResponse;
	}
	public String getDestinationResponse() {
		return strDestinationResponse;
	}
	
	
	public String queryRALServer(String strIPaddr) {
		//if(strIPaddr != null) { //only testing for sender address right now
			out.println(strIPaddr);
			strSenderResponse = in.nextLine();
			return strSenderResponse;
			//System.out.println(strSenderResponse);
		//}
	}
	protected void finalize() {  //needs to be explicitly called
		try {
			in.close();
			out.close();
			sock.close(); 	
		} 
		catch (IOException ioe) {
			System.out.println("Error: " + ioe);
		}
		System.out.println("Closed socket: " + sock);		
		System.out.println("Object is destroyed by the Garbage Collector");  
	}  
	
}

	