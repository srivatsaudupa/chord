import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listener thread that keeps listening to a port and asks talker thread to process
 * when a request is accepted.
 * @author Anusha
 * @author Prarthana Raghavan
 * @author Srivatsa
 *
 */

public class Listener extends Thread {

	private Node local;
	private ServerSocket serverSocket;
	private boolean status;

	public Listener (Node n) {
		local = n;
		status = true;
		InetSocketAddress localAddress = local.getAddress();
		int port = localAddress.getPort();

		//open server/listener socket
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			throw new RuntimeException("\nCannot open listener port "+port+". Now exit.\n", e);
		}
	}

	@Override
	public void run() {
		while (status) {
			Socket talkSocket = null;
			try {
				talkSocket = serverSocket.accept();
			} catch (IOException e) {
				throw new RuntimeException(
						"Cannot accepting connection", e);
			}

			//new talker
			new Thread(new Talker(talkSocket, local)).start();
		}
	}

	public void kill() {
		status = false;
	}
}
