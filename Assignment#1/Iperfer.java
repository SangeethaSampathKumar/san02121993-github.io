public class Iperfer {
	public static void main(String args[]) {
		int returnValue;

		System.out.println("------------------------------------------------");
		System.out.println("Iperfer Tool");
		System.out.println("------------------------------------------------");

		if(args.length < 1) {
			System.out.println("Error: missing or additional arguments");
			Iperfer.printUsage();
			System.exit(0);
		}

		if(args[0].equals("-c")) {
			Client c = new Client();

			System.out.println("Message: Creating a Client");
			returnValue = c.validateInput(args);
			if(returnValue != 0) {
				/* Invalid Inputs */
				switch(returnValue) {
					case -1:
						System.out.println("Error: missing or additional arguments");
						break;
					case -2:
						System.out.println("Error: missing or additional arguments");
						break;
					case -3:
						System.out.println("Error: Invalid value for port number or time!");
						break;
					case -4:
						System.out.println("Error: Invalid options & arguments!");
						break;
					case -5:
						System.out.println("Error: port number must be in the range 1024 to 65535");
						break;
				}
				Iperfer.printUsage();
				System.exit(0);
			}
			System.out.println("Message: Client Created successfully");

			//System.out.println("Return : " + val);
			//c.printClientInfo();

			returnValue = c.establishConnection();
			if(returnValue != 0) {
				System.out.println("Error: Could not establish connection with Server!");
				System.out.println("------------------------------------------------");
				System.exit(0);
			}
			System.out.println("Message: Established connection with " + c.serverName + " on port number " + c.portNumber);

			System.out.println("Message: Sending data chunks of 1000 bytes for " + c.time + " seconds");
			returnValue = c.pushData();
			if(returnValue != 0)
			{
				System.out.println("------------------------------------------------");
				System.exit(0);
			}
			System.out.println("Message: Sending completed");

			returnValue = c.closeConnection();
			/* TBD : Handle error cases */

			/* Bandwidth Calculation and display */
			System.out.println("Message: Performance metrics");
			System.out.println(c.getBandWidthInfo());

		} else if(args[0].equals("-s")) {
			
			Server sObj = new Server();
			returnValue = sObj.validateInput(args);
			if(returnValue != 0) {
				/* Invalid Inputs */
				switch(returnValue) {
					case -1:
						System.out.println("Error: missing or additional arguments");
						break;
					case -2:
						System.out.println("Error: missing or additional arguments");
						break;
					case -3:
						System.out.println("Error: Invalid value for port number or time!");
						break;
					case -4:
						System.out.println("Error: Invalid options & arguments!");
						break;
					case -5:
						System.out.println("Error: port number must be in the range 1024 to 65535");
						break;
				}
				Iperfer.printUsage();
				System.exit(0);
			}

			returnValue = sObj.establishConnection();
			if(returnValue != 0) {
				System.out.println("Error: Could not establish connection with Client!");
				System.exit(0);
			}
			sObj.getData();
			sObj.closeConnection();
			/* TBD : Handle error cases */

			/* Bandwidth Calculation and display */
			System.out.println(sObj.getBandwidthInfo());



		}	
		else {
			System.out.println("Error: Invalid option");
		}
		System.out.println("------------------------------------------------");
	}

	static void printUsage() {
		System.out.println("Usage:");
		System.out.println("Client: java Iperfer -c -h <Host Name> -p <Port Number> -t <Time>");
		System.out.println("Server: java Iperfer -s -p <Port Number>");
		System.out.println("------------------------------------------------");
	}
}
