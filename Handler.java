import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * A Handler class that does the following things:
 *       - Hashing - for the IP address, file names and other strings
 *       - Relative distance of node from another (relative measurements), 
 *       - Routing services for the networking finger table  
 *
 * @author Anusha Naik
 * @author Srivatsa Udupa
 * @author Prarthana Raghavan
 *
*/

public class Handler {

	private static HashMap<Integer, Long> powerOfTwo = null;

	/**
	 * Initialize the handler function to power of two for 32 finger table entries
	 */
	public Handler() {
		// use the two table maps
		powerOfTwo = new HashMap<Integer, Long>();
		long base = 1;
		for (int i = 0; i <= 32; i++) {
			powerOfTwo.put(i, base);
			base *= 2;
		}
	}

	/**
	 * Caculate an identifier for the socket
	 * Args: 
	 *       addr: address for which we have to hash the value
	 *
	 * Returns:
	 *       hash value of long type
	 */
	public static long hashSocketAddress (InetSocketAddress addr) {
		int i = addr.hashCode();
		return hashHashCode(i);
	}

	/**
	 * Compute the hash value for the passed string
	 * Args:
	 *       s: string for which we have to compute the hash value
	 *
	 * Returns:
	 *        The ID for the string to long type
	 */
	public static long hashString (String s) {
		int i = s.hashCode();
		return hashHashCode(i);
	}

		
	/**
	 * Compute the hash value for the passed integer(32-b)
	 * Args:
	 *       s: string for which we have to compute the hash value
	 *
	 * Returns:
	 *        The ID for the integer to long type
	 */	
	private static long hashHashCode(int i) {

		//cycle the bits to calculate the message digest
		byte[] hashbytes = new byte[4];
		hashbytes[0] = (byte) (i >> 24);
		hashbytes[1] = (byte) (i >> 16);
		hashbytes[2] = (byte) (i >> 8);
		hashbytes[3] = (byte) (i /*>> 0*/);

		// Initialize message digest to use SHA1
		MessageDigest md =  null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// converting and compressing byte[4] for achieving the same
		if (md != null) {
			md.reset();
			md.update(hashbytes);
			byte[] result = md.digest();

			byte[] compressed = new byte[4];
			for (int j = 0; j < 4; j++) {
				byte temp = result[j];
				for (int k = 1; k < 5; k++) {
					temp = (byte) (temp ^ result[j+k]);
				}
				compressed[j] = temp;
			}

			long ret =  (compressed[0] & 0xFF) << 24 | (compressed[1] & 0xFF) << 16 | (compressed[2] & 0xFF) << 8 | (compressed[3] & 0xFF);
			ret = ret&(long)0xFFFFFFFFl;
			return ret;
		}
		return 0;
	}

	/**
	 * Compute Normalization relative ID to universal to local
	 * Assume: local node - 0
	 * Args:
	 *       universal: The universal ID
	 *       local : The local ID
	 *
	 * Returns:
	 *       ret: The difference between the Ids 
	 */
	public static long computeRelativeId(long universal, long local) {
		long ret = universal - local;
		if (ret < 0) {
			ret += powerOfTwo.get(32);
		}
		return ret;
	}	

	/**
	 * Provide the ith finger table entry of the nodeID
	 * Args:
	 *       nodeid: the nodeID required for the finger table 
	 * 
	 * Returns:
	 *       nodeid with powerOfTwo
	 */
	public static long ithStart(long nodeid, int i) {
		return (nodeid + powerOfTwo.get(i-1)) % powerOfTwo.get(32);
	}

	/**
	 * Request address for server 
	 * Args:
	 *       server: The server address
	 *       req: The request placed at the server
	 * 
	 * Returns:
	 *       The requested address
	 */
	public static InetSocketAddress requestAddress(InetSocketAddress server, String req) {
		// invalid values for the parameters
		if (server == null || req == null) {
			return null;
		}
		// Compute response for the sent request
		String response = CommunicationHandler.sendRequest(server, req);
		// if response is absent - return null
		if (response == null) {
			return null;
		}
		// or return server itself 
		else if (response.startsWith("NOTHING"))
			return server;
		// return the created socket address
		else {
			InetSocketAddress ret = Handler.buildSocketAddress(response.split("_")[1]);
			return ret;
		}
	}

	/**
	 * Create InetSocketAddress using ip address and port number
	 * Create the socket address from the string 
	 * Args:
	 *       addr: The address in string format 
	 * 
	 * Returns:
	 *       returns the computed socket address
	 */
	public static InetSocketAddress buildSocketAddress(String addr) {
		
		if (addr == null) {
			return null;
		}

		
		String[] splitted = addr.split(":");
		if (splitted.length >= 2) {
			String ip = splitted[0];
			if (ip.startsWith("/")) {
				ip = ip.substring(1);
			}
			InetAddress m_ip = null;
			try {
				m_ip = InetAddress.getByName(ip);
			} catch (UnknownHostException e) {
				System.out.println("Cannot create ip address: "+ip);
				return null;
			}


			String port = splitted[1];
			int m_port = Integer.parseInt(port);

			return new InetSocketAddress(m_ip, m_port);
		}

		else {
			return null;
		}

	}

	public static InetSocketAddress fileSearch(InetSocketAddress localAddress, long hashValue)
	{
		InetSocketAddress nodeAddr = Handler.requestAddress(localAddress, "FINDSUCC_"+hashValue);
		String response = CommunicationHandler.sendRequest(nodeAddr, "CHECKFILE_"+hashValue);
		if(response.equals("EXISTS"))
			return nodeAddr;
		return null;
	}

}