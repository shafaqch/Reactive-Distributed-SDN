/**
 * Registered Agency List (RAL) Server that works with the Floodlight XTalker module's RAL Client
 * 
 * @author Shafaq Chaudhry
 * Last Updated: November 2020

 */


package ralserver;

import java.net.Socket;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.Scanner;

public class RALServer {
	
	private static Map<String, String> registeredAgencyList = new HashMap<String, String>();
	
	public static void registerAgency(String host, String agencyName) {
		//System.out.println("registering agency "+host+" "+agencyName);
		registeredAgencyList.put(host, agencyName);		
	}
	
	public static void unregisterAgency(String host) {
		System.out.println("unregistering agency "+host);
		registeredAgencyList.remove(host);   
	}
	
	public static String getAgencyByHost(String host) {	
		String mappedvalue = registeredAgencyList.get(host);
		
		if (mappedvalue == null)
			return "Unregistered";
		else {
			//System.out.println("mapped value is "+  mappedvalue);
			return mappedvalue;
		}
		//return registeredAgencyList.get(host);//returns null when the map contains no such mapping for the key
	}	
		
	
	private static void registerAgencies() {
   		/*registerAgency("10.0.0.1", "AgencyA-h1");//register h1
   		registerAgency("10.0.0.2", "AgencyA-h2");//register h2
   		*/
		String hostnameinA = "10.0.0.";
		String hostnameinB = "10.0.1.";
		String hostnametemp = "";
		for (int i = 1; i <= 10; i++) {
			hostnametemp = hostnameinA+Integer.toString(i);
			registerAgency(hostnametemp,hostnametemp);
			hostnametemp = hostnameinB+Integer.toString(i);
			registerAgency(hostnametemp,hostnametemp);
			
		}
   		/*registerAgency("10.0.0.1", "10.0.0.1");//register h1
   		registerAgency("10.0.0.2", "10.0.0.2");//register h2
   		*/   		
	}
	
	public static void main(String[] args) throws Exception {
		//register agencies
		registerAgencies();
		
		//wait for clients
		try(ServerSocket listener = new ServerSocket(5999)) {
		
			System.out.println("Waiting for a client listening on port 5999...");
			
			// get available processors
			//int coreCount = Runtime.getRuntime().availableProcessors(); //8
			//System.out.println(coreCount);
			//for CPU intensive tasks, create a pool with as many threads as the available processors or less depending upon what other apps are running
			//for IO intensive tasks, you can have a bigger pool, too many threads in the wait state increases memory consumption 
			var pool = Executors.newFixedThreadPool(20);//use a pool of n threads to avoid resource issues
			
			while (true) { //continue listening for connections
				Socket sock = listener.accept(); //server socket accepts connection 
				pool.execute(new RALregister(sock));// and establishes a new thread from a thread pool
			}
			
		}
	}
	
	private static class RALregister implements Runnable { //what runs in the thread, implements Runnable interface
		private Socket socket;
		
		RALregister(Socket socket){ //capture arguments in the constructor
			this.socket = socket;
		}
		
		@Override
		public void run() { //thread task's work goes here
			System.out.println("Connected: " + socket);
			try {
				//wrap socket input and output streams in a Scanner and PrintWriter so the bytes get converted to strings
				Scanner sockIn = new Scanner(socket.getInputStream());
				PrintWriter sockOut = new PrintWriter(socket.getOutputStream(),true);
				
				while (sockIn.hasNextLine()) {
					//get the next line
					String strQuery = sockIn.nextLine();
					System.out.println("Received query: "+ strQuery);
					String strResponse = getAgencyByHost(strQuery);
					
					if(strResponse != null)
						sockOut.println(strResponse);
					//String[] addresses = strQuery.split(",");
					
					//find if agency is registered
					/*
					for(int i = 0 ; i<addresses.length; i++) {
						if(i==0)
							strResponse += ",";
						if(getAgencyByHost(addresses[i]) != null)
							 strResponse += strResponse;
						//if(strResonse == null)
					
					}*/
					//sockOut.println(strResponse);
				}
				sockIn.close();
				sockOut.close();
			} 
			catch (Exception e) {
				System.out.println("Error: " + socket);
			} 
			finally { //finally block closes the socket
				try {
					socket.close(); 
				} 
				catch (IOException ioe) {
					System.out.println("Error: " + ioe);
				}
				System.out.println("Closed: " + socket);
			}
		}
	}
}
