import java.net.InetSocketAddress;

/**
 * Heart Beat Monitoring of node
 * Check if previous node is still up and running
 */
public class HeartBeat extends Thread {
	private Node current;
	private boolean heartBeatStatus;
	
	public HeartBeat(Node plocal) {
		current = plocal;
		heartBeatStatus = true;
	}
	
	@Override
	public void run() {
		while (heartBeatStatus) {
			InetSocketAddress prevNode = current.getPredecessor();
			if (prevNode != null) {
				String response = CommunicationHandler.sendRequest(prevNode, "KEEP");
				if (response == null || !response.equals("ALIVE")) {
					current.clearPredecessor();	
				}

			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void toDie() {
		heartBeatStatus = false;
	}
}


