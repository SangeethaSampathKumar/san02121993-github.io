import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.*;

/**
 * Class Name : Client
 * Client class encloses data members and methods necessary for connection
 * with server and calculation of the performance metrics.
 *
 * @author (Shebin Roy Yesudhas : royyesudhas@wisc.edu)
 * @author (Sangeetha Sampath Kumar : sampathkuma4@wisc.edu)
 */
public class Client {

	/* Data Members required for connection */
	Socket clientSocket;
	DataOutputStream outputStream;

	/* Data Memebers to be passed as input */
	String serverName;
	int portNumber;
	int time;

	/* Data Members required for bandwidth calculation */
	long dataSent;
	double bandwidth;

	/**
	 * Constructor of Class Client
	 * Initializes data members to default values
	 */
	public Client() {
		this.serverName = "0.0.0.0";
		this.portNumber = 0;
		this.time = 0;
		this.dataSent = 0;
		this.clientSocket = null;
		this.outputStream = null;
	}

	/**
	 * Method Name : printClientInfo
	 * Prints the data member values of client object
	 */
	void printClientInfo() {
		System.out.println("Server Name : " + this.serverName);
		System.out.println("Port Number : " + this.portNumber);
		System.out.println("Time : " + this.time);
	}

	/**
	 * Method Name : validateInput
	 * Validates the input with respect to expected format and expected
	 * values. If the values are of expected range, then updates the data
	 * members to the values passed as input.
	 *
	 * @param (String args[]) (Array of input values passed in the command line)
	 * @return (Type : int
	 *		 0 : Success
	 *		-1 : less number of arguments than expected
	 *		-2 : more number of arguments than expected
	 *		-3 : port number and time is of wrong format
	 *		-4 : arguments passed is of wrong format
	 *		-5 : port number is not of expected range)
	 */
	int validateInput(String args[]) {
		/* Missing Arguments */
		if(args.length < 7)
			return -1;
		/* Extra Arguments */
		if(args.length > 7)
			return -2;

		String inputArguments;
		StringBuilder sb = new StringBuilder(args[0]);
		for(int i = 1; i < args.length; i++) {
			sb.append(" ");
			sb.append(args[i]);
		}
		inputArguments = sb.toString();

		/* Regular expression to group the arguments */
		Pattern p = Pattern.compile("^-c\\s-h\\s(.*)\\s-p\\s(\\d+)\\s-t\\s(\\d+)$");
		Matcher m = p.matcher(inputArguments);

		if(m.find()) {
			this.serverName = m.group(1);
			try {
				this.portNumber = Integer.parseInt(m.group(2));
				this.time = Integer.parseInt(m.group(3));
			} catch (NumberFormatException e) {
				return -3;
			}
		}
		else {
			return -4;
		}

		/* Valid Range of Port Numbers : 1024 to 65535, less than 1024 port number are reserved */
		if(portNumber < 1024 || portNumber > 65535)
			return -5;

		return 0;
	}

	/**
	 * Method Name : establishConnection
	 * Establishes connection with a server on the port given as input and
	 * intansiates an output stream for the socket
	 *
	 * @return (Type : int
	 *		 0 : Success
	 *		-1 : Unknown host exception
	 *		-2 : I/O error exception)
	 */
	int establishConnection() {
		try {
			/* Creating a client socket object */
			clientSocket = new Socket(this.serverName, this.portNumber);
			/* Creating an output stream object and linking it with client socket object to send data to server */
			outputStream = new DataOutputStream(clientSocket.getOutputStream());
		} catch(UnknownHostException e) {
			System.out.println("Error: Unknown host name " + serverName);
			return -1;
		} catch(IOException e) {
			System.out.println("Error: I/O error with " + serverName);
			return -2;
		}
		return 0;
	}

	/**
	 * Method Name : closeConnection
	 * Closes the output stream and connection on the socket
	 *
	 * @return (Type : int
	 *		 0 : Success
	 *		-1 : Exception)
	 */
	int closeConnection() {
		try {
			/* Closing output stream */
			outputStream.close();
			/* Closing client socket */
			clientSocket.close();
		} catch(IOException e) {
			System.out.println("Error: Exception " + e);
			return -1;
		}
		return 0;
	}

	/**
	 * Method Name : pushData
	 * Pushes data of 1000 bytes during every iteration for t seconds give as
	 * input. Saves the total data sent in Kilo Bytes
	 *
	 * @return (Type : int
	 *		 0 : Success
	 *		-1 : Exception)
	 */
	int pushData() {
		/* Data of 1000 bytes sent from Client - all initiatlized to '0' */
		byte data[] = new byte[1000];
		long start, now, timeTaken = 0;

		dataSent = 0;
		try {
			start = System.nanoTime();
			/* Sends data till time expires */
			while(true) {
				now = System.nanoTime();
				timeTaken = (now - start) / 1000000000;
				if(timeTaken < this.time) {
					outputStream.write(data);
					/* 1000 Bytes = 1 KByte
					* Since, the data sent is calculated as 1KB per sending,
					* Hence, adding 1 instead of 1000 */
					dataSent += 1;
				}
				else {
					break;
				}
			}
		} catch(Exception e) {
			System.out.println("Error: Exception " + e);
			return -1;
		}
		return 0;
	}

	/**
	 * Method Name : getBandWidth
	 * Calculates bandwidth based on the data collected for the previous connection
	 * and returns a String object in expected format
	 *
	 * @return (Type : String of data sent and rate in expected format)
	 */
	String getBandWidthInfo() {
		String bandwidthInfo;
		String BWValue;

		/* Expected Bandwidth metric : Mbps - Mega bits per second
		* Data sent : X KBytes
		* Bandwidth : X * 8 / 1000 / time -> Megabits per Second
		* Note : To avoid overflows, divided before converting to bits
		*/
		this.bandwidth = ((this.dataSent * 1.0 / 1000) * 8) / this.time;
		BWValue = String.format("%.3f", this.bandwidth);
		bandwidthInfo = "sent=" + this.dataSent + " KB " + "rate=" + BWValue + " Mbps";
		return bandwidthInfo;
	}
}
