import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.*;

public class Client {

	/* Data Members */
	Socket clientSocket;
	DataOutputStream outputStream;
	String serverName;
	int portNumber;
	int time;
	int bytesSent;
	float bandwidth;

	/* Constructor which initializes data members with default values */
	public Client() {
		this.serverName = "0.0.0.0";
		this.portNumber = 0;
		this.time = 0;
		this.bytesSent = 0;
		this.clientSocket = null;
		this.outputStream = null;
	}

	void printClientInfo() {
		System.out.println("Server Name : " + this.serverName);
		System.out.println("Port Number : " + this.portNumber);
		System.out.println("Time : " + this.time);
	}

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
		System.out.println(inputArguments);

		Pattern p = Pattern.compile("^-c\\s-h\\s(.*)\\s-p\\s(\\d+)\\s-t\\s(\\d+)$");
		Matcher m = p.matcher(inputArguments);

		if(m.find()) {
			this.serverName = m.group(1);
			// TBD : Server Name Validation
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

		if(portNumber < 1024 || portNumber > 65534)
			return -5;

		return 0;
	}

	int establishConnection() {
		try {
			clientSocket = new Socket(this.serverName, this.portNumber);
			outputStream = new DataOutputStream(clientSocket.getOutputStream());
		} catch(UnknownHostException e) {
			System.out.println("Error : Unknown host name " + serverName);
			return -1;
		} catch(IOException e) {
			System.out.println("Error : I/O error with " + serverName);
			return -2;
		}
		return 0;
	}

	void closeConnection() {
		try {
			outputStream.close();
			clientSocket.close();
		} catch(IOException e) {
			// TBD : Handle exception
			System.out.println("Error : Exception " + e);
		}
	}

	void pushData() {
		// Data of 1000 bytes sent from Client - all initiatlized to '0'
		byte data[] = new byte[1000];
		long start, now, timeTaken = 0;

		bytesSent = 0;
		try {
			start = System.nanoTime();
			while(true) {
				now = System.nanoTime();
				timeTaken = (now - start) / 1000000000;
				if(timeTaken < this.time) {
					outputStream.write(data);
					bytesSent += 1000;
				}
				else {
					break;
				}
			}
		} catch(Exception e) {
			// TBD : Handle Exceptions
			System.out.println("Error : Exception " + e);
		}
	}

	String getBandWidthInfo() {
		String bandwidthInfo;
		this.bandwidth = ((this.bytesSent * 8) / (1000 * 1000)) / this.time;

		bandwidthInfo = "sent=" + (this.bytesSent/1000) + " KB " + "rate=" + this.bandwidth + " Mbps";
		// TBD : BW & Data Sent - Unit to be displayed
		return bandwidthInfo;
	}
}
