import java.io.*;
import java.net.*;

/**
 * Listener thread that keeps listening to a port and asks talker thread to process
 * when a request is accepted.
 * @author Anusha
 * @author Prarthana Raghavan
 * @author Srivatsa
 *
 */

public class Listener extends Thread {
	private Node currentNode;
	private ServerSocket listenerSocket;
	private boolean status;

	public Listener (Node currentNode) {
		this.currentNode = currentNode;
		this.status = true;
		InetSocketAddress currNodeAddr = currentNode.getAddress();
		int nodePort = currNodeAddr.getPort();
		// Create server socket to listen for incoming connections
		try 
		{
			listenerSocket = new ServerSocket(nodePort);
		} 
		catch (Exception e) 
		{
			System.out.println("Cannot create socket");
		}
	}

	@Override
	public void run() {
		while (status) 
		{
			Socket receiverSock = null;
			try 
			{
				receiverSock = listenerSocket.accept();
			} 
			catch (Exception e) 
			{
				System.out.println("Cannot establish communication");
			}
			// Create a Receiver thread to handle incoming requests
			new Thread(new Receiver(receiverSock, currentNode)).start();
		}
	}

	public void kill() {
		status = false;
	}
}
