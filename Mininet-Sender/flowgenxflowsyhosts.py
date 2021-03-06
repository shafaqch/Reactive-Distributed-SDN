#!/usr/bin/python

"""begin comment
Author: Shafaq Chaudhry
Date: October 2020

********** Instructions ***********
Usage: sudo python flowgen.py <mean> <num_flows> <num_hosts> <network_prefix>
<mean> is 1/lambda the inter-arrival time of flows at source network's switch; It is also used as the mean of the inter-departure time of packets within a flow set in ITG Send command for poisson -O <mean>
<num_flows> is the number of flows per (sender, receiver) pair that we want the script to generate; max value can be up to 53, see note below. We are allowing 50.
<num_hosts> is the number of hosts in the sender network and number of hosts in the receiver network that we will create num_flows for. Each host will get its own script file for use by ITG Send in mininet later
<network_prefix> is the x.x.x.i prefix of the destination network where i = 1 to num_hosts TODO check for max value
<flow_duration> is the duration value in milliseconds "t" for each flow generated by ITGSend 
<udp_or_tcp> takes either 'u' or 't' for UDP or TCP traffic respectively
Description: This file will generate <num_hosts> script files for ITGSend to use. Each script file will have <num_flows> UDP flows for the destination 10.0.1.i i from 1 to num_hosts. 
Each flow within a script file will have its port number go from 5000 to 5000 + <num_flows>. Thus within a script file, a flow is defined as (source_ip,system-generated-port ; destination_ip,5000+i) 
Each packet in each flow will be of size 512 bytes.

Note: num_flows maximum value can be up to 53, ./ITGSend seems to not generate the flows if 54 or more is selected. Checked one of the defintiion files that has DEFINE value for a limit on this

Assumptions:
- The same lamda will be used for interflow arrival and inter packet arrival within a flow
- The recipient network is 10.0.1.x


********** End Instructions ***********

********** Summary of changes *********
Edit on 10/20/2020, changed the time duriation from 10 sec to 60 sec
This file is used to generate the input script for multiple flows to be fed into the ./ITGSend command

Edit on 11/1/2020, decided to add a little padding (10 sec) to all the cumulcated arrival time that sets the -d parameter in ITGSend script  
so the actual generation of flows  starts after all the xterms have been started in mininet

Edit on 11/4/2020, added RTTM value to the -m option for TCP to measure the round trip delay; for UDP this stays the default of OWDM; RTTM helps keep both Tx and Rx times of the sender machine, 
while for OWDM, both sender and receiver clocks need to be synched e.g. with NTP
For a single flow, changed the IDT from of packets from -O _mean to -E _lambda packets/second

Edit on 11/6, decide that interflow arrival times are not exponential and remove the delay (-d) option in the script file of ITGSend

end comment
"""

import random
import math
import sys

def write_to_file(params):
	
	_mean = float(params[0]) #e.g. 30 ; mean = 1/lambda 
	_lambda = float(1.0/float(_mean))
	_flows = int(params[1]) #e.g. 50 ; no. of flows is the max number of flows being generated by one ITGSend
	_numhosts = int(params[2]) #e.g. 10 ; no. of hosts i.e. senders and receivers; it will generate num_flows for each sender and receiver pair
	_networkprefix = params[3]
	_flowduration = params[4]
	_udportcp = params[5] # u for udp and t for tcp
	#_inter_arrv_time = 0 # inter-arrival time between flows will be generated using mean
	_next_time = 0
	#_arrival_time = 0 # this variable is to capture the cumulative arrival time so we can add the initial delay of generating the next flow using the -d param of ITGSend
	#_destination = str(params[2])	

	#_destination = "10.0.1."
	_destination = _networkprefix

	# decide that interflow arrival times are not exponential
	_is_interflow_exponential = 0

	for j in range (_numhosts):
		if _udportcp == 'u':
			_file_name = "multiflowu-"+str(_mean)+"-"+str(_flows)+"-"+_destination+str(j+1)
		else:
			_file_name = "multiflowt-"+str(_mean)+"-"+str(_flows)+"-"+_destination+str(j+1)

		#print "generating N= " +params[1]+ "flows with inter-arrival time 1/" +params[0]+ " and writing to file " + _file_name
	
		_arrival_time = 10 # this variable is to capture the cumulative arrival time so we can add the initial delay of generating the next flow using the -d param of ITGSend
		
		
		print "random-uniform,next-time,padded-time"
		with open(_file_name,'w') as f:

			for i in range (_flows): 
				#generate random number from Uniform(0,1)
				u=random.random()
	
				#calculate inter-arrival time i.e. nextTime
				_next_time = _mean * -math.log(1.0 - u)
				#_next_time = -math.log(1.0-u)/_lambda

				#calculate arrival time
				_padded_time = _arrival_time + _next_time

				x=(str(u)+','+str(_next_time)+','+str(_padded_time))
				print(x)		
			
				# generating UDP flows 
				# dest address a, dest port rp, packet rate C packets per second, each packet of size c bytes, duration of traffic generation t in ms, start generating after d ms,  
				port_num = 50000+i
				if _udportcp == 'u':
					if _is_interflow_exponential == 1:
						# x='-a '+_destination+str(j+1)+' -rp '+str(port_num)+' -O '+str(_mean)+' -c 512 -T UDP -t '+str(_flowduration)+' -d '+str(_padded_time) 
						x='-a '+_destination+str(j+1)+' -rp '+str(port_num)+' -E '+str(_lambda)+' -c 512 -T UDP -t '+str(_flowduration)+' -d '+str(_padded_time) 
					else:
						x='-a '+_destination+str(j+1)+' -rp '+str(port_num)+' -E 0.01 -c 512 -T UDP -t '+str(_flowduration) 
				else:
					if _is_interflow_exponential == 1:
						# x='-a '+_destination+str(j+1)+' -rp '+str(port_num)+' -O '+str(_mean)+' -c 512 -T TCP -m RTTM  -t '+str(_flowduration)+' -d '+str(_padded_time) 
						x='-a '+_destination+str(j+1)+' -rp '+str(port_num)+' -E '+str(_lambda)+' -c 512 -T TCP -m RTTM  -t '+str(_flowduration)+' -d '+str(_padded_time) 
					else:
						x='-a '+_destination+str(j+1)+' -rp '+str(port_num)+' -E '+str(_lambda)+' -c 512 -T TCP -m RTTM  -t '+str(_flowduration)

			
				#write to file
				f.write(str(x)+'\n')
		f.close()

if __name__ == '__main__':
	if(len(sys.argv)) < 7:
		print "Usage: <filename.py> <mean> <num_flows> <num_hosts> <network_prefix> <flow_duration> <u/t> ex: flwogenxflowsyhosts.py 30 50 10 10.0.1. 60000 u"
		quit()

	if(isinstance(sys.argv[2],int) and int(sys.argv[2]) > 50):
		print "Usage: flows can be up to 50"
		quit()
	write_to_file(sys.argv[1:])
