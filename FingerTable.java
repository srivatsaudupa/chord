import java.net.InetSocketAddress;
import java.util.Random;

/**
 * Maintains a thread for Finger Table checks
 * Access a random entry in finger table periodically and 
 * and fix it.
 * @author Anusha Naik
 * @author Srivatsa Udupa
 * @author Prarthana Raghavan
 */


public class FingerTable extends Thread{

	private Node currentNode;
	Random random;
	boolean status;

	public FingerTable (Node node) {
		currentNode = node;
		status = true;
		random = new Random();
	}

	@Override
	public void run() {
		while (status) {
			int i = random.nextInt(31) + 2;
			InetSocketAddress ithfinger = currentNode.find_nextNode(Handler.ithStart(currentNode.getId(), i));
			currentNode.updateFingers(i, ithfinger);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Terminate process
	public void kill() {
		status = false;
	}

}
