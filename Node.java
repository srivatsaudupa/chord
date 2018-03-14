
/**
 * Node - representing each IP joining the chord ring
 * @author Anusha
 * @author Srivatsa
 * @author Prarthana Raghavan
 *
 */

import java.net.*;
import java.util.*;
import java.io.*;

public class Node {

	private long nodeID;
	private InetSocketAddress nodeAddress;
	private InetSocketAddress prevNode;
	private HashMap<Integer, InetSocketAddress> fingerTable;
	private HashMap<Long, String> filesTable;
	private InetSocketAddress nextNode;
	private Listener listenerObj;
	private Stabilization stabilizeObj;
	private FingerTable fingerTableObj;
	private HeartBeat heartBeatObj;

	/**
	 * Constructor
	 * Args:
	 *       address: local address of current node
	 **/
	public Node (InetSocketAddress address) {
		nodeAddress = address;
		nodeID = Handler.hashSocketAddress(nodeAddress);

		// Create an empty fingerTable for maintaining 32 entries
		fingerTable = new HashMap<Integer, InetSocketAddress>();
		for (int i = 1; i <= 32; i++) {
			updateIthFinger (i, null);
		}

		
		prevNode = null;

		// Initialize for enabling them to update after every operation
		listenerObj = new Listener(this);
		stabilizeObj = new Stabilization(this);
		fingerTableObj = new FingerTable(this);
		heartBeatObj = new HeartBeat(this);
		nodeFileSystemUpdate();
	}
	
	/**
	 * Get methods
	 * Args:
	 *       No arguments
	 *
	 * Returns:
	 *       The hash value/address/predecessor/nextNodes
	 */

	public long getId() 
	{
		return nodeID;
	}
	public InetSocketAddress getAddress() 
	{
		return nodeAddress;
	}
	public InetSocketAddress getPredecessor() 
	{
		return prevNode;
	}
	public InetSocketAddress getSuccessor() {
		if (fingerTable != null && fingerTable.size() > 0) {
			return fingerTable.get(1);
		}
		return null;
	}

	/**
	 * Creating or joining a ring 
	 * Args:
	 *       contact: The address passed for reaching a node - IP address
	 * 
	 *  Returns:
	 *        true: if joined/created successfully
	 *        false: if not
	 */
	public boolean joinRing(InetSocketAddress connectorNodeAddr) 
	{
		// If the address is null or is equal to current IP address - then it will be creating
		if (connectorNodeAddr != null && !connectorNodeAddr.equals(nodeAddress)) {
			nextNode = Handler.requestAddress(connectorNodeAddr, "RQFSC_" + nodeID);
			if (nextNode == null)  {
				System.out.println("\nError: Unable to locate the node.\n");
				return false;
			}
			updateIthFinger(1, nextNode);
		}

		// As soon as any node creates/joins the network - values to be updated : 
		// finger table values, predecessor and nextNodes	
		listenerObj.start();
		stabilizeObj.start();
		fingerTableObj.start();
		heartBeatObj.start();
		if(connectorNodeAddr != null && !connectorNodeAddr.equals(nodeAddress))
		{
			fileExchange();
		}

		return true;
	}

	/**
	 * Provide the predecessor address to the nextNode
	 * Args:
	 * 		nextNode : nextNode address
	 *
	 *  Returns:
	 *  	response given by the send request/null
	 */
	public String notify(InetSocketAddress nextNode) {
		if (nextNode!=null && !nextNode.equals(nodeAddress))
			return CommunicationHandler.sendRequest(nextNode, "RQPNGPRE_"+nodeAddress.getAddress().toString()+":"+nodeAddress.getPort());
		else
			return null;
	}

	/**
	 * Update predecessor on being evoked by another node
	 * Args:
	 *       new pre: new predecessor address given
	 *
	 * Returns:
	 *       returns nothing
	 */
	public void notified (InetSocketAddress newpre) {
		if (prevNode == null || prevNode.equals(nodeAddress)) {
			this.setPredecessor(newpre);
		}
		else {
			long oldPreId = Handler.hashSocketAddress(prevNode);
			long localRelativeId = Handler.computeRelativeId(nodeID, oldPreId);
			long newpreRelativeId = Handler.computeRelativeId(Handler.hashSocketAddress(newpre), oldPreId);
			if (newpreRelativeId > 0 && newpreRelativeId < localRelativeId)
				this.setPredecessor(newpre);
		}
	}
	public InetSocketAddress find_nextNode (long id) {

		// Current Node nextNode assigned to return value
		InetSocketAddress ret = this.getSuccessor();

		// find the predecessor of the given ID
		InetSocketAddress pre = find_predecessor(id);

		// If predecessor is not the local node - then request the nextNode
		if (!pre.equals(nodeAddress))
			ret = Handler.requestAddress(pre, "RQCSC");

		// Return the local address in case of no nextNode found
		if (ret == null)
			ret = nodeAddress;

		return ret;
	}

	/**
	 * Request current node for provided ID predecessor
	 * Args:
	 *      findid: The Id for locating the predecessor
	 *
	 * Returns:
	 *      return the valid predecessor for the requested ID
	 */
	private InetSocketAddress find_predecessor (long findid) {
		InetSocketAddress curNode = this.nodeAddress;
		InetSocketAddress curNodeSuccessor = this.getSuccessor();
		InetSocketAddress mostRecentlyAlive = this.nodeAddress;
		long curNodeSuccessorRelativeId = 0;
		if (curNodeSuccessor != null)
			curNodeSuccessorRelativeId = Handler.computeRelativeId(Handler.hashSocketAddress(curNodeSuccessor), Handler.hashSocketAddress(curNode));
		long findidRelativeId = Handler.computeRelativeId(findid, Handler.hashSocketAddress(curNode));

		while (!(findidRelativeId > 0 && findidRelativeId <= curNodeSuccessorRelativeId)) {

			
			InetSocketAddress tempCurNode = curNode;

			// Current node and local node same - return the preceding finger which is nearest
			if (curNode.equals(this.nodeAddress)) {
				curNode = this.closestFingerEntry(findid);
			}

			// Otherwise fetch the nearest node of the requested node
			else {
				InetSocketAddress result = Handler.requestAddress(curNode, "RQIM_" + findid);

				// Absence of response, fetch the nextNode current node 
				if (result == null) {
					curNode = mostRecentlyAlive;
					curNodeSuccessor = Handler.requestAddress(curNode, "RQCSC");
					if (curNodeSuccessor==null) {
						System.out.println("It's not possible.");
						return nodeAddress;
					}
					continue;
				}

				// if nearest is same - return current node
				else if (result.equals(curNode))
					return result;

				// Or nearest node to result
				else {	
					// The recently active node is assigned as the current node
					mostRecentlyAlive = curNode;		
					// request the sucessor of the resultant node
					curNodeSuccessor = Handler.requestAddress(result, "RQCSC");	
					// On response, current node is the result node
					if (curNodeSuccessor!=null) {
						curNode = result;
					}
					// On no response/null response - we request the nextNode of the current node
					else {
						curNodeSuccessor = Handler.requestAddress(curNode, "RQCSC");
					}
				}

				// Variables needed for the loop structure
				curNodeSuccessorRelativeId = Handler.computeRelativeId(Handler.hashSocketAddress(curNodeSuccessor), Handler.hashSocketAddress(curNode));
				findidRelativeId = Handler.computeRelativeId(findid, Handler.hashSocketAddress(curNode));
			}
			if (tempCurNode.equals(curNode))
				break;
		}
		return curNode;
	}

	/**
	 * Closest finger table entry's node address
	 * Args:
	 *       findid: Id for locating the closest node from the finger table
	 *
	 * Returns:
	 *       closest finger preceding node's socket address
	 */
	public InetSocketAddress closestFingerEntry(long findid) {
		long findidRelative = Handler.computeRelativeId(findid, nodeID);

		// check the finger tables from the nodes with the maximum hops/distance
		for (int i = 32; i > 0; i--) {
			InetSocketAddress ithFinger = fingerTable.get(i);
			if (ithFinger == null) {
				continue;
			}
			long ithFingerId = Handler.hashSocketAddress(ithFinger);
			long ithFingerRelativeId = Handler.computeRelativeId(ithFingerId, nodeID);

			// In case of relative id being nearest - check for life
			if (ithFingerRelativeId > 0 && ithFingerRelativeId < findidRelative)  {
				String response  = CommunicationHandler.sendRequest(ithFinger, "RQALV");

				//If alive, return the same
				if (response!=null &&  response.equals("RPALV")) {
					return ithFinger;
				}

				// Or remove from the finger table
				else {
					updateFingers(-2, ithFinger);
				}
			}
		}
		return nodeAddress;
	}


	/**
	 * Update the finger table based on parameters(for multiple threads simultaneaously)
	 * Args:
	 *       i: index for finger table entry
	 *       value: address to be updated to
	 *
	 * Returns:
	 *       returns nothing
	 */
	public synchronized void updateFingers(int i, InetSocketAddress value) {

		// Check for index validity
		if (i > 0 && i <= 32) {
			updateIthFinger(i, value);
		}

		// deletion operation
		else if (i == -1) {
			deleteSuccessor();
		}

		// deletion of specified entry in FT
		else if (i == -2) {
			deleteCertainFinger(value);

		}

		// fill the nextNode values
		else if (i == -3) {
			fillSuccessor();
		}

	}


	/**
	 * Update ith entry in finger table
	 * Args:
	 *      i: Index of finger table entry to be changed
	 *      value: Value to be updated to
	 *
	 * Returns:
	 *      returns nothing 
	 */
	private void updateIthFinger(int i, InetSocketAddress value) {
		fingerTable.put(i, value);
		// if new is the local node - notify accordingly to the nextNode
		if (i == 1 && value != null && !value.equals(nodeAddress)) {
			notify(value);
		}
	}

	/**
	 * Delete nextNode finger tables entries along with it
	 */
	private void deleteSuccessor() {
		nextNode = getSuccessor();

		//empty return
		if (nextNode == null)
			return;

		// find the last existence of nextNode in the finger table
		int i = 32;
		for (i = 32; i > 0; i--) {
			InetSocketAddress ithFinger = fingerTable.get(i);
			if (ithFinger != null && ithFinger.equals(nextNode))
				break;
		}

		// delete it, from the last existence to the first one
		for (int j = i; j >= 1 ; j--) {
			updateIthFinger(j, null);
		}

		// if predecessor is nextNode, delete it
		if (prevNode!= null && prevNode.equals(nextNode))
			setPredecessor(null);

		// try to fill nextNode
		fillSuccessor();
		nextNode = getSuccessor();

		// if nextNode is still null or local node, 
		// and the predecessor is another node, keep asking 
		// it's predecessor until find local node's new nextNode
		if ((nextNode == null || nextNode.equals(nextNode)) && prevNode!=null && !prevNode.equals(nodeAddress)) {
			InetSocketAddress p = prevNode;
			InetSocketAddress pPre = null;
			while (true) {
				pPre = Handler.requestAddress(p, "RQEPR");
				if (pPre == null)
					break;

				// if p's predecessor is node is just deleted, 
				// or itself (nothing found in p), or local address,
				// p is current node's new nextNode, break
				if (pPre.equals(p) || pPre.equals(nodeAddress)|| pPre.equals(nextNode)) {
					break;
				}

				// else, keep asking
				else {
					p = pPre;
				}
			}

			// update nextNode
			updateIthFinger(1, p);
		}
	}

	/**
	 * Delete a node from the finger table(all instances)
	 * Args:
	 *     f: The address of the node to be deleted
	 *
	 * Returns:
	 *     returns nothing
	 */
	private void deleteCertainFinger(InetSocketAddress f) {
		for (int i = 32; i > 0; i--) {
			InetSocketAddress ithFinger = fingerTable.get(i);
			if (ithFinger != null && ithFinger.equals(f))
				fingerTable.put(i, null);
		}
	}

	/**
	 * Filling operation directed to either nextNode or predecessor for updating finger table
	 */
	private void fillSuccessor() {
		InetSocketAddress nextNode = this.getSuccessor();
		if (nextNode == null || nextNode.equals(nodeAddress)) {
			for (int i = 2; i <= 32; i++) {
				InetSocketAddress ithFinger = fingerTable.get(i);
				if (ithFinger!=null && !ithFinger.equals(nodeAddress)) {
					for (int j = i-1; j >=1; j--) {
						updateIthFinger(j, ithFinger);
					}
					break;
				}
			}
		}
		nextNode = getSuccessor();
		if ((nextNode == null || nextNode.equals(nodeAddress)) && prevNode!=null && !prevNode.equals(nodeAddress)) {
			updateIthFinger(1, prevNode);
		}

	}


	/**
	 * Set predecessor.to null
	 */
	public void clearPredecessor () {
		setPredecessor(null);
	}

	/**
	 * Assigning a new value to a predecessor.
	 * Args:
	 *       pre: the new value to be assigned to the predecessor
	 *
	 * Returns:
	 *       returns nothing
	 */
	private synchronized void setPredecessor(InetSocketAddress pre) {
		prevNode = pre;
	}


	

	/**
	 * Printing functionalities
	 * Args:
	 *        No arguments
	 *
	 * Returns:
	 *        returns nothing
	 */

	public void displayNodeInformation(String currentIP) {
		System.out.println("\n************ Current Node Information ***************");
		Long nodeID = Handler.hashSocketAddress(nodeAddress);
		System.out.println("Current Node Access point:\n\tIP Address: "+currentIP+"\n\tPort: "+nodeAddress.getPort()+"\n\tNode ID: "+nodeID);
		InetSocketAddress nextNode = fingerTable.get(1);
		// update that the nextNode and predecessor as pointing to the current node in case of absence
		System.out.println("\nNode Successor and Predecessor Information");
		System.out.println("---------------------------------------------");
		if ((prevNode == null || prevNode.equals(nodeAddress)) && (nextNode == null || nextNode.equals(nodeAddress))) {
			System.out.println("Predecessor: None");
			System.out.println("Successor: None");
			System.out.println("This Node is the only running node in the Ring");

		}
		
		// Otherwise assign it as null
		else {
			if (prevNode != null) {
				System.out.println("Predecessor: "+prevNode.getAddress().toString()+", "
						+ "port "+prevNode.getPort()+ ", position "+Handler.hashSocketAddress(prevNode)+".");
			}
			else {
				System.out.println("Predecessor is in the process of joining/updating.");
			}

			if (nextNode != null) {
				System.out.println("Successor:"+nextNode.getAddress().toString()+", "
						+ "port "+nextNode.getPort()+ ", position "+Handler.hashSocketAddress(nextNode)+".");
			}
			else {
				System.out.println("Successor is in the process of joining/updating");
			}
		}
	}

	public void displayFingerTable() {
		System.out.println("\n********************* Finger Table ***********************************************");
		System.out.println("#\tNode Address (IP Address:Port)\tNodeID");
		System.out.println("\n**********************************************************************************");
		for (int i = 1; i <= 32; i++) {
			long ithstart = Handler.ithStart(Handler.hashSocketAddress(nodeAddress),i);
			InetSocketAddress fingerTableEntry = fingerTable.get(i);
			StringBuilder sb = new StringBuilder();
			sb.append(i+"\t"+ "\t");
			if (fingerTableEntry!= null)
				sb.append(fingerTableEntry.toString()+"\t"+Handler.hashSocketAddress(fingerTableEntry));
			else 
				sb.append("NULL");
			System.out.println(sb.toString());
		}
		System.out.println("\n********************************************************************************\n");
	}

	/**
	 * Terminating all the threads
	 * Args:
	 *      No arguments
	 *
	 * Returns:
	 *      returns nothing.
	 */
	public void terminateNodeInstance() {
		InetSocketAddress nextNode = this.getSuccessor();
		if(nextNode != null)
			this.handFilesOver(nextNode);
		if (listenerObj != null)
			listenerObj.kill();
		if (fingerTableObj != null)
			fingerTableObj.kill();
		if (stabilizeObj != null)
			stabilizeObj.kill();
		if (heartBeatObj != null)
			heartBeatObj.kill();
	}

	/* ******************************** Node File System ************************************** */
	private void nodeFileSystemUpdate()
	{	
		try{
			File fileFolder = new File("Files");
			File[] filesList = fileFolder.listFiles();
			String filename = null;
			filesTable = new HashMap<Long, String>();
			for(int i=0;i<filesList.length;i++)
			{
				filename = filesList[i].getName();
				filesTable.put(Handler.hashString(filename), filename);
			}
		}
		catch(Exception e)
		{
			System.out.println("Files System error. Mount file folder is invalid");
			System.exit(0);	
		}
	}
	public boolean nodeHasFile(long filehash)
	{
		if(this.filesTable.containsKey(filehash))
		{
			return true;
		}
		return false;
	}

	public void fileExchange()
	{
		InetSocketAddress nextNode = this.find_nextNode(nodeID);
		String request = "RQFILE_"+nodeID;
		String response = CommunicationHandler.sendRequest(nextNode, request);
		String[] splitResponse = response.split("_");
		if(splitResponse[1].equals("NOFILE"))
			return;
		for(int i=1; i<splitResponse.length;i++)
		{
			this.updateFileSystem(splitResponse[i]);
		}
	}

	public void updateFileSystem(String response)
	{
		long fileHash;
		try
		{
			File newFile = new File("Files/"+response);
			boolean nFile = newFile.createNewFile();
			fileHash = Handler.hashString(response);
			this.filesTable.put(fileHash, response);
		}
		catch(Exception e)
		{
			System.out.println("Cannot open file");
		}

	}

	public String fetchFiles(long id)
	{
		String filename = null;
		String response = "RETFILE";
		ArrayList<Long> removeList = new ArrayList<Long>();
		long currentNodeId = this.getId();

		for(long fileId: this.filesTable.keySet())
		{
			if(fileId <= id && (fileId > currentNodeId || currentNodeId > id))
			{
				filename = this.filesTable.get(fileId);
				try
				{
					File sendFile = new File("Files/"+filename);
					if(sendFile.exists())
					{
						response += "_"+filename;
						removeList.add(fileId);
						sendFile.delete();
					}	

				}
				catch(Exception e)
				{
					System.out.println("No File Found");
				}
			}
		}
		if(response.equals("RETFILE"))
		{
			return response + "_NOFILE";
		}
		else if(!removeList.isEmpty()) {
			for(Long element:removeList)
			{
				this.filesTable.remove(element);
			}
		}
		return response;
	}

	public boolean handFilesOver(InetSocketAddress nextNode)
	{
		String request = "RQFTX";
		ArrayList<Long> removeList = new ArrayList<Long>();
		for(long fileId: this.filesTable.keySet())
		{
			request += "_"+this.filesTable.get(fileId);
			removeList.add(fileId);
		}
		String response = CommunicationHandler.sendRequest(nextNode, request);

		if(response.equals("RPFTXCMP"))
		{
			if(!removeList.isEmpty()){
				for(long hashId:removeList)
				{
					File delFile = new File("Files/"+this.filesTable.get(hashId));
					try{
						delFile.delete();
						this.filesTable.remove(hashId);
					}	
					catch(Exception e)
					{
						System.out.println("No File Found");
					}
				}
			}
			return true;
		}
		return false;
	}
	public boolean updateFileTable(String filename)
	{
		try
		{
			File movFile = new File("Files/"+filename);
			long fileHash = Handler.hashString(filename);
			this.filesTable.put(fileHash, filename);
			if(movFile.createNewFile())
				return true;
			return false;
		}
		catch(Exception e)
		{
			System.out.println("No File Found");
			return false;
		}
	}
}
