import java.util.*;
import java.net.*; 
import java.io.*;

/** Implementation of a early generation peer-to-peer system using a Pastry routing algorithm
 * @author Scott Roberts
 * @version 1.0
 * @date 04/22/18
 */
public class PastryServer {
	private Map<String, String> leafMap;
	private Map<String, String> routeTable;
	
	/** Initializes global leafMap map */
	private void leafMapInit() {
		leafMap = new HashMap<>();
		leafMap.put("3333", "18.219.42.194");
		leafMap.put("0001", "54.67.23.56");
		leafMap.put("0012", "18.188.6.10");
		leafMap.put("0032", "18.188.30.228");
	}
	
	/** Initializes global routeTable map */
	private void routeTableInit() {
		routeTable = new HashMap<>();
		// Row 0
		routeTable.put("0", "011:52.8.111.209");
		routeTable.put("1", "111:52.14.108.231");
		routeTable.put("2", "321:54.177.170.153");
		routeTable.put("3", "310:54.177.53.121");
		// Row 1
		routeTable.put("00", "11:52.8.111.209");
		routeTable.put("01", "33:54.244.201.48");
		routeTable.put("02", "00:18.218.247.215");
		routeTable.put("03", "22:54.213.89.200");
		// Row 2
		routeTable.put("000", "1:54.67.23.56");
		routeTable.put("001", "1:52.8.111.209");
		routeTable.put("002", "2:18.188.6.10");
		routeTable.put("003", "2:18.188.30.228");
		// Row 3
		routeTable.put("0010", ":NULL");
		routeTable.put("0011", ":52.8.111.209");
		routeTable.put("0012", ":18.188.6.10");
		routeTable.put("0013", ":NULL");
	}

	/** Uses the global leafMap to search for a matching entry
	 * @param gUID		Globally Unique ID
	 * @return 			Resulting string representing (GUID):(IP) or null
	 */
	private String findLeaf(String gUID) {
		String result = null;
		String value = null;
		if(leafMap.containsKey(gUID)) {
			value = leafMap.get(gUID);
			result = gUID + ":" + value;
		}
		return result;
	}

	/** Uses the global routeTable to search for a matching entry.
	 * Checks per character of the GUID, until a match is found.
	 * If no match is found, null is returned.
	 * @param gUID		Globally Unique ID
	 * @return 			Resulting string representing (GUID):(IP) or null
	 */
	private String findRouteTableNode(String gUID) {
		String result = null;
		String tempGUID = "";
		int row = 0;
		
		while (tempGUID.length() != gUID.length()) {
			tempGUID += Character.toString(gUID.charAt(row));
			if (routeTable.containsKey(tempGUID)) {
				System.out.println("You have a hit");
				if (gUID.length() <= row + 1) {
					result = tempGUID + routeTable.get(tempGUID);
					// System.out.println("Inner TempGUID: " + tempGUID);
					// System.out.println("Inner Route Table: " + routeTable.get(tempGUID));
					// System.out.println("Inner Result: " + result);
				}
				else {
					// You have gone too far and did not find a match, backtrack.
					tempGUID = tempGUID.substring(0, row + 1);
					result = tempGUID + routeTable.get(tempGUID);
					// System.out.println("Outer TempGUID: " + tempGUID);
					// System.out.println("Outer Route Table:" + routeTable.get(tempGUID));
					// System.out.println("Outer Result: " + result);
				}
			}
			row++;
		}
		return result;
	}
	
	/** Pastry Algorithm in an executable strategy.
	 * @param message			Unsanitized, Non-validated Globally Unique ID (GUID)
	 * @return					Resulting string representing (GUID):(IP) or null
	 * @exception Exception		General Exception to represent incorrect input
	 */
	private String pastryStrategy(String message) {
		// Sanitize
		message = message.trim();

		// Validate
		try {
			if(message.length() > 4) {
				throw new Exception();
			}
			for (char ch : message.toCharArray()) {
				int value = Integer.parseInt(Character.toString(ch));
				if(value > 3 || value < 0){
					throw new Exception();
				}
			}
		}
		catch (Exception e){
			System.out.println("Invalid request: " + message);
			return "INVALID REQUEST";
		}

		// Use Validate GUID to Find Node
		String gUID = message;
		
		// Pastry Search Algorithm
		String reply = findLeaf(gUID);
		if (reply == null) {
			reply = findRouteTableNode(gUID);
			if (reply == null) {
				reply = "NULL";
			}
		}
		return reply;
	}

	/** Execution of the Pastry Server. Acts as an ongoing UDP Server that accepts
	 * small size messages containing GUID
	 * @exception SocketException		Socket failure
	 * @exception IOException			IO failure
	*/
	private void start() {
		DatagramSocket aSocket = null; 
		try {
			// CHANGE SOCKET TO 32710
			aSocket = new DatagramSocket(32710);
            byte[] buffer = new byte[1000];
			while(true) {
				// Receive
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);

				// Custom Message Handler
				String message = new String(request.getData());
				message = message.substring(0, request.getLength());
				String result = pastryStrategy(message);
				request.setData(result.getBytes());
				
				// Send
				DatagramPacket reply = new DatagramPacket(request.getData(), request.getLength(), request.getAddress(), request.getPort()); 
				aSocket.send(reply);
			}
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage()); 
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket != null) {
				aSocket.close();
			}
		}
	}
	
	public static void main(String[] args) {
		PastryServer ps = new PastryServer();
		ps.leafMapInit();
		ps.routeTableInit();
		ps.start();
	}
}