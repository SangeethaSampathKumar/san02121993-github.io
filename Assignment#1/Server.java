import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.*;

public class Server {

	/* Data Members */
	ServerSocket serverSocket;
	Socket clientSocket;
	DataInputStream inputStream;
	int portNumber;
	long time;
	int totalBytesReceived;
    float bandWidth;
	
	/* Constructor which initializes data members with default values */
	public Server() {
		this.portNumber = 0;
		this.time = 0;
        this.totalBytesReceived = 0;
        this.serverSocket = null;
		this.clientSocket = null;
		this.inputStream = null;
	}

	void printServerInfo() {
		System.out.println("Port Number : " + this.portNumber);
		System.out.println("Time : " + this.time);
	}

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
		System.out.println(inputArguments);
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

	int establishConnection() {
		try {
            serverSocket = new ServerSocket(this.portNumber);
			clientSocket = serverSocket.accept();
			inputStream = new DataInputStream(clientSocket.getInputStream());
	   
		}catch(IOException e) {
			//System.out.println("Error : I/O error with " + serverName);
			return -2;
		}catch(Exception e) {
			System.out.println("Error : Exception " + e);
		}
		return 0;
	}

	void closeConnection() {
		try {
			inputStream.close();
            serverSocket.close();   
		} catch(IOException e) {
			System.out.println("Error : Exception " + e);
		}
	}

    void getData() {
		totalBytesReceived = 0;
		long start, now = 0;
		try{
			int count = 0;
			byte data1[] = new byte[1000];
			start = System.nanoTime();
            while ((count = inputStream.read(data1)) > 0) {
               totalBytesReceived += count;
			}
			now = System.nanoTime();
			this.time = (now - start) / 1000000000;

		} catch(Exception e) {
			System.out.println("Error : Exception " + e);
		}
	}

	String getBandwidthInfo(){
		String bandWidthInfo;
		bandWidth = (this.totalBytesReceived * 8) / this.time;
		bandWidthInfo = "Received=" + this.totalBytesReceived + "B " + "rate=" + bandWidth + "bps";
		// TBD : BW & Data Sent - Unit to be displayed
		return bandWidthInfo;
	}
}
