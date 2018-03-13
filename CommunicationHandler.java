import java.io.*;
import java.net.*;

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
		/* Validate Request */
		if (server == null || request == null)
			return null;
		/* Create send socket and write to the output stream */
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
			System.out.println("Input Stream Error: \nServer: "+server.toString()+"\nRequest: "+request);
		}
		
		/* Read response packets and parse */
		String response = parseIOStream(inMessageStream);
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
	public static String parseIOStream(InputStream inMessageStream) {
		if (inMessageStream == null) {
			return null;
		}
		BufferedReader msgReader = new BufferedReader(new InputStreamReader(inMessageStream));
		String readline = null;
		try 
		{
			readline = msgReader.readLine();
		} 
		catch (IOException e) 
		{
			return null;
		}
		return readline;
	}
}