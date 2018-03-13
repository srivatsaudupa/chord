import java.io.*;
import java.net.*;

/**
 * Receiver thread that processes request accepted by listener and writes
 * response to socket.
 * @author Srivatsa Udupa
 * @author Anusha Naik
 * @author Prarthana Raghavan
 *
 */

public class Receiver implements Runnable{
	Socket receiverSocket;
	private Node currentNode;
	public Receiver(Socket receiverSocket, Node currentNode)
	{
		this.receiverSocket = receiverSocket;
		this.currentNode = currentNode;
	}
	public void run()
	{
		InputStream iStream = null;
		OutputStream oStream = null;
		try 
		{
			iStream = receiverSocket.getInputStream();
			String request = CommunicationHandler.parseIOStream(iStream);
			String response = handleRequest(request);
			if (response != null) 
			{
				oStream = receiverSocket.getOutputStream();
				oStream.write(response.getBytes());
			}
			iStream.close();
		} 
		catch (Exception e) 
		{
			System.out.println("Communication from Node at "+this.currentNode.getAddress()+":"+this.currentNode.getAddress().getPort()+" to Receiver at "+this.receiverSocket.getPort()+" was not possible. ");
		}
	}

	private String handleRequest(String request)
	{
		InetSocketAddress resNodeAddr = null;
		String retMsg = null;
		String nodeIP = null;
		int nodePort = 0;
		long hashId = 0;
		if (request  == null) {
			return null;
		}
		String[] reqString = request.split("_");
		String rqCode = reqString[0];
		switch(rqCode)
		{
			// CLOSEST
			case "RQIM":
						hashId = Long.parseLong(request.split("_")[1]);
						resNodeAddr = currentNode.closest_preceding_finger(hashId);
						nodeIP = resNodeAddr.getAddress().toString();
						nodePort = resNodeAddr.getPort();
						retMsg = "RPIM_"+nodeIP+":"+nodePort;
						break;
			//YOURSUCC
			case "RQCSC":
						resNodeAddr =currentNode.getSuccessor();
						if (resNodeAddr != null) 
						{
							nodeIP = resNodeAddr.getAddress().toString();
							nodePort = resNodeAddr.getPort();
							retMsg = "RPCSC_"+nodeIP+":"+nodePort;
						}
						else 
						{
							retMsg = "NRP";
						}
						break;
			// YOURPRE
			case "RQEPR":
						resNodeAddr =currentNode.getPredecessor();
						if (resNodeAddr != null) 
						{
							nodeIP = resNodeAddr.getAddress().toString();
							nodePort = resNodeAddr.getPort();
							retMsg = "RPEPR_"+nodeIP+":"+nodePort;
						}
						else 
						{
							retMsg = "NRP";
						}
						break;
			// FINDSUCC
			case "RQFSC":
						hashId = Long.parseLong(request.split("_")[1]);
						resNodeAddr = currentNode.find_nextNode(hashId);
						nodeIP = resNodeAddr.getAddress().toString();
						nodePort = resNodeAddr.getPort();
						retMsg = "RPFSC_"+nodeIP+":"+nodePort;
						break;  
			// REQFILE
			case "RQFILE":
						hashId = Long.parseLong(request.split("_")[1]);
						retMsg = currentNode.fetchFiles(hashId);
						break;
			// FILETX
			case "RQFTX":
						for(int i=1; i<reqString.length;i++)
						{
							currentNode.updateFileTable(reqString[i]);
						}
						retMsg = "RPFTXCMP";
						break;
			// CHECKFILE
			case "RQCHF":
						hashId = Long.parseLong(reqString[1]) ;
						if(currentNode.nodeHasFile(hashId))
							retMsg = "RPEXISTS";
						retMsg = "RPNFL";
				break;
			// IAMPRE
			case "RQPNGPRE":
						InetSocketAddress updPredecessor = Handler.buildSocketAddress(request.split("_")[1]);
						currentNode.notified(updPredecessor);
						retMsg = "RPPNGD";
						break;
			case "RQALV":
						retMsg = "RPALV";
				break;
		}
		return retMsg;
	}
}