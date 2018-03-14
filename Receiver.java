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
	// Constructor
	public Receiver(Socket receiverSocket, Node currentNode)
	{
		this.receiverSocket = receiverSocket;
		this.currentNode = currentNode;
	}

	//Thread runnable
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

	/* Method to handle requests 
		Args: String request
		Return: String response 
	*/
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
		/*
			Message format - Message is separated by _, and the first string will denote the request / response code
			Eg. RQFILE_hashId - RQFILE code denotes a request for file transfer by a new node from its successor
				Response: RETFILE_file1_file2_file3 or RETFILE_NOFILE
				In each of the following cases, the Request and response codes are described
		*/
		switch(rqCode)
		{
			/* RQIM_hashId
				- Request for the immediate preceding finger table entry and return the nodeIp and address
				- Response Message: 
					- RPIM_nodeIP:nodePort
			*/
			case "RQIM":
						hashId = Long.parseLong(request.split("_")[1]);
						resNodeAddr = currentNode.closestFingerEntry(hashId);
						nodeIP = resNodeAddr.getAddress().toString();
						nodePort = resNodeAddr.getPort();
						retMsg = "RPIM_"+nodeIP+":"+nodePort;
						break;
			/* RQCSC
				- Request a node for the IP and Port number of its successor node
				- Response Message
					- RPCSC_nodeIP:nodePort
			*/
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
			/* RQEPR
				- Request a node for the IP and Port number of its predecessor node
				- Response Message
					- RPEPR_nodeIP:nodePort
			*/
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
			/* RQFSC_hashId
				- Find successor of a node using its hash ID
				- Response Message
					- RPFSC_nodeIP:nodePort
			*/
			case "RQFSC":
						hashId = Long.parseLong(request.split("_")[1]);
						resNodeAddr = currentNode.find_nextNode(hashId);
						nodeIP = resNodeAddr.getAddress().toString();
						nodePort = resNodeAddr.getPort();
						retMsg = "RPFSC_"+nodeIP+":"+nodePort;
						break;  
			/* RQFILE_hashId
				- Request files with hashId <= requested hashId
				- Response Message
					- RETFILE_filename1_filename2..._filenameN
			*/
			case "RQFILE":
						hashId = Long.parseLong(request.split("_")[1]);
						retMsg = currentNode.fetchFiles(hashId);
						break;
			/* RQFTX_filename1_filename2_.._filenameN
				- Request for transfer of files from a departing node to its successor
				- Response Message
					- RPFTXCMP
			*/
			case "RQFTX":
						for(int i=1; i<reqString.length;i++)
						{
							currentNode.updateFileTable(reqString[i]);
						}
						retMsg = "RPFTXCMP";
						break;
			/* RQCHF_hashId
				- Request a node if a file with hashId exists in it
				- Response Message
					- RPEXISTS - if file exists in the current node
					- RONFL - if no such file exists in the current node
			*/
			case "RQCHF":
						hashId = Long.parseLong(reqString[1]) ;
						if(currentNode.nodeHasFile(hashId))
							retMsg = "RPEXISTS";
						else
							retMsg = "RPNFL";
				break;
			/* RQPNGPRE_InetSocketAddress
				- Ping a node to notify that the pinging node is now its predecessor
				- Response Message
					- RPPNGD
			*/
			case "RQPNGPRE":
						InetSocketAddress updPredecessor = Handler.buildSocketAddress(request.split("_")[1]);
						currentNode.notified(updPredecessor);
						retMsg = "RPPNGD";
						break;
			/* RQALV
				- Request the status of a node (Heart Beat Monitoring)
				- Response Message
					- RPALV
			*/
			case "RQALV":
						retMsg = "RPALV";
				break;
		}
		return retMsg;
	}
}