# Reactive-SDN
This project contains the code for: 

1)	Distributed, reactive SDN application built as a pluggable Floodlight module (XTalker) in Java for facilitating inter-agency data sharing: XTalker.java
2)	Mininet topology script with traffic generator to create the network for the sending agency and the network for the receiving agency: topo-ditg.py
3)	Python script for generating traffic scripts (TCP or UDP) assuming Exponential inter-arrival time of packets to be fed into the traffic generator: flowgenxflowsyhosts.py
4)	Registered Agencies List (RAL) Server and Client: RALServer.java and RALClient2.java.
5)	Bash script to coalesce the results of the traffic generation: calcTotalFlows.sh.

The instructions provided below assume that the RAL Server will be running on a machine with Windows OS and the Floodlight Controllers and the Mininet networks will be running as their own Linux Virtual Machines on the host machine.

## Getting Started

1)	Download and install Mininet VM: https://github.com/mininet/mininet/wiki/Mininet-VM-Images 
2)	Download, build and run Floodlight VM: https://floodlight.atlassian.net/wiki/spaces/floodlightcontroller/pages/8650780/Floodlight+VM 
3)	Download and install Oracle VM VirtualBox: https://www.virtualbox.org/. Import and run the Mininet and Floodlight VMs: 
4)	Download and build Distributed Internet Traffic Generator (D-ITG) in the Mininet VM following the instructions: http://www.grid.unina.it/software/ITG/manual/index.html#SECTION00030000000000000000 
5)	Assuming Windows OS on the host machine, download and install Xming: http://www.straightrunning.com/XmingNotes/ and PuTTY: https://www.chiark.greenend.org.uk/~sgtatham/putty/ 
6)	Install the XTalker Module and RAL Client:
a.	In the Floodlight VM, download and add the XTalker.java and RALClient2.java class from the Floodlight-Module git folder into a new package in the Floodlight controller. 
a.	Register the module by adding the fully qualified module name in src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule; net.floodlightcontroller.xtalker.XTalker
b.	Append the module in the Floodlight module configuration file src/main/resources/floodlightdefault.properties  floodlight.modules = <existing modules>, net.floodlightcontroller.xtalker.XTalker
7)	Download the RAL Server
a.	On the host machine serving as the RAL Server, download the RALServer.java file from the RALServer git folder.
b.	Set the RAL Server IP address by defining the properties net.floodlightcontroller.xtalker.XTalker.RALServerIP and net.floodlightcontroller.xtalker.XTalker.RALServerPort in the Floodlight properties file: src/main/resources/floodlightdefault.properties.
8)	Clone the Floodlight VM to have two controllers, one for the sender network and one for the receiver network. Assume the two controllers are FLVM1 and FLVM2.
9)	Clone the Mininet VM to have two networks, one for the sending hosts and one for the receiving hosts. Assume the two Mininet VMs are MNVM1 and MNVM2.
10)	In MNVM1, copy the following files from Mininet-Sender git folder: flowgenxflowsyhosts.py and calcTotalFlows.sh into the D-ITG/bin directory and topo-ditg.py into the mininet/custom directory.
11)	In MNVM2, copy the following files from Mininet-Receiver git folder: calcTotalFlows.sh into the D-ITG/bin directory and topo-ditg.py into the mininet/custom directory.

## Instructions for starting the reactive SDN application and network topologies with the traffic generator

12)	On the host machine, start the Xming server, an X11 display server for Microsoft Windows. This allows us to launch the ITGSend and ITGRecv on different terminals.
13)	Start the RAL server (using Eclipse or Java complier). The server starts listening on port 5,999 and awaits client connection requests. Note the IP of the host machine.
14)	Start the two Floodlight VMs and note their IP addresses. Update the RAL Server IP address and Port if needed as described in 7-b.
15)	Start the controllers by navigating to src/main/java/net.floodlightcontroller.core/Main.java and using the menu option: Run As > Java Application > Default-Conf. Default port numbers for the controller have been used. Check that the controllers were able to successfully establish the connection with the RAL server. Controllers will start listening for Packet In messages on their respective ports 6,653.
16)	Run PuTTY with X11 forwarding enabled and connect to MNVM1.
17)	Run PuTTY with X11 forwarding enabled and connect to MNVM2.
18)	Start the Mininet topology for the receiving network on MNVM2. This also starts all the receiver processes at the respective hosts. This needs the IP address of the receiving network's controller along with the experiment's parameters. sudo python mininet/custom/topo-ditg.py <FLVM2 IP> <other parameters>
19)	Generate the desired traffic generation scripts on MNVM1 in the D-ITG bin folder using sudo python flowgenxflowsyhosts.py <parameters>.
20)	Start the Mininet topology for the sending network on MNVM1. This also starts all the sender processes at hosts. This needs the IP address of the sender network's controller along with the experiment's parameters. sudo python mininet/custom/topo-ditg.py <FLVM1 IP> <other parameters>
21)	After the experiment ends, run the calcTotalFlows.sh script to splice the results from the binary log files. This script uses ITGDec to decode the files to text and then using the AWK utility to search for the relevant information. The script is run at both the sender and receiver networks separately, because metrics like number of packets sent and round-trip time (if TCP) are measured at sender hosts while metrics like number of packets received, number of packets dropped, and one-way delay are measured at receiving hosts.
