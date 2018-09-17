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
			String input;
			while((input = in.readLine()) != null) {
				System.out.println("Client : " + input);
				out.println(input);
				System.out.println("Server : " + input);
			}
		} catch(IOException e) {
			System.out.println("Error : Exception while listening to the port! " + portNumber);
		}
	}
}
