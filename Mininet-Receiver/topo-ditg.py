#!/usr/bin/python

"""begin comment
Author: Shafaq Chaudhry
Date: Jan - Dec, 2020
\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\


*********** Instructions and Description  *************
Description: This file generates the receiving agency network; connects to a remote controller and forms a GRE tunnel with the sending agency network's switch

Usage: sudo python <filename.py> <controller_ip> <p/r> <numofhosts> <sigma> <u/t>

Explanation of parameters:

<filename.py> is the name of this file

<controller_ip> is the ip of the remote floodlight controller that the switch will connect; it must be running before starting the mininet script
Also, the port where controller is listening is assumed to be 6653.

<p/r> p implies run in proactive mode; this will install UDP/TCP rules proactively in the switch when mininet starts
r implies run in reactive mode; here floodlight controller will install the rules when switch sends packet_in messages to the floodlight controller. 
Note that in reactive mode, we still add a couple of configuration rules like for ARP, ICMP, and DITG signalling.

<numofhosts> this parameter is needed so that for each host, it will run an xTerm and in each xTerm, it will run the ITGRecv, which is the listening for the D-ITG traffic generator

<sigma> this is the overflow policy, if this parameter is set then it will only be used in the reactive case if p/r is set to 'r'.
If the value of this parameter is 100, then after 100 flow rules are installed by the Floodlight controller, upon the 101th rule, the switch will evict one of the ephemeral rules; 
note that the code will set flow_limit = sigma + x; where x is the count of proactive rules installed for ARP, ICMP and D-ITG signaling and it will set teh overflow_policy of the switch to evict.
This delets the flow that will expire soonest, meaning a flow that was either idle for a long time and it going to expire soon; or a flow that has reached its hard timeout value

 


*********** Summary of changes **********
11/5/2020 Changed overflow policy to only be set for reactive UDP case, not reactive TCP case. Changed flow_limit to be sigma + num of permanent rules in reactive case.


endcomment
"""

from mininet.net import Mininet
from mininet.node import Controller, OVSSwitch, RemoteController
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mininet.term import makeTerms, makeTerm, cleanUpScreens

import sys, subprocess, time


	
def multiControllerNet(argv):

    #argv[0] has remote controller ip address 
    #argv[1] has r for reactive and p for proactive



    #argv[2] is numofhosts to generate
    sigma = argv[3] 
    udportcp = argv[4]

    "Create a network from semi-scratch with multiple controllers."

    net = Mininet( controller=Controller, switch=OVSSwitch )

    print "*** Creating (reference) controllers"
 
    # add a local controller

    #works with remote pox controller
    #c1 = net.addController( 'c2', controller=RemoteController, 
#		ip=arg1, port=int(arg2))  #6633 is default 
    ##c1 = net.addController( 'c2', controller=RemoteController, 
    ##		ip='127.0.0.1', port=6633) 
    ###c1 = net.addController( 'c2', controller=RemoteController, 
    ###		ip='192.168.56.103', port=6653) 
    c1 = net.addController( 'c2', controller=RemoteController, 
    		ip=argv[0], port=6653) 

    #c1 = net.addController( 'c1', controller=RemoteController, 
	#	ip=arg1, port=6653)

    print "*** Creating switches with OF10"
    s1 = net.addSwitch( 's2', protocols="OpenFlow10" )

    print "*** Creating hosts"
    
    hosts1 = [ net.addHost('b1',ip='10.0.1.1')]

    # for range from 2 to 11, goes up to 10 not including 11
    for i in range(2,int(argv[2])+1): 
	hostname = 'b'+str(i)
    	hostip = '10.0.1.'+str(i)
    	hosts1.append ( net.addHost(hostname,ip=hostip) )

    print hosts1

    print "*** Creating links"
    for h in hosts1:
        net.addLink( s1, h )
	print "Added link " + s1.name + " -> " +h.name


    print "*** Starting network"
    net.build()
    net.start()

    time.sleep(1)

    # configure the GRE tunnel
    print "*** Configuring GRE tunnel ***"
    s1.cmd('ovs-vsctl add-port s2 s2-gre1 -- set interface s2-gre1 type=gre '+
       'options:remote_ip=192.168.56.101')
    s1.cmdPrint('ovs-vsctl show')   
   
    print "*** Inserting proactive rules for DITG signaling and ARP ***" 
    # add the proactive rules for DITG signaling on port 9000
    s1.cmd('ovs-ofctl add-flow s2 dl_type=0x800,nw_proto=6,tcp_dst=9000,actions=normal')
    s1.cmd('ovs-ofctl add-flow s2 dl_type=0x800,nw_proto=6,tcp_src=9000,actions=normal')
    # add proactive rule for ARP and ICMP
    s1.cmd('ovs-ofctl add-flow s2 dl_type=0x806,arp_op=1,nw_proto=1,actions=flood')#ARP request
    s1.cmd('ovs-ofctl add-flow s2 dl_type=0x800,nw_proto=1,icmp_type=8,actions=normal')#ICMP echo request
    s1.cmd('ovs-ofctl add-flow s2 dl_type=0x800,nw_proto=1,icmp_type=0,actions=normal')#ICMP echo reply
    s1.cmd('ovs-ofctl add-flow s2 dl_type=0x806,arp_op=2,actions=normal')#ARP reply

    print "*** Inserted proactive rules for DITG signaling and ARP ***" 

    #if 'r' is chosen, then configure overflow-policy, else add UDP allow rule for proactive case
    if argv[1] == 'r':
	print "r"
	if udportcp == 'u':
		flow_limit = int(sigma) + 6
	    	#configure overflow-policy only in case of UDP reactive 11/5
		print "*** Configuring overflow policy ***"
		strEvict = "ovs-vsctl -- --id=@ft create Flow_Table flow_limit="+str(flow_limit)+" overflow_policy=evict groups='\"NXM_OF_IN_PORT[]\"' -- set Bridge s2 flow_tables:0=@ft"
	    	s1.cmd(strEvict)
    elif argv[1] == 'p':
	if udportcp == 'u':
		print "*** Configuring UDP proactive rules ***"

		#configure udp allow proactive rule
    		s1.cmd('ovs-ofctl add-flow s2 dl_type=0x800,nw_proto=17,actions=normal')

	else:
		print "*** Configuring TCP  proactive rules ***"

		#configure tcp allow proactive rule
    		s1.cmd('ovs-ofctl add-flow s2 dl_type=0x800,nw_proto=6,actions=normal')


    print "*** Testing network with pingAll"
    net.pingAll()

    #wait a bit before starting xterms
    print "*** Starting xterms...***"
    #time.sleep(5)

    for b in hosts1:
	#the followings works but generates log file in the mininet custom folder insted of DITG bin
	#which makes ITGDec an extra step of having to move all the files
	#term = makeTerm(b, cmd="bash -c '~/D-ITG-2.8.1-r1023-src/D-ITG-2.8.1-r1023/bin/ITGRecv'")
	
	#assuming that you are in the DITG directory
	term = makeTerm(b, cmd="bash -c './ITGRecv;'")
    	net.terms.append( term )
	

    
    # open switch in a terminal and dump flows
    net.terms += makeTerms (net.switches, 'switch')
    #net.terms += makeTerms (net.hosts, 'host')
    #makeTerm(s1, cmd="bash -c 'ovs-ofctl dump-flows s2;'")
    

    #start the terminal for each controller, switch, host
    #net.startTerms()

    #start terminal for each host
    #net.terms += makeTerms( net.hosts, "host" )
    
    ###net.terms += makeTerm ( net.hosts[1], cmd="bash -c 'ifconfig;'" )
    #start the ITGRecv on 10 hosts

    #for h in hosts1:
	#net.terms.append( term ) 

    #if test1 is specified or no test is specified, run SimpleHTTPServer on h4
    ##h1 = net.hosts[0]
    ##h2 = net.hosts[1]
    #h3 = net.hosts[2]
    #h4 = net.hosts[3]
    
    ##print "*** Starting SimpleHTTPServer on host " + h2.name
    ##print h2.cmd('python -m SimpleHTTPServer 80 &')
    time.sleep(1)

    print "*** Running CLI"
    CLI( net )

    print "*** Stopping network"
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )  # for CLI output

    if (len(sys.argv)) < 6:
        print "Usage: <filename.py> <controller_ip> <p/r> <numofhosts> <sigma> <u/t>"
        quit()
    arg1 = sys.argv[1]
    #arg2 = sys.argv[2]

    print "*** clear previous mininet topologies ***"
    
    bashCommand = "sudo mn -c"
    process = subprocess.Popen(bashCommand.split(), stdout=subprocess.PIPE)
    output, error = process.communicate()

    multiControllerNet(sys.argv[1:])
