import java.net.*;

/**
 * Stabilization thread that periodically asks successor for its predecessor
 * and determine if current node should update or delete its successor.
 * @author Srivatsa Udupa
 * @author Anusha Naik
 * @author Prarthana Raghavan
 */

public class Stabilization extends Thread {
	
	private Node currentNode;
	private boolean status;

	public Stabilization(Node currentNode) {
		this.currentNode = currentNode;
		this.status = true;
	}

	@Override
	public void run() {
		while (status) {
			InetSocketAddress succNode = currentNode.getSuccessor();
			if (succNode == null || succNode.equals(currentNode.getAddress())) {
				currentNode.updateFingers(-3, null); //fill
			}
			succNode = currentNode.getSuccessor();
			if (succNode != null && !succNode.equals(currentNode.getAddress())) {

				// try to get my successor's predecessor
				InetSocketAddress preSuccNode = Handler.requestAddress(succNode, "RQEPR");

				// if bad connection with successor! delete successor
				if (preSuccNode == null) {
					currentNode.updateFingers(-1, null);
				}

				// else if successor's predecessor is not itself
				else if (!preSuccNode.equals(succNode)) {
					long currentNodeId = Handler.hashSocketAddress(currentNode.getAddress());
					long succRelativeId = Handler.computeRelativeId(Handler.hashSocketAddress(succNode), currentNodeId);
					long preSuccRelativeId = Handler.computeRelativeId(Handler.hashSocketAddress(preSuccNode),currentNodeId);
					if (preSuccRelativeId>0 && preSuccRelativeId < succRelativeId) {
						currentNode.updateFingers(1,preSuccNode);
					}
				}
				
				// successor's predecessor is successor itself, then notify successor
				else {
					currentNode.notify(succNode);
				}
			}

			try {
				Thread.sleep(60);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public void kill() {
		status = false;
	}





}
