///////////////////////////////////////////////////////////////////////////////
// Title:            (Iperfer Application - Assignment #1)
// Files:            (Iperfer.java, Client.java, Server.java)
// Semester:         (CS640) Fall 2018
//
// Author:           (Shebin Roy Yesudhas, Sangeetha Sampath Kumar)
// Email:            (royyesudhas@wisc.edu, sampathkuma4@wisc.edu)
// CS Login:         (shebinroy, sangeetha)
// Lecturer's Name:  (Aditya Akella)
///
//////////////////// PAIR PROGRAMMERS COMPLETE THIS SECTION ////////////////////

/**
 * Class Name : Iperfer
 * Iperfer class encloses the main function which creates either Client or Server
 * object based on user input from the command line arguments. Based on the object
 * created, different methods are invoked and the performance metrics is finally
 * displayed to the user
 *
 * @author (Shebin Roy Yesudhas : royyesudhas@wisc.edu)
 * @author (Sangeetha Sampath Kumar : sampathkuma4@wisc.edu)
 */
public class Iperfer {
	/**
	 * Method Name : main
	 * Controls the follow of execution based on user input
	 *
	 * @param (String args[]) (Array of input values passed in the command line)
	 */
	public static void main(String args[]) {
		int returnValue;

		if(args.length < 1) {
			System.out.println("Error: missing or additional arguments");
			//Iperfer.printUsage();
			System.exit(0);
		}

		if(args[0].equals("-c")) {
			Client c = new Client();

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
				//Iperfer.printUsage();
				System.exit(0);
			}

			//System.out.println("Return : " + val);
			//c.printClientInfo();

			returnValue = c.establishConnection();
			if(returnValue != 0) {
				System.out.println("Error: Could not establish connection with Server!");
				System.exit(0);
			}

			returnValue = c.pushData();
			if(returnValue != 0)
			{
				System.exit(0);
			}

			returnValue = c.closeConnection();
			/* TBD : Handle error cases */

			/* Bandwidth Calculation and display */
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
				//Iperfer.printUsage();
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
	}

	/**
	 * Method Name : printUsage
	 * Static function which displays the usage of the Iperfer Application
	 *
	 * @param (String args[]) (Array of input values passed in the command line)
	 */
	static void printUsage() {
		System.out.println("Usage:");
		System.out.println("Client: java Iperfer -c -h <Host Name> -p <Port Number> -t <Time>");
		System.out.println("Server: java Iperfer -s -p <Port Number>");
	}
}
