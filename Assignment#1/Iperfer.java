public class Iperfer {
	public static void main(String args[]) {
		if(args.length < 1) {
			System.out.println("Usage :"); 
			System.out.println("Client : java Iperfer -c -h <Host Name> -p <Port Number> -t <Time>");
			System.out.println("Server : java Iperfer -s -p <Port Number>");
			System.exit(0);
			//return -1;
		}

		if(args[0].equals("-c")) {
			Client c = new Client();
			int val = c.validateInput(args);
			System.out.println("Return : " + val);
			c.printClientInfo();
			c.establishConnection();
			c.pushData();
			c.closeConnection();
			System.out.println("sent=" + c.totalBytesSent + "Bytes rate=" + c.bandWidth + "bps");
		} else if(args[0].equals("-s")) {
			Server s = new Server();
			s.startListening(3501);
		}
		else {
			System.out.println("Invalid Endpoint!\n");
		}
		//return 1;
	}
}
