import java.io.*;
import java.net.*;

/* Dummy Server to check Client Connection */
/* Replace with original Server class */
public class Server {
	void startListening(int portNumber) {
		try {
			ServerSocket serverSocket = new ServerSocket(portNumber);
			Socket clientSocket = serverSocket.accept();
			PrintWriter out = new PrintWriter(
					clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(
				new InputStreamReader(clientSocket.getInputStream()));
			DataInputStream dIn = new DataInputStream(clientSocket.getInputStream());
			String input;
			while(true) {
				byte message[] = new byte[1000];
				dIn.readFully(message, 0, 1000);
				System.out.println("Recieved!");
			}
		} catch(IOException e) {
			System.out.println("Error : Exception while listening to the port! " + portNumber);
		}
	}
}
