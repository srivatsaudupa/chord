import java.net.*;
import java.util.*;

/**
 * Query class that offers the interface by which users can do 
 * search by querying a valid chord node.
 * @author Anusha Naik
 * @author Prarthana Raghavan
 * @author Srivatsa Udupa
 *
 */

public class Search{

	private static InetSocketAddress currentAddress;
	private static Handler handler;

	public static void main (String[] args) {
		handler = new Handler();
		// check for the number of arguments
		if (args.length == 2) {
			// extract the socket address
			currentAddress = Handler.buildSocketAddress(args[0]+":"+args[1]);
			if (currentAddress == null) {
				System.out.println("Error: Connection could not be established");
				System.exit(0);
			}
			// send request for the local IP
			String response = CommunicationHandler.sendRequest(currentAddress, "RQALV");

			// In the absence of response - exit
			if (response == null || !response.equals("RPALV"))  {
				System.out.println("\nNode Connection Failed:\nCould not establish connection to the node\n");
				System.exit(0);
			}

			// Otherwise, print the connection info
			System.out.println("**************************** Node Connection Successful **********************************");
			System.out.println("\tNode IP: "+currentAddress.getAddress().toString()+"\n\tNode Port: "+currentAddress.getPort()+"\n\tNode ID: "+handler.hashSocketAddress(currentAddress));
			System.out.println("******************************************************************************************");
			boolean predStatus = false;
			boolean succStatus = false;
			InetSocketAddress predNodeAddr = Handler.requestAddress(currentAddress, "RQEPR");			
			InetSocketAddress succNodeAddr = Handler.requestAddress(currentAddress, "RQCSC");
			if (predNodeAddr == null || succNodeAddr == null) {
				System.out.println("Error: Could not connect to the node.");
				System.exit(0);	
			}
			if (predNodeAddr.equals(currentAddress))
				predStatus = true;
			if (succNodeAddr.equals(currentAddress))
				succStatus = true;

			// Validity - if both predecessor and successor or both are unavailable
			while (predStatus^succStatus) {
				System.out.println("System is stabilizing...");
				predNodeAddr = Handler.requestAddress(currentAddress, "RQEPR");			
				succNodeAddr = Handler.requestAddress(currentAddress, "RQCSC");
				if (predNodeAddr == null || succNodeAddr == null) {
					System.out.println("Error: Could not connect to the node");
					System.exit(0);	
				}
				
				predStatus = predNodeAddr.equals(currentAddress)?true:false;
				succStatus = succNodeAddr.equals(currentAddress)?true:false;
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}

			}

			// Enter the key to be searched
			Scanner searchKey = new Scanner(System.in);
			while(true) {
				System.out.print("\nSearch Engine\n-------------------------\n(Type a filename or 'Exit' to quit the search engine): ");
				String command = searchKey.nextLine();
	
				if (command.toLowerCase().startsWith("exit")) 
				{
					System.exit(0);				
				}
				else if (command.length() > 0)
				{
					long hashId = Handler.hashString(command);
					System.out.println("\nSearch Key Hash Value: "+hashId);					
					// print out response from the node
					System.out.println("Locating file...");
					try
					{
						InetSocketAddress nodeAddr = Handler.fileSearch(currentAddress, hashId);
						if(nodeAddr == null)
							System.out.println("The file does not exist in the system");
						else
						{
							System.out.println("The file has been located in the following node");
							System.out.println("\tNode IP: "+nodeAddr.getAddress()+"\n\tNode Port: "+nodeAddr.getPort()+"\n\tNode Hash ID: "+Handler.hashSocketAddress(nodeAddr));
						}	
					}
					catch(Exception e)
					{
						System.out.println("The Search Engine node has disconnected");
						System.exit(0);
					}
				}
			}
		}
		else {
			System.out.println("\n Error: Invalid arguments provided \n");
		}
	}
}
