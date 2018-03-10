import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * Query class that offers the interface by which users can do 
 * search by querying a valid chord node.
 * @author Anusha
 * @author Prarthana Raghavan
 * @author Srivatsa
 *
 */

public class Query {

	private static InetSocketAddress localAddress;
	private static Handler handler;

	public static void main (String[] args) {

		handler = new Handler();

		// check for the number of arguments
		if (args.length == 2) {

			// extract the socket address
			localAddress = Handler.buildSocketAddress(args[0]+":"+args[1]);
			if (localAddress == null) {
				System.out.println("Error: Unable to contact the node");
				System.exit(0);
			}

			// send request for the local IP
			String response = CommunicationHandler.sendRequest(localAddress, "KEEP");

			// In the absence of response - exit
			if (response == null || !response.equals("ALIVE"))  {
				System.out.println("\nCannot find node you are trying to contact. Now exit.\n");
				System.exit(0);
			}

			// Otherwise, print the connection info
			System.out.println("Connection to node "+localAddress.getAddress().toString()+", port "+localAddress.getPort()+", position "+Handler.hashSocketAddress(localAddress)+".");

			boolean pred = false;
			boolean succ = false;
			InetSocketAddress pred_addr = Handler.requestAddress(localAddress, "YOURPRE");			
			InetSocketAddress succ_addr = Handler.requestAddress(localAddress, "YOURSUCC");
			if (pred_addr == null || succ_addr == null) {
				System.out.println("Error: Node attempting to contact - unable to");
				System.exit(0);	
			}
			if (pred_addr.equals(localAddress))
				pred = true;
			if (succ_addr.equals(localAddress))
				succ = true;

			// Validity - if both predecessor and successor or both are unavailable
			while (pred^succ) {
				System.out.println("Waiting for the system to be stable...");
				pred_addr = Handler.requestAddress(localAddress, "YOURPRE");			
				succ_addr = Handler.requestAddress(localAddress, "YOURSUCC");
				if (pred_addr == null || succ_addr == null) {
					System.out.println("Error: Node attempting to contact - unable to");
					System.exit(0);	
				}
				if (pred_addr.equals(localAddress))
					pred = true;
				else 
					pred = false;
				if (succ_addr.equals(localAddress))
					succ = true;
				else 
					succ = false;
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}

			}

			// Enter the key to be searched
			Scanner searchKey = new Scanner(System.in);
			while(true) {
				System.out.println("\nPlease enter your search key (or type \"quit\" to leave): ");
				String command = null;
				command = searchKey.nextLine();
				
				if (command.startsWith("quit")) {
					System.exit(0);				
				}
				
				else if (command.length() > 0){
					long hash = Handler.hashString(command);
					System.out.println("\nHash value is "+Long.toHexString(hash));
					InetSocketAddress result = Handler.requestAddress(localAddress, "FINDSUCC_"+hash);
					
					// if local node is disconnected - exit
					if (result == null) {
						System.out.println("The node your are contacting is disconnected. Now exit.");
						System.exit(0);
					}
					
					// print out response from the node
					System.out.println("\nResponse from node "+localAddress.getAddress().toString()+", port "+localAddress.getPort()+", position "+Handler.hashSocketAddress(localAddress)+":");
					System.out.println("Node "+result.getAddress().toString()+", port "+result.getPort()+", position "+Handler.hashSocketAddress(result ));
				}
			}
		}
		else {
			System.out.println("\n Error: Invalid data!\n");
		}
	}
}
