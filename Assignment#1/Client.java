import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.*;

public class Client {
	String serverName;
	int portNumber;
	int time;
	byte dataChunks[];
	int totalBytesSent;
	float bandWidth;
	Socket clientSocket;
	PrintWriter outputStream;

	public Client() {
		this.serverName = "0.0.0.0";
		this.portNumber = 0;
		this.time = 0;
		this.dataChunks = new byte[1000];
		totalBytesSent = 0;
		clientSocket = null;
		outputStream = null;
	}

	void printClientInfo() {
		System.out.println("Server Name : " + this.serverName);
		System.out.println("Port Number : " + this.portNumber);
		System.out.println("Time : " + this.time);
		System.out.println("Data : ");
		for(byte val : dataChunks);
			//System.out.print(val + " ");
		System.out.println();
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

		return 1;
	}

	int establishConnection() {
		try {
			clientSocket = new Socket(this.serverName, this.portNumber);
			outputStream = new PrintWriter(
				clientSocket.getOutputStream(), true);
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
			clientSocket.close();
			outputStream.close();
		} catch(IOException e) {
			System.out.println("Error : Exception " + e);
		}
	}

	void pushData() {
		totalBytesSent = 0;
		long start, now, timeTaken = 0;
		try {
			start = System.nanoTime();
			while(true) {
				now = System.nanoTime();
				timeTaken = (now - start) / 1000000;
				if(timeTaken < this.time) {
					outputStream.println(this.dataChunks);
					totalBytesSent += 1000;
				}
				else {
					break;
				}
			}
		} catch(Exception e) {
			System.out.println("Error : Exception " + e);
		}
		System.out.println("Time Taken : " + timeTaken);
		System.out.println("Total Bytes Sent : " + totalBytesSent);
		bandWidth = totalBytesSent * 8 / time;
	}
}
