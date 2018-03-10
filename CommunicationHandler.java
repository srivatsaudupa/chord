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
 * Communication Handler class that does the following things:
 *  *
 * @author Anusha Naik
 * @author Srivatsa Udupa
 * @author Prarthana Raghavan
 *
*/

public class CommunicationHandler {
	/* *********************************************************************************************** */
	/* 									Communication Handlers 										   */
	/* *********************************************************************************************** */
	/* 1. Request Send */
	public static String sendRequest(InetSocketAddress server, String request) {
		if (server == null || request == null)
			return null;
		Socket senderSocket = null;
		try 
		{
			senderSocket = new Socket(server.getAddress(),server.getPort());
			PrintStream output = new PrintStream(senderSocket.getOutputStream());
			output.println(request);
		} 
		catch (IOException e) 
		{
			return null;
		}
		try 
		{
			Thread.sleep(60);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		InputStream inMessageStream = null;
		try 
		{
			inMessageStream = senderSocket.getInputStream();
		} 
		catch (IOException e) 
		{
			System.out.println("Cannot get input stream from "+server.toString()+"\nFor the request is: "+request+"\n");
		}
		String response = inputStreamToString(inMessageStream);
		try 
		{
			senderSocket.close();
		} 
		catch (IOException e) {
			throw new RuntimeException(
					"Cannot close socket", e);
		}
		return response;
	}

	/* Read input stream and parse it into a string */
	public static String inputStreamToString(InputStream inMessageStream) {
		if (inMessageStream == null) {
			return null;
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(inMessageStream));
		String readline = null;
		try 
		{
			readline = reader.readLine();
		} 
		catch (IOException e) 
		{
			return null;
		}
		return readline;
	}
}