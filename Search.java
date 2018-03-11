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

public class Search{

	private static InetSocketAddress localAddress;
	private static Handler handler;

	public static void main (String[] args) {
		handler = new Handler();
		// check for the number of arguments
		if (args.length == 2) {
			// extract the socket address
			localAddress = Handler.buildSocketAddress(args[0]+":"+args[1]);
			if (localAddress == null) {
				System.out.println("Error: Connection could not be established");
				System.exit(0);
			}
			// send request for the local IP
			String response = CommunicationHandler.sendRequest(localAddress, "KEEP");

			// In the absence of response - exit
			if (response == null || !response.equals("ALIVE"))  {
				System.out.println("\nNode Connection Failed:\nCould not establish connection to the node\n");
				System.exit(0);
			}

			// Otherwise, print the connection info
			System.out.println("**************************** Node Connection Successful **********************************");
			System.out.println("\tNode IP: "+localAddress.getAddress().toString()+"\n\tNode Port: "+localAddress.getPort()+"\n\tNode ID: "+handler.hashSocketAddress(localAddress));
			System.out.println("******************************************************************************************");
			boolean pred = false;
			boolean succ = false;
			InetSocketAddress pred_addr = Handler.requestAddress(localAddress, "YOURPRE");			
			InetSocketAddress succ_addr = Handler.requestAddress(localAddress, "YOURSUCC");
			if (pred_addr == null || succ_addr == null) {
				System.out.println("Error: Could not connect to the node.");
				System.exit(0);	
			}
			if (pred_addr.equals(localAddress))
				pred = true;
			if (succ_addr.equals(localAddress))
				succ = true;

			// Validity - if both predecessor and successor or both are unavailable
			while (pred^succ) {
				System.out.println("System is stabilizing...");
				pred_addr = Handler.requestAddress(localAddress, "YOURPRE");			
				succ_addr = Handler.requestAddress(localAddress, "YOURSUCC");
				if (pred_addr == null || succ_addr == null) {
					System.out.println("Error: Could not connect to the node");
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
				System.out.print("\nSearch Engine\n-------------------------\n(Type a filename or 'Exit' to quit the search engine): ");
				String command = null;
				command = searchKey.nextLine();
				
				if (command.startsWith("Exit")) {
					System.exit(0);				
				}
				
				else if (command.length() > 0){
					long hash = Handler.hashString(command);
					System.out.println("\nSearch Key Hash Value: "+hash);					
					// print out response from the node
					System.out.println("Locating file...");
					InetSocketAddress nodeAddr = Handler.fileSearch(localAddress, hash);
					if(nodeAddr == null)
						System.out.println("The file does not exist in the system");
					else
					{
						System.out.println("The file has been located in the following node");
						System.out.println("\tNode IP: "+nodeAddr.getAddress()+"\n\tNode Port: "+nodeAddr.getPort()+"\n\tNode Hash ID: "+Handler.hashSocketAddress(nodeAddr));
					}	
				}
			}
		}
		else {
			System.out.println("\n Error: Invalid arguments provided \n");
		}
	}
}
