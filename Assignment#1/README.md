# Assingment 1
Part 1: Write Iperfer
Part 2: Mininet
Part 3: Measurements

# Design

Client.java
   The Client.class contains the info with respect to TCP connection
   such as Host Name, Port Number, time span to send data which are
   passed as input arguments

   There are five member functions
	validateInput()
	establishConnection()
	closeConnection()
	pushData()
	printClientInfo()

Iperfer.java
   The Iperfer.class has the main functions which creates Client/Server
   object based on the 1st input command line argument.
