import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Ring class with two options:
 *         1. Create a new chord ring
 *         2. Joining an existing ring
 * @author Anusha
 * @author Srivatsa
 * @author Prarthana Raghavan
 *
 */

public class Ring {
	
	private static Node currentNodeObj;
	private static InetSocketAddress requestingNode;
	private static Handler handler;

	public static void main (String[] args) {
		
		// Helps to create binding addresses for creating/joining nodes
		handler = new Handler();
		
		//Read local IP for creating node objects for the joining/creating Ring nodes
		String currentIP = null;
		try {
			currentIP = InetAddress.getLocalHost().getHostAddress();

		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// create the Node object of the ring
		currentNodeObj = new Node(Handler.buildSocketAddress(currentIP+":"+args[0]));
		
		/* Use command line data to choose between creating a new ring and joining an already stable ring */
		/* If arguments length = 1, the node is initiating a new ring */
		/* If arguments length = 3, the node is requesting a stable ring to join */
		if (args.length == 1) {
			System.out.println("\nInitiating node creation..");
			System.out.println("Initiating creation of ring..");
			requestingNode = currentNodeObj.getAddress();
		}
		else if (args.length == 3) {
			// Verbose message to user terminal
			System.out.println("\nCreating node and joining the ring");
			requestingNode = Handler.buildSocketAddress(args[1]+":"+args[2]);
			if (requestingNode == null) {
				System.out.println("Error: The node requested to join refused connection or the address is invalid. Node is terminating..");
				return;
			}	
		}		
		else {
			System.out.println("Error: Please specify the right number of arguments on command line");
			System.exit(0);
		}
		
		// Join the ring.
		boolean joined = currentNodeObj.join(requestingNode);
		
		// Check for create or join failure 
		if (!joined) {
			if(args.length == 1)
				System.out.println("Error: Unable to create the ring. Node is terminating..");
			else if(args.length == 3)
				System.out.println("Error: Unable to join the ring. Node is terminating..");
			System.exit(0);
		}

		/* Node information */
		currentNodeObj.displayNodeInformation(currentIP);
		/* Node Options */
		/* Provide a virtual terminal for the node with options to choose */
		/* Available options are 
			* 1. View Current Node information 
			* 2. View the current node finger table
			* 3. Terminate the node and exit */
		Scanner option = new Scanner(System.in);
		while(true) {
			System.out.print("\nNode Options: Choose from the following\n\t1. Node Information\n\t2. Finger Table\n\t3. Terminate Node Session\nOption:");
			int opt = option.nextInt();
			switch(opt)
			{
				case 0: continue;
				case 1: // Display node information 
					currentNodeObj.displayNodeInformation(currentIP);
					break;
				case 2: // Display the finger table 
					currentNodeObj.displayFingerTable();
					break;
				case 3: // Terminate the processes 
					System.out.println("Termination of node instance initiated. Killing all the processes.."); 
					currentNodeObj.terminateNodeInstance();
					System.out.println("Node terminated");
					System.exit(0);
				break;
				default: System.out.println("Choose from the available valid options");
			}
		}
	}
}
