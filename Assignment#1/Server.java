import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.*;

/**
 * Class Name : Server
 * Server class encloses data members and methods necessary for connection
 * with client and calculation of the performance metrics.
 *
 * @author (Shebin Roy Yesudhas : royyesudhas@wisc.edu)
 * @author (Sangeetha Sampath Kumar : sampathkuma4@wisc.edu)
 */
public class Server {

	/* Data Members */
	ServerSocket serverSocket;
	Socket clientSocket;
	DataInputStream inputStream;
	int portNumber;
	long time;
	long totalKiloBytesReceived;
	float bandWidth;


	/**
	* Method Name: Server() - Constructor which initializes data members with default values
	*/
	public Server() {
		this.portNumber = 0;
		this.time = 0;
		this.totalKiloBytesReceived = 0;
		this.serverSocket = null;
		this.clientSocket = null;
		this.inputStream = null;
	}

	/**
	* Method Name:  validateInput - to validate command line inputs and return error codes
	* @param String args[]: Array of strings obtained as command line arguments
	* @return Type: int
			-1 : missing arguments
			-2 : extra arguments
			-3 : invalid port number type
			-4 : port number not found
			-5 : port number not within valid range 
			0 : validation successful
	*/
	int validateInput(String args[]) {
		/* Missing Arguments */
		if(args.length < 3)
			return -1;
		/* Extra Arguments */
		if(args.length > 3)
			return -2;

		String inputArguments;
		StringBuilder sb = new StringBuilder(args[0]);
		for(int i = 1; i < args.length; i++) {
			sb.append(" ");
			sb.append(args[i]);
		}
		inputArguments = sb.toString();
		//System.out.println(inputArguments);
		/* Regular expression to group the arguments */
		Pattern p = Pattern.compile("^-s\\s-p\\s(\\d+)$");
		Matcher m = p.matcher(inputArguments);

		if(m.find()) {
			try {
				this.portNumber = Integer.parseInt(m.group(1));
			} catch (NumberFormatException e) {
				return -3;
			}
		}
		else {
			return -4;
		}

		if(portNumber < 1024 || portNumber > 65534)
			return -5;

		return 0;
	}

	/**
	* Method name: establishConnection - Method to establish connection
	* @return Type: int
			0 : connection successful
			1 : IO Exception
	*/
	int establishConnection() {
		try {
			/* Creating server socket object */
			serverSocket = new ServerSocket(this.portNumber);
			/* Accepting client connection using serversocket.accept method */
			clientSocket = serverSocket.accept();
			/* Creating input stream object to receive data from client */
			inputStream = new DataInputStream(clientSocket.getInputStream());

		}catch(IOException e) {
			return -1;
		}catch(Exception e) {
			System.out.println("Error : Exception " + e);
		}
		return 0;
	}

	/**
	* Method Name : closeConnection() - to close connection
	*/
	void closeConnection() {
		try {
			/* Closing input stream */
			inputStream.close();
			/* Closing client socket */
			clientSocket.close();
			/* Closing server stream */
			serverSocket.close();
		} catch(IOException e) {
			System.out.println("Error : Exception " + e);
		}
	}

	/**
	* Method Name: getData() -  to get Data from client
	*/
	void getData() {
		
		totalKiloBytesReceived = 0;
		long start, now = 0;
		int tempBytesCount =0;
		try{
			int count = 0;
			byte data1[] = new byte[1000];
			/* Storing start time */
			start = System.nanoTime();
			int bytes_count=0;
			/* Obtaining data from client and incrementing in Kilo Bytes during the connection time period */
			while (true) {
				bytes_count = inputStream.read(data1);
				if(bytes_count == -1)
					break;
				tempBytesCount += bytes_count;
				if(tempBytesCount > 1000){
					totalKiloBytesReceived+=1;
					tempBytesCount -= 1000;
				}
			}
			if(tempBytesCount > 1000){
				totalKiloBytesReceived += 1;
			}
			/* Storing end time to calculate bandwidth */
			now = System.nanoTime();
			/* Time taken from start to end of the connection : divide by 10^9 to get time in seconds */
			this.time = (now - start) / 1000000000;
		} catch(Exception e) {
			System.out.println("Error : Exception " + e);
		}
	}

	/**
	* Method Name:  getBandwidthInfo - calculates data rate
	* @return String: Received= <Integer>KB rate=<bandWidth>Mbps"
	*/
	String getBandwidthInfo(){
		String bandWidthInfo;
		/* Calculate bandwidth in Mbps */
		this.bandWidth = ((this.totalKiloBytesReceived/1000)*8) / this.time;
		bandWidthInfo = "received=" + this.totalKiloBytesReceived + " KB " + "rate=" + bandWidth + " Mbps";
		return bandWidthInfo;
	}
}
