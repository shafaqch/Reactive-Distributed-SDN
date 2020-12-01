package net.floodlightcontroller.xtalker;
//import java.io.IOException;
//import java.net.UnknownHostException;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collection;
//import java.util.Iterator;
//import java.util.List;
import java.util.Map;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
//import org.projectfloodlight.openflow.protocol.OFMatch;
import org.projectfloodlight.openflow.protocol.OFMessage;
//import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
//import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
//import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
//import org.projectfloodlight.openflow.protocol.action.OFActionStripVlan;
import org.projectfloodlight.openflow.protocol.action.OFActions;
//import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
//import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
//import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
//import org.projectfloodlight.openflow.protocol.match.Match.Builder;
import org.projectfloodlight.openflow.protocol.match.MatchField;
//import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
//import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
//import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
//import org.projectfloodlight.openflow.types.VlanVid;
//import org.projectfloodlight.openflow.util.HexString;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
//import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
//import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.projectfloodlight.openflow.protocol.OFStatsRequest;
//import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;

/**
 * XTalker: Reactive SDN application (Floodlight module) to grant access to data in another agency's network registered in the Registered Agency List (RAL)
 * 
 * @author Shafaq Chaudhry
 * Last Updated: November 2020

 */


public class XTalker implements IFloodlightModule, IOFMessageListener {
	//register member variables
	public final int DEFAULT_CACHE_SIZE = 10;
	protected IFloodlightProviderService floodlightProvider;
	//protected static Logger logger;//logger to output
	protected static Logger log = LoggerFactory.getLogger(XTalker.class);
	//private IStaticFlowEntryPusherService flowPusher;
	private static String RALServerIP;
	private static int  RALServerPort;
	private static int flowRuleExpirationTimer;
	private static int flowRuleHardTimer;
	private static int priorityAllow;
	private static int priorityDeny;
	private static RALClient2 client;
	private static double countPacketIns;
	private static double countFlowAdds;	
	//private static int countUDPPackets;
	//private static int countTCPPackets;	
	
	@Override
	public String getName() {//Need an ID in our OFMessage listener
		return "CrossAgencyTalker";//return XTalker.class.getSimpleName();
	}
	
	/**
	 *  IOFMessageListener's isCallbackOrderingPrereq(OFType type, String name) and isCallbackOrderingPostreq(OFType type, String name) functions
	 *  define the order of modules that process packets received from a switch.
	 *  Each OpenFlow message received, e.g. a PACKET_IN, is processed by one module at a time
	 *  so that modules can pass metadata from one to another about the packet or terminate the processing chain entirely.
	 *  isCallbackOrderingPrereq() defines modules that should run before our module when processing a particular type of OpenFlow message.
	 *  isCallbackOrderingPostreq() defines modules that should run after our module when processing a particular type of OpenFlow message.
	 *  https://floodlight.atlassian.net/wiki/spaces/floodlightcontroller/pages/1343513/How+to+Write+a+Module
	 */
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}
	
	@Override
		public boolean isCallbackOrderingPostreq(OFType type, String name) {//defines modules that should run after our module
		////return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));
		/**
		 * Forwarding module implements layer-2 reactive packet forwarding
		 * since Forwarding will insert flows and modify the network state
		 * then we need to tell Forwarding to run after our module, thus we should use isCallbackOrderingPostreq().
		 */
		return false;
	}
	
	//This is where we pull fields from the packet-in
	/**
	  * The controller will invoke the receive()
	  * function automatically when an OFMessage is
	  * received from a switch if the module
	  * implements the IOFMessageListener interface
	  * and is registered with the controller as an
	  * IOFMessageListener
	  * https://floodlight.atlassian.net/wiki/spaces/floodlightcontroller/pages/1343547/How+to+use+OpenFlowJ-Loxigen
	 * @throws Exception 
	  **/	
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {//defines the behavior for PACKET_IN messages. the Command.CONTINUE allows this message to continue to be handled by other PACKET_IN handlers
				
	   switch (msg.getType()) { 
	      case PACKET_IN:
	    	  /* Retrieve the de-serialized packet in message */
			  Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			
		     /* Various getters and setters are exposed in Ethernet */
		     MacAddress srcMac = eth.getSourceMACAddress();
		     MacAddress destMac = eth.getDestinationMACAddress();
		     //VlanVid vlanId = VlanVid.ofVlan(eth.getVlanID());	
		     //System.out.println("getEtherType "+eth.getEtherType().toString()+" EthType.IPv4 "+EthType.IPv4.toString());
		     /* 
		      * Check the ethertype of the Ethernet frame and retrieve the appropriate payload.
		      * Note the shallow equality check. EthType caches and reuses instances for valid types.
		      */
		     countPacketIns++; 
		     log.info("PACKET_IN in xTalker (count:"+countPacketIns+")from sw "+sw.getId().toString() +" of ethType "+ eth.getEtherType().toString());
		     if (eth.getEtherType() == EthType.IPv4) //IPv4 is 0x800
			  {
				  /* We got an IPv4 packet; get the payload from Ethernet */
				  IPv4 ipv4 = (IPv4) eth.getPayload();
	          
				  /* Various getters and setters are exposed in IPv4 */
				  //byte[] ipOptions = ipv4.getOptions();
				  IPv4Address srcIp = ipv4.getSourceAddress();
				  IPv4Address dstIp = ipv4.getDestinationAddress();
				  
				  String srcIpAddress = srcIp.toString();	
				  String destIpAddress = dstIp.toString();
				  log.info("sw="+sw.getId().toString() +" payload="+ ipv4.getProtocol().toString()+" srcIp="+srcIpAddress+" dstIp="+destIpAddress);
		         //Check IP protocol
		         if (ipv4.getProtocol() == IpProtocol.TCP) //TCP is 0x6
		         {
		             /* We got a TCP pafloodlightcket; get the payload from IPv4 */
		        	 //System.out.println("we got a TCP payload");//+Integer.toString(++countTCPPackets));
		             TCP tcp = (TCP) ipv4.getPayload();
			
		             /* Various getters and setters are exposed in TCP */
		             TransportPort srcPort = tcp.getSourcePort();
		             TransportPort dstPort = tcp.getDestinationPort();
			             
			         //short flags = tcp.getFlags();
	            	 
		             //System.out.println("$$$$$-Get the Source IP -$$$$$ " + srcIpAddress);
		             //System.out.println("$$$$$-Get the Destination IP -$$$$$ "+destIpAddress);
		             //System.out.println("$$$$$-Get the TCP Source Port-$$$$$ "+srcPort.toString());
		             //System.out.println("$$$$$-Get the TCP Destination Port-$$$$$ "+ dstPort.toString());
			             
		             //assume all TCP traffic is blocked
		             //if destination ip address is part of RAL, then forward as normal
		             //else block
		             //example:- h4 is listening on TCP and host h3 is listening on TCP but h3 is not in RAL,
		             //so, wget request h1->h4 will be forwarded while wget request h2->h3 will be blocked
			             
		             boolean isDestRegistered = false;
		             boolean isSrcRegistered = false;

	            	 if(client.queryRALServer(srcIpAddress).equals(srcIpAddress)) {
	            		isSrcRegistered = true;
	            		if (client.queryRALServer(destIpAddress).equals(destIpAddress)) 
	            			isDestRegistered = true;
	            		else 
	            			log.info("Destination is not registered " + destIpAddress); 
	            	 }
	            	 else {
	            		log.info("Source is not registered " + srcIpAddress); 
	            	 }
	            	 
	            	 /*RALClient2 client = new RALClient2(RALServerIP,RALServerPort,srcIpAddress,destIpAddress);
		             String sourceFound = client.getSenderResponse();
		             String destFound = client.getDestinationResponse();
		             
		             if (sourceFound.equals(srcIpAddress)) {
		            	 isSrcRegistered = true;
		            	 log.info("Source is registered " + srcIpAddress);
		            	 //System.out.println("sourceFound matches srcIpAddress " + sourceFound);
		             }
		             else {
		            	 log.info("Source is not registered " + srcIpAddress);
		            	 System.out.println("sourceFound "+sourceFound+" does not match srcIpAddress "+srcIpAddress);
		             }
		             if (destFound.equals(destIpAddress)) {
		            	 isDstRegistered = true;
		            	 log.info("Destination is registered " + destIpAddress);
		            	 //System.out.println("destFound matches destIpAddress " + destFound);
		             }
		             else {
		            	 log.info("Destination is not registered " + destIpAddress);
		            	 System.out.println("destFound "+destFound+" does not match destIpAddress "+destIpAddress);
		             }	 */           
		             /*RALClient2 client = null;
		     		//String destIpAddress = ipv4.getDestinationAddress().toString();
		     		//String srcIpAddress = ipv4.getSourceAddress().toString();
		     		
		       		//client = new RALClient2("192.168.1.183",5999,destIpAddress);
		     		System.out.println("$$$$$-connecting with:"+RALServerIP+":"+RALServerPort);
		     		client = new RALClient2(RALServerIP,RALServerPort,srcIpAddress,destIpAddress)
		     		//client = new RALClient2(RALServerIP,RALServerPort,destIpAddress);
		       		//String clientResponse = null;
		       		//clientResponse = client.getQueryResponse();
		       		
		       		if(client.getQueryResponse().equals("Unregistered"))
		       			System.out.println("too bad, the destination" + destIpAddress + " is not registered");
		       		else //(clientResponse != null)
		       		{
		       			System.out.println("great, the destination " +destIpAddress+ " is registered");
		       			isDstRegistered = true;
		       		}
		       		client.bye();
			       		
		       		//client = new RALClient2("192.168.1.183",5999,srcIpAddress);
		       		client = new RALClient2(RALServerIP,RALServerPort,srcIpAddress);

		       		clientResponse = client.getQueryResponse();
		       		
		       		if(clientResponse.equals("Unregistered"))
		       			System.out.println("too bad, the source" + srcIpAddress + " is not registered");
		       		else //(clientResponse != null)
		       		{
		       			System.out.println("great, the source " + srcIpAddress + " is registered");
		       			isSrcRegistered = true;
		       		}
					*/
		       		//if( ipv4.getDestinationAddress().toString().equals("10.0.1.2") &&
		            	//	 ipv4.getSourceAddress().toString().equals("10.0.0.1"))	 	
		            if(isDestRegistered && isSrcRegistered)// && (srcPort.toString().equals("9000") || dstPort.toString().equals("9000")) )	//TCP only allow DITG signaling
		       		{
		            	 //System.out.println("$$$$$-ALLOW-$$$$$ ");
		            	 OFFactory myOF10Factory = sw.getOFFactory();			            	 
		            	 OFVersion detectedVersion = myOF10Factory.getVersion();

		            	 switch (detectedVersion.toString()) 
		            	 {
	            	 		case "OF_10":
	            	 			//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString());
	            	 		
	            	 			ArrayList<OFAction> actionList = new ArrayList<OFAction>();
	            	 			OFActions actions = myOF10Factory.actions();
				            	 
				            	
				            	OFActionOutput output = actions.buildOutput()
				            	     //.setMaxLen(0xFFffFFff)
				            	    .setPort(OFPort.NORMAL)//process as L2/L3 learning switch
				            	    .build();
				            	actionList.add(output);				            	 
					            //System.out.println("$$$$$-actionsList built-$$$$$ ");
					             
				            	/* RULE 1 */
				            	//create a match object
								Match myMatch = myOF10Factory.buildMatch()
									 .setExact(MatchField.ETH_TYPE, EthType.IPv4)
									 .setExact(MatchField.IPV4_SRC,IPv4Address.of(srcIpAddress))
									 .setExact(MatchField.IPV4_DST,IPv4Address.of(destIpAddress))
									 .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
									 .setExact(MatchField.TCP_SRC, srcPort)
									 .setExact(MatchField.TCP_DST, dstPort)
									 .build();	
								//System.out.println("$$$$$-match object built-$$$$$ ");
								 
								//compose an OFFlowAdd to allow traffic
								OFFlowAdd flowAdd = myOF10Factory.buildFlowAdd()
								 	.setMatch(myMatch)
								    .setBufferId(OFBufferId.NO_BUFFER)
								    .setHardTimeout(flowRuleHardTimer) //remove optional timer for DITG signaling
								    .setIdleTimeout(flowRuleExpirationTimer)
								    .setPriority(priorityAllow)//mid priority 32768
								    .setActions(actionList)			    
								    .build();
								 //System.out.println("$$$$$-Rule 1 composed-$$$$$ Add SRC:"+srcIpAddress+" DST:"+destIpAddress+", PROTO=TCP, srcPort:"+srcPort.toString()+", dstPort:"+dstPort.toString());
								 sw.write(flowAdd);
								 //System.out.println("$$$$$-TCP Rule Allow sent to switch-$$$$$ Add SRC:"+srcIpAddress+" DST:"+destIpAddress+", PROTO=TCP, srcPort:"+srcPort.toString()+", dstPort:"+dstPort.toString());
								 countFlowAdds++;
								 log.info("$$$$$-TCP ALLOW Rule 1 sent (countFlowAdds:"+countFlowAdds+")-$$$$$ Add SRC:"+srcIpAddress+" DST:"+destIpAddress+", PROTO=TCP, srcPort:"+srcPort.toString()+", dstPort:"+dstPort.toString());
								 //System.out.println("$$$$$-Rule 1 sent to switch -$$$$$ ");
									 
								 /* RULE 2 */
				            	 /*//create a match object
								 Match myMatch2 = myOF10Factory.buildMatch()
									 .setExact(MatchField.ETH_TYPE, EthType.IPv4)
									 .setExact(MatchField.IPV4_SRC,IPv4Address.of(destIpAddress))
									 .setExact(MatchField.IPV4_DST,IPv4Address.of(srcIpAddress))
									 .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
									 .setExact(MatchField.TCP_SRC, dstPort)
									 .setExact(MatchField.TCP_DST, srcPort)
									 .build();	
								 //System.out.println("$$$$$-match object built-$$$$$ ");
								 //log.info("$$$$$-match object built-$$$$$ ");
								 
								 //compose an OFFlowAdd to allow traffic
								 OFFlowAdd flowAdd2 = myOF10Factory.buildFlowAdd()
								 	.setMatch(myMatch2)
								    .setBufferId(OFBufferId.NO_BUFFER)
								    .setHardTimeout(flowRuleHardTimer) //remove optional timer for DITG signaling
								    .setIdleTimeout(flowRuleExpirationTimer)
								    .setPriority(priorityAllow)//mid priority 32768
								    .setActions(actionList)
								    .build();
								 //System.out.println("$$$$$-TCP Rule 2 composed-$$$$$ Add SRC:"+destIpAddress+", DST:"+srcIpAddress+",PROTO=TCP,srcPort:"+dstPort.toString()+", dstPort:"+srcPort.toString());
								 sw.write(flowAdd2);
								 log.info("$$$$$-TCP ALLOW Rule 2 composed-$$$$$ Add SRC:"+destIpAddress+", DST:"+srcIpAddress+",PROTO=TCP,srcPort:"+dstPort.toString()+", dstPort:"+srcPort.toString());
								 //System.out.println("$$$$$-Rule 2 sent to switch -$$$$$ ");
								*/
								 
								 /* Rule 3 */ //remove Rule 3
								 /*Match myMatch3 = myOF10Factory.buildMatch()
									 .setExact(MatchField.ETH_TYPE, EthType.IPv4)
									 .setExact(MatchField.IP_PROTO, IpProtocol.TCP)//match all TCP flows
									 .build();	
								 //System.out.println("$$$$$-match object built-$$$$$ ");									 
								 OFFlowAdd flowAdd3 = myOF10Factory.buildFlowAdd()
								 	.setMatch(myMatch3)
								    .setBufferId(OFBufferId.NO_BUFFER)
								    .setHardTimeout(flowRuleHardTimer)
								    .setIdleTimeout(flowRuleExpirationTimer)
								    .setPriority(priorityDeny)//low priority
								    //.setActions(actionList) //commenting this out results in default action of drop
								    .build();
								 //System.out.println("$$$$$-Rule 3 composed-$$$$$ Drop Prot:TCP");
								 //sw.write(flowAdd3);
								 //System.out.println("$$$$$-Rule 3 sent to switch -$$$$$ ");
								*/
				            	 break;
		            	 	case "OF_13":
		            	 		//TODO support OF 1.3 later
		            	 		log.info("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
		            	 		//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
		            	 		break;
		            	 	default:
		            	 		log.info("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
		            	 		//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
		            	 		break;
		            	}									 
									
		       		}			            
			        else //if not registered
			        {
			            //add flow rule in switch 1 to deny traffic
			        	//System.out.println("$$$$$-TCP DENY-$$$$$ ");
			        	log.info("$$$$$-TCP DENY-$$$$$ ");
			            	 
			            //OF 1.0
			            OFFactory myOF10Factory = sw.getOFFactory();
			            	 
			            OFVersion detectedVersion = myOF10Factory.getVersion();
			            //System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString());
			            switch (detectedVersion.toString()) 
			            {
			            	case "OF_10":
			             		//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString());
					           	//log.info("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString());
					          	//create a match object
								Match myMatch = myOF10Factory.buildMatch()
									 .setExact(MatchField.ETH_TYPE, EthType.IPv4)
									 .setExact(MatchField.IPV4_SRC,IPv4Address.of(srcIpAddress))
									 .setExact(MatchField.IPV4_DST,IPv4Address.of(destIpAddress))											 
									 .setExact(MatchField.IP_PROTO, IpProtocol.TCP)
									 .setExact(MatchField.TCP_DST, dstPort)
									 .setExact(MatchField.TCP_SRC, srcPort)
									 .build();	
								//System.out.println("$$$$$-match object built-$$$$$ ");
								//log.info("$$$$$-match object built-$$$$$ ");
								
								//compose an OFFlowAdd message to drop TCP traffic
								OFFlowAdd flowAdd = myOF10Factory.buildFlowAdd()
								 	.setMatch(myMatch)
								    .setBufferId(OFBufferId.NO_BUFFER)
								    .setHardTimeout(flowRuleHardTimer)
								    .setIdleTimeout(flowRuleExpirationTimer)//seconds
								    .setPriority(priorityDeny)//
								    .setMatch(myMatch)
								    //.setActions(actionList)//not setting this value results in actions=drop
								    .build();
								 //System.out.println("$$$$$-Rule 4 composed-$$$$$ Drop SRC:"+srcIpAddress+", DST:"+destIpAddress+", PROTO:TCP, srcPort:"+srcPort.toString()+", dstPort:"+dstPort.toString());	 
								 log.info("$$$$$-Rule 4 composed-$$$$$ Drop SRC:"+srcIpAddress+", DST:"+destIpAddress+", PROTO:TCP, srcPort:"+srcPort.toString()+", dstPort:"+dstPort.toString());
								 sw.write(flowAdd);
								 //System.out.println("$$$$$-Rule 4 sent to switch -$$$$$ ");
								 //log.info("$$$$$-Rule 4 sent to switch -$$$$$ "); 
				            	 break;
		            	 	case "OF_13":
		            	 		//TODO support OF 1.3
		            	 		//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
		            	 		log.info("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
		            	 		break;
		            	 	default:
		            	 		//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
		            	 		log.info("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
		            	 		break;
			            }//END SWITCH
			            				            	 
			         }//end else for source/destination not registered

		         } // end of IpProtocol.TCP
		         else if (ipv4.getProtocol() == IpProtocol.UDP) { //UDP is 0x11
		        	 //System.out.println("$$$$$-we got a UDP packet-$$$$$ ");//+Integer.toString(++countUDPPackets));
		             /* We got a UDP packet; get the payload from IPv4 */
		             UDP udp = (UDP) ipv4.getPayload();
	
		             /* Various getters and setters are exposed in UDP */
		             //TransportPort srcPort = udp.getSourcePort();
		             //TransportPort dstPort = udp.getDestinationPort();
		              
		             /* Your logic here! */
		             TransportPort srcPort = udp.getSourcePort();
		             TransportPort dstPort = udp.getDestinationPort();
		             
		             //System.out.println("$$$$$-Source:"+srcIpAddress+":"+srcPort.toString()+", Destination:"+destIpAddress+":"+dstPort.toString());
		             boolean isDestRegistered = false;
		             boolean isSrcRegistered = false;
		             

	            	 if(client.queryRALServer(srcIpAddress).equals(srcIpAddress)) {
	            		isSrcRegistered = true;
	            		if (client.queryRALServer(destIpAddress).equals(destIpAddress)) 
	            			isDestRegistered = true;
	            		else 
	            			log.info("Destination is not registered " + destIpAddress); 
	            	 }
	            	 else {
	            		log.info("Source is not registered " + srcIpAddress); 
	            	 }
		             
	            	 /*RALClient2 client = new RALClient2(RALServerIP,RALServerPort,srcIpAddress,destIpAddress);
		             String sourceFound = client.getSenderResponse();
		             String destFound = client.getDestinationResponse();
		             
		             if (sourceFound.equals(srcIpAddress)) {
			            	 isSrcRegistered = true;
			            	 log.info("Source is registered " + srcIpAddress);
			            	 //System.out.println("sourceFound matches srcIpAddress " + sourceFound);
			             }
			             else {
			            	 log.info("Source is not registered " + srcIpAddress);
			            	 System.out.println("sourceFound "+sourceFound+" does not match srcIpAddress "+srcIpAddress);
			             }
			             if (destFound.equals(destIpAddress)) {
			            	 isDestRegistered = true;
			            	 log.info("Destination is registered " + destIpAddress);
			            	 //System.out.println("destFound matches destIpAddress " + destFound);
			             }
			             else {
			            	 log.info("Destination is not registered " + destIpAddress);
			            	 System.out.println("destFound "+destFound+" does not match destIpAddress "+destIpAddress);
			             }
		            }*/
		            
		             
		             /*
		             // Check if source and destination are registered with RAL
		             RALClient2 client = null;
		             // Check if destination is registered with RAL
		             //client = new RALClient2("192.168.1.183",5999,dstIpString);
		             System.out.println("$$$$$-connecting with:"+RALServerIP+":"+RALServerPort);
		             client = new RALClient2(RALServerIP,RALServerPort,destIpAddress);
		             String clientResponse = null;
		             clientResponse = client.getQueryResponse();

		             if(clientResponse == null) 
		            	 System.out.println("Unable to talk to RAL server");
		             else if(clientResponse.equals("Unregistered"))
		            	 System.out.println("the destination" + destIpAddress + " is not registered");
		             else //(clientResponse != null)
		             {
		            	 System.out.println("the destination host " +destIpAddress+ " is registered");
		            	 isDstRegistered = true;
			             // Check if source is registered with RAL
		            	 RALClient2 client2 = new RALClient2(RALServerIP,RALServerPort,srcIpAddress);
			             String client2Response = null;
			             client2Response = client2.getQueryResponse();
			             if(clientResponse == null) 
			            	 System.out.println("Unable to talk to RAL server");			             
			             else if(client2Response.equals("Unregistered"))
			            	 System.out.println("the source IP " + srcIpAddress + " is not registered");
			             else //(clientResponse != null)
			             {
			            	 System.out.println("the source IP  " + srcIpAddress + " is registered");
			            	 isSrcRegistered = true;
			            	 client2.bye();//added on 10/6	
			             }
			             client.bye();
		             }
		             */
		             // If source and destination are registered then generate ALLOW rules
		             // UDP traffic only has one ALLOW rule from source to destination uni-directional
		             if(isDestRegistered && isSrcRegistered)	//UDP
		             {
		            	 //System.out.println("$$$$$-ALLOW-$$$$$ ");
		            	 OFFactory myOF10Factory = sw.getOFFactory();			            	 
		            	 OFVersion detectedVersion = myOF10Factory.getVersion();
		            	 switch (detectedVersion.toString()) 
		            	 {
		            	 	case "OF_10":
		            	 		//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString());
	            	 		
	            	 			ArrayList<OFAction> actionList = new ArrayList<OFAction>();
	            	 			OFActions actions = myOF10Factory.actions();
	            	 			OFActionOutput output = actions.buildOutput()
					            	    //.setMaxLen(0xFFffFFff)
					            	    .setPort(OFPort.NORMAL)//process as L2/L3 learning switch
					            	    .build();
	            	 			actionList.add(output);				            	 
	            	 			//System.out.println("$$$$$-actionsList built-$$$$$ ");
	            	 			log.debug("$$$$$-actionsList built-$$$$$ ");
					            	
	            	 			/* RULE 1 */ //to send from source to destination
					            	
	            	 			//create a match object
	            	 			Match myMatch = myOF10Factory.buildMatch()
										 .setExact(MatchField.ETH_TYPE, EthType.IPv4)
										 .setExact(MatchField.IPV4_SRC,IPv4Address.of(srcIpAddress))
										 .setExact(MatchField.IPV4_DST,IPv4Address.of(destIpAddress))
										 .setExact(MatchField.IP_PROTO, ipv4.getProtocol())
										 .setExact(MatchField.UDP_SRC, srcPort)
										 .setExact(MatchField.UDP_DST, dstPort)
										 //.setExact(MatchField.ETH_SRC, srcMac)////
										 //.setExact(MatchField.ETH_DST, destMac)////
										 .build();	
	            	 			//System.out.println("$$$$$-match object built-$$$$$ ");
	            	 			log.debug("$$$$$-match object built-$$$$$ ");
								
	            	 			//compose an OFFlowAdd to allow traffic
								OFFlowAdd flowAdd = myOF10Factory.buildFlowAdd()
									 	.setMatch(myMatch)
									    .setBufferId(OFBufferId.NO_BUFFER)
									    .setHardTimeout(flowRuleHardTimer)
									    .setIdleTimeout(flowRuleExpirationTimer)
									    .setPriority(priorityAllow)//mid priority 32768
									    .setActions(actionList)			    
									    .build();
								//System.out.println("$$$$$-UDP Rule 1, switch: "+sw.getId().toString()+", src:"+srcIpAddress+", dst:"+destIpAddress+", srcPort:"+srcPort+", dstPort:"+dstPort+", prot:"+ipv4.getProtocol());
								
								sw.write(flowAdd);
								countFlowAdds++;
								log.info("UDP Rule 1 sent to switch (countFlowAdds:"+countFlowAdds+"): "+sw.getId().toString()+", src:"+srcIpAddress+", dst:"+destIpAddress+", srcPort:"+srcPort+", dstPort:"+dstPort+", prot:"+ipv4.getProtocol());
								/*
								//https://www2.cs.duke.edu/courses/fall14/compsci590.4/notes/slides_floodlight_updated.pdf
								//query stats from controller to switch through OFStatisticsRequest message and receive OfStatistics message as a reply
								//wildcards matching all flows/ports on switch
								OFFlowStatisticsRequest specificReq = new OFFlowStatisticsRequest();
								specificReq.setMatch(new OFMatch().setWildcards(OFMatch.OFPFW_ALL));
								specificReq.setOutput(OFPort.OFPP_NONE.getValue());
								List<OFStatistics> specificReqs = new ArrayList<OFstatistics>();
								specificReqs.add(specificReq);
								//type of statistics we are interested in, i.e. FLOW
								OFStatisticsRequest req = new OFStatisticsRequest();
								req.setStatisticsRequestType(OFStatisticsType.FLOW);
								req.setStatistics(specificReqs);
								int reqLen = req.getLengthU();
								reqLen += specificReq.getLength();
								//send request and get response
								Future<List<OFStatistics>> = future = sw.queryStatistics(req);
								List<OFStatistics> values = future.get(10,TimeUnit.SECONDS);
								for (OFStatistics stat : values ) {
									if (stat instanceof OFFlowStatisticsReply) {
										OFFlowStatisticsReply flowstat = (OFFlowStatisticsReply)stat
										//add logic
									}
								}
								*/
								
								//System.out.println("$$$$$-Rule 1 sent to switch -$$$$$ ");
								log.debug("$$$$$-Rule 1 sent to switch -$$$$$ ");
			             
								break;
		            	 	case "OF_13":
		            	 		//TODO support OF 1.3 later
		            	 		//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
		            	 		log.info("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
		            	 		break;
		            	 	default:		         
			            		//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
			            		log.info("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
			            		break;
		            	 } // end switch (detectedVersion.toString()) 						 
		             } // end (isDstRegistered && isSrcRegistered)		
		             else //if not registered
		             {
		            	 //add flow rule in switch 1 to deny traffic
		            	 //System.out.println("$$$$$-DENY-$$$$$ ");
				         log.info("$$$$$-UDP DENY-$$$$$ ");   	 
				         //OF 1.0
				         OFFactory myOF10Factory = sw.getOFFactory();
				         OFVersion detectedVersion = myOF10Factory.getVersion();
				         //System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString());
				         switch (detectedVersion.toString())
				         {
				         	case "OF_10":
				         		//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString());
					           	log.info("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString());
					           	
				         		//create a match object
				         		Match myMatch = myOF10Factory.buildMatch()
									.setExact(MatchField.ETH_TYPE, EthType.IPv4)
									.setExact(MatchField.IPV4_SRC,IPv4Address.of(srcIpAddress))
									.setExact(MatchField.IPV4_DST,IPv4Address.of(destIpAddress))											 
									.setExact(MatchField.IP_PROTO, ipv4.getProtocol())
									.setExact(MatchField.UDP_DST, dstPort)
									.setExact(MatchField.UDP_SRC, srcPort)
									.setExact(MatchField.ETH_SRC, srcMac)////
									.setExact(MatchField.ETH_DST, destMac)////
									.build();	
				         		//System.out.println("$$$$$-match object built-$$$$$ ");
				         		log.info("$$$$$-match object built-$$$$$ ");
				         		//compose an OFFlowAdd message to drop TCP traffic
				         		OFFlowAdd flowAdd = myOF10Factory.buildFlowAdd()
								 	.setMatch(myMatch)
								    .setBufferId(OFBufferId.NO_BUFFER)
								    .setHardTimeout(flowRuleHardTimer)
								    .setIdleTimeout(flowRuleExpirationTimer)//seconds
								    .setPriority(priorityDeny)//low priority 2
								    .setMatch(myMatch)
								    //.setActions(actionList)//not setting this value results in actions=drop
								    .build();
				         		
				         		//System.out.println("$$$$$-Rule 4 composed-$$$$$ Drop source:"+srcIpAddress+":"+srcPort+" dst:"+destIpAddress+":"+dstPort+", prot:"+ipv4.getProtocol());			 
				         		log.info("$$$$$-Rule 4 composed-$$$$$ Drop source:"+srcIpAddress+":"+srcPort+" dst:"+destIpAddress+":"+dstPort+", prot:"+ipv4.getProtocol());
				         		
				         		sw.write(flowAdd);
				         		
				         		//System.out.println("$$$$$-Rule 4 sent to switch -$$$$$ ");
				         		log.info("$$$$$-Rule 4 sent to switch -$$$$$ ");
									 
				         		break;
				         	case "OF_13":
				         		//TODO support OF 1.3
			            	 	//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
			            	 	log.info("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
			            	 	break;
				            default:
			            		//System.out.println("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
				            	log.info("$$$$$-OFFactory Version-$$$$$ " + detectedVersion.toString() + " is currently not supported");
			            	 	break;
				         }//END switch (detectedVersion.toString()) inside not registered block
		             }//end else block for source/destination not registered
		         
		         }
		         else { //if it is not TCP and not UDP, then logic has not been implemented
		        	 //System.out.println("$$$$$-we got a "+ipv4.getProtocol().toString()+" packet-$$$$$ ");//0x01 is ICMP protocol i.e. nw_proto=1 with ETHType/dl_type 0x800
		         }		         
		     } //end of EthType.IPv4
		     else if (eth.getEtherType() == EthType.ARP) { //ARP is 0x806
		         /* We got an ARP packet; get the payload from Ethernet */
		    	 //System.out.println("$$$$$-we got an ARP packet-$$$$$ ");
		    	 
		         ARP arp = (ARP) eth.getPayload();
				 //arp.getOpCode();
				 log.info("$$$$$-we got an ARP packet-$$$$$ "+arp.getOpCode()+","+arp);
		         /* Various getters and setters are exposed in ARP */
		         boolean gratuitous = arp.isGratuitous();
		         /*//meaning it is not prompted by an ARP request but sent as a broadcast as a way for a node to announce or update its IP to MAC mapping to the entire network */
	        
	             /* Your logic here! */
		
		     } 
		     else {
		         /* Unhandled ethertype */
		    	 //System.out.println("$$$$$-Unhandled ethertype-$$$$$ ");
		    	 log.info("$$$$$-Unhandled ethertype-$$$$$ "+eth.getEtherType());
		     }
		     
		     break; // end of case PACKET_IN:
	     
	     default: 
	    	 //something other than packet in
	    	 //System.out.println(msg.getType().toString());
	    	 log.info("Got something other than packet_in"+msg.getType().toString());
	    	 break;
	   }
	   return Command.CONTINUE;//this allows this packet to continue to be handled by other PACKET_IN handlers
	}
	 
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {//add this module to the module loading system, telling the module loader we depend on it
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}
	 
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {//load dependencies and initialize data structures
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		
		
		// read our config options
		Map<String, String> configOptions = context.getConfigParams(this);
		//logger = LoggerFactory.getLogger(XTalker.class);
		RALServerIP = configOptions.get("RALServerIP");
		RALServerPort = Integer.parseInt(configOptions.get("RALServerPort"));
		flowRuleExpirationTimer = 60;//seconds, set the flow rule expiration timer i.e. idle timeout
		flowRuleHardTimer = 3600;//seconds, set the flow rule hard timer i.e. evict this rule if hard timer has expired regardless of recent match activity against this rule
		priorityAllow = 32768;
		priorityDeny = 2;
		//countUDPPackets = 0;
		//countTCPPackets = 0;	
		countPacketIns = 0;
		countFlowAdds = 0;		
		try {
			client = new RALClient2(RALServerIP,RALServerPort);
		}
		catch (Exception e) {
			System.out.println("Error: " + e);
		}
		
	}
	
	@Override
	public void startUp(FloodlightModuleContext context) {//register for PACKET_IN message in the startUp method of the module. This assures that other modules we depend on are already initialized
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}
}