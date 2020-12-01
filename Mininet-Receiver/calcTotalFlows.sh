#!/bin/bash

if [ "$#" -ne 6 ]
then
	echo "Usage: <mean> <flows> <hosts> <tau> <sigma> <run-numer>"
	exit 1
fi

arg1=$1
arg2=$2
arg3=$3
arg4=$4
arg5=$5
arg6=$6

valid=1
count=1

numflows=1000
totalflows=0
totalpackets=0
totaldropped=0
totalavgdelay=0
avgtotalavgdelay=0

while [ $valid ]
do
	#echo $count
	filename="ru-r-m$arg1-f$arg2-x$arg3-t$arg4-s$arg5-h$count-b$count-r$arg6"
	echo $filename

	var1=`(exec ./ITGDec $filename | awk '/Number of flows/ { b=$5 } END{ print b }' )`	
	totalflows=$((var1+totalflows))
	#echo totalflows $totalflows

	var1=`(exec ./ITGDec $filename | awk '/Total packets/ { b=$4 } END{ print b }' )`
	totalpackets=$((var1+totalpackets))
	#echo totalpackets $totalpackets

	var1=`(exec ./ITGDec $filename | awk '/Packets Dropped/ { b=$4 } END{ print b }' )`
	totaldropped=$((var1+totaldropped))
	#echo totaldropped $totaldropped

	var1=`(exec ./ITGDec $filename | awk '/Average delay/ { b=$4 } END{ print b }' )`
	#echo $var1
	totalavgdelay=`echo $var1 + $totalavgdelay | bc -l`
	#echo totalavgdelay $totalavgdelay

	((count++))
	if [ $count -eq 4 ];
	then
		echo totalflows $totalflows
		echo totalpackets $totalpackets
		echo totalavgdelay $totalavgdelay
		avgtotalavgdelay=`echo $totalavgdelay/10.0 | bc -l`
		echo avgtotalavgdelay $avgtotalavgdelay

		break
	fi
done

#(exec ./ITGRecv)


#end = $arg2
#echo $end "secs"
#end = end + 2
#echo $end
exit 1
while  [ $arg2 -gt 0 ]
do
	echo "hello"
	$arg2 = $arg2 - 1
done

