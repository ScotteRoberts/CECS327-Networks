import java.io.*;
import java.net.*;
import java.util.*;

/** Implementation of a early generation peer-to-peer system using a Pastry routing algorithm
 * @author Scott Roberts
 * @version 1.0
 * @date 04/27/18
 */
public class PastryClient {
	// Constants for the program.
	public static final int GUID_DIGIT_SIZE = 4;
	public static final int MAX_HOPS = GUID_DIGIT_SIZE + 2;
	public static final int NUM_OF_TRIALS = 100;
	public static final int SERVER_PORT = 32710;
	public static final String MY_IP = "52.8.111.209";
	public static final String ERROR = "error";
	public static final int TIMEOUT_MS = 500;

	/** Array used for tracking number of "hops" */
	public int[] histogram;
	
	/** Generates a random quartary number
	 * @param size		Number of digits allowed for the GUID and their max integer number minus 1 (size - 1)
	 * @return			GUID
	 */
	public String generateGUID(int size) {
		Random gen = new Random();
		String gUID = "";
		int iter = 0;
		while (iter < size) {
			gUID += String.valueOf(gen.nextInt(size));
			iter++;
		}
		return gUID;
	}

	/** Takes the input and conforms it to my standards of checking.
	 * @param reply		Reply from UDP message data
	 * @return			Sanitized reply
	 */
	private String santizeReply(String reply) {
		return reply.trim().toLowerCase();
	}

	/** Expects a standard reply that will parse out the GUID from the reply
	 * @param reply		{GUID}:{IP} formatted string
	 * @return 			{GUID}
	 */
	private String getGUID(String reply) {
		if (reply.length() > 4 && reply.charAt(4) == ':'){
			return reply.split(":")[0];
		} else if (reply.equals("null")) {
			return reply;
		} else {
			return ERROR;
		}
	}

	/** Expects a standard reply that will parse out the IP from the reply
	 * @param reply		{GUID}:{IP} formatted string
	 * @return 			{IP}
	 */
	private String getIP(String reply) {
		if (reply.length() > 4 && reply.charAt(4) == ':'){
			return reply.split(":")[1];
		} else if (reply.equals("null")) {
			return reply;
		} else {
			return ERROR;
		}
	}

	/** Large function. Attempts to find the desired startGUID by contacting multiple machine's 
	 * IPs through hops. The hops are recorded and sent to the histogram data structure.
	 * @param startIP					My server's IP address
	 * @param serverPort				Standardized server port on each machine in the graph
	 * @param startGUID					Randomized GUID
	 * @exception SocketException		Socket failure
	 * @exception IOException			IO failure
	 * 
	 */
	private void findNode (String startIP, int serverPort, String startGUID) {
		DatagramSocket aSocket = null;
		try {
			int hops = 1;
			String iP = startIP;
			String gUID = startGUID;
			System.out.println("\nStart IP: " + startIP);
			System.out.println("Start GUID: " + startGUID + "\n");

			// Init Socket
			aSocket = new DatagramSocket();
			aSocket.setSoTimeout(TIMEOUT_MS);

			while (hops < MAX_HOPS) {
				// Construct message
				byte [] m = gUID.getBytes();
				System.out.println("Hop " + hops + " : " + gUID + ":" + iP);
				InetAddress aHost = InetAddress.getByName(iP);
				DatagramPacket request = new DatagramPacket(m, m.length, aHost, serverPort);
				aSocket.send(request);
				
				// Receive Reply
				byte[] buffer = new byte[1000];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(reply);

				// Process Reply
				String stringReply = new String(reply.getData());
				stringReply = santizeReply(stringReply);
				System.out.println("Reply: " + stringReply + "\n");
				iP = getIP(stringReply);

				if (iP.equals(ERROR)) {
					System.out.println("Error in parsing...");
					break;
				}
				if (iP.equals("null")) {
					System.out.println("Found a null.");
					// Add to the list of found nulls.
					break;
				}
				if (getGUID(stringReply).equals(gUID.toString())){
					System.out.println("You found me!");
					// Add to the list of found IPs.
					break;
				}
				hops++;
			}
			if (hops < MAX_HOPS) {
				histogram[hops] += 1;
			}
			if(hops == MAX_HOPS) {
				System.out.println("Did not find node within max number of hops.");
				// Error finding the proper node. Discard the Run.
			}
			
		} 
		catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage()); 
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally { 
			if(aSocket != null) {
				aSocket.close();
			}	
		}
	}

	/** Driver for multiple trials of node finding.
	 * @param numOfTrials	Constant value of trials for analysis.
	 * 
	 */
	private void Start(int numOfTrials) {
		int trial = 0;
		while (trial < numOfTrials) {
			System.out.println("\n******* Trial #" + trial + " *********");
			String gUID = generateGUID(GUID_DIGIT_SIZE);
			findNode(MY_IP, SERVER_PORT, gUID);
			trial++;
		}
		System.out.println("Histogram of required hops:");
		for (int hop : histogram) {
			System.out.print(hop + " ");
		}
		System.out.println();
	}

	/** Driver. No arguments necessary */
	public static void main (String[] args) {
		PastryClient pC = new PastryClient();
		pC.histogram = new int[MAX_HOPS];
		pC.Start(NUM_OF_TRIALS);
	}
}
