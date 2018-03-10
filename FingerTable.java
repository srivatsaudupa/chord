import java.net.InetSocketAddress;
import java.util.Random;

/**
 * FingerTable access a random entry in finger table periodically and 
 * and fix it.
 */

public class FingerTable extends Thread{

	private Node local;
	Random random;
	boolean alive;

	public FingerTable (Node node) {
		local = node;
		alive = true;
		random = new Random();
	}

	@Override
	public void run() {
		while (alive) {
			int i = random.nextInt(31) + 2;
			InetSocketAddress ithfinger = local.find_nextNode(Handler.ithStart(local.getId(), i));
			local.updateFingers(i, ithfinger);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void toDie() {
		alive = false;
	}

}
