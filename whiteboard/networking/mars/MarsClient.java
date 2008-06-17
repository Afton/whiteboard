/**
 * 
 */
package whiteboard.networking.mars;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import whiteboard.core.WhiteboardCore;
import whiteboard.networking.NetworkingUtils;
import whiteboard.networking.Peer;

/**
 * @author patrick
 */
public class MarsClient {
	public static String name;
	public static int port;
	private static Peer peer;

	public static void connect() throws IOException {
		if (!peer.isConnected() || peer.isClosed()) {
			// Connect to peer
			peer.connect();

			// Send handshake
			initiateHandshake();
		}
	}

	public static void connectTo(final Peer newPeer) throws IOException {
		final Peer oldPeer = peer;
		peer = newPeer;

		// Close the old connection if it's still open
		if (null != oldPeer) {
			oldPeer.close();
		}

		// Connect to peer
		connect();
	}

	public static boolean createWhiteboard(final WhiteboardCore whiteboard) {
		boolean created = false;

		// Add whiteboard
		if (MarsProtocol.addWhiteboard(whiteboard)) {
			// Create message
			final StringBuffer messageBuffer = new StringBuffer();
			messageBuffer.append("CREATE" + MarsProtocol.LINE_END);
			messageBuffer.append(whiteboard.getName() + MarsProtocol.LINE_END);
			messageBuffer.append(whiteboard.getPermissionLevel().ordinal() + MarsProtocol.LINE_END);
			messageBuffer.append(whiteboard.getHead().getName() + MarsProtocol.LINE_END);
			messageBuffer.append(whiteboard.getHead().getPort() + MarsProtocol.LINE_END);

			// Indiciate end of message
			messageBuffer.append(MarsProtocol.LINE_END);

			// Get message as bytes
			final byte[] message = messageBuffer.toString().getBytes();

			// Send message
			MarsProtocol.send(message);

			// The whiteboard has been created
			created = true;
		}

		return created;
	}

	public static boolean deleteWhiteboard(final String whiteboardName) {
		boolean deleted = false;

		// Delete whiteboard
		if (MarsProtocol.removeWhiteboard(whiteboardName)) {
			// Create message
			final StringBuffer messageBuffer = new StringBuffer();
			messageBuffer.append("DELETE" + MarsProtocol.LINE_END);
			messageBuffer.append(whiteboardName + MarsProtocol.LINE_END);

			// Indiciate end of message
			messageBuffer.append(MarsProtocol.LINE_END);

			// Get message as bytes
			final byte[] message = messageBuffer.toString().getBytes();

			// Send message
			MarsProtocol.send(message);

			deleted = true;
		}

		return deleted;
	}

	public static boolean deleteWhiteboard(final WhiteboardCore whiteboard) {
		return deleteWhiteboard(whiteboard.getName());
	}

	public static void getPeers() throws IOException {
		// If peer is null, just return
		if (null == peer) {
			return;
		}

		// Check if peer is closed, if so, open connection
		if (peer.isClosed()) {
			// Open connection
			connect();
		}

		// Create message
		final StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append("GET PEERS" + MarsProtocol.LINE_END);

		// Indiciate end of message
		messageBuffer.append(MarsProtocol.LINE_END);

		// Get message as bytes
		byte[] packet = messageBuffer.toString().getBytes();

		// Send message to peer
		peer.send(packet);

		// Get response header
		packet = MarsProtocol.createPacket();
		int bytesReceived = peer.receive(packet);

		String[] message = NetworkingUtils.getMessage(packet, bytesReceived);

		// Check message
		if (message[0].equals("PEERS")) {
			final int numPeers = Integer.parseInt(message[1]);
			final int length = Integer.parseInt(message[2]);

			// Get response body
			packet = MarsProtocol.createPacket(length);
			bytesReceived = peer.receive(packet);
			message = NetworkingUtils.getMessage(packet, bytesReceived);

			// Set the peers
			MarsProtocol.setPeers(extractPeers(message, numPeers));

			// Add connected peer
			final Peer connectedPeer = MarsProtocol.createPeer(peer.getName(), NetworkingUtils.convertIPtoArray(peer.getIP()), peer.getPort());
			connectedPeer.connect();
			MarsProtocol.addPeer(connectedPeer);
		} else {
			System.err.println("MARSCLNT: Received an invalid server peer response.");
		}
	}

	public static void getUpdate() throws IOException {
		// If peer is null, just return
		if (null == peer) {
			// move to next peer in list
			return;
		}

		// If peer is closed, open connection
		if (peer.isClosed()) {
			connect();
		}

		// Get peers
		getPeers();

		// Get list of whiteboards
		getWhiteboards();

		// Close connection
		peer.close();
	}

	public static void getUpdateFrom(final Peer updatePeer) throws IOException {
		// Connect to peer
		connectTo(updatePeer);

		// Get update
		getUpdate();
	}

	public static void getWhiteboards() throws IOException {
		// If peer is null, just return
		if (null == peer) {
			return;
		}

		// Check if peer is closed, if so, open connection
		if (peer.isClosed()) {
			// Open connection
			connect();
		}

		// Create message
		final StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append("GET WHITEBOARDS" + MarsProtocol.LINE_END);

		// Indiciate end of message
		messageBuffer.append(MarsProtocol.LINE_END);

		// Get message as bytes
		byte[] packet = messageBuffer.toString().getBytes();

		// Send message to peer
		peer.send(packet);

		// Get response header
		packet = MarsProtocol.createPacket();
		int bytesReceived = peer.receive(packet);

		String[] message = NetworkingUtils.getMessage(packet, bytesReceived);

		// Check message
		if (message[0].equals("WHITEBOARDS")) {
			final int numBoards = Integer.parseInt(message[1]);
			final int length = Integer.parseInt(message[2]);

			// Get response body
			packet = MarsProtocol.createPacket(length);
			bytesReceived = peer.receive(packet);
			message = NetworkingUtils.getMessage(packet, length);

			// Set the whiteboards
			MarsProtocol.setWhiteboards(extractWhiteboards(message, numBoards));
		} else {
			System.err.println("MARSCLNT: Received an invalid server whiteboard response.");
		}
	}

	public static boolean initiateHandshake() throws IOException {
		boolean status = false;
		
		// Create message
		final StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append("HELLO " + MarsProtocol.PROTOCOL + MarsProtocol.LINE_END);
		messageBuffer.append(name + MarsProtocol.LINE_END);
		messageBuffer.append(MarsServer.getPort() + MarsProtocol.LINE_END);

		// Indiciate end of message
		messageBuffer.append(MarsProtocol.LINE_END);

		// Get message as bytes
		byte[] packet = messageBuffer.toString().getBytes();

		// Send message to peer
		peer.send(packet);

		// Get handshake reply
		packet = MarsProtocol.createPacket();
		final int bytesReceived = peer.receive(packet);

		if (-1 != bytesReceived) {
			final String[] message = NetworkingUtils.getMessage(packet, bytesReceived);
	
			// Check message
			if (message[0].equals(MarsProtocol.PROTOCOL + " OK")) {
				peer.setName(message[1]);
				status = true;
			} else {
				System.err.println("MARSCLNT: Received an invalid handshake reply.");
			}
		}

		return status;
	}

	/**
	 * @param message
	 * @param numPeers
	 * @return
	 */
	private static List<Peer> extractPeers(final String[] message, final int numPeers) {
		final List<Peer> peers = new ArrayList<Peer>(numPeers);

		// Loop through message getting peers
		if (0 < numPeers) {
			for (int i = 0; (i + 2) < message.length; i+=3) {
				// Get the peer information
				final String peerName = message[i];
				final byte[] peerAddr = NetworkingUtils.convertIPtoArray(message[i+1]);
				int peerPort = Integer.parseInt(message[i+2]);

				try {
					// Create peer and connect
					final Peer newPeer = MarsProtocol.createPeer(peerName, peerAddr, peerPort);
					newPeer.connect();

					// Add peer to list
					peers.add(newPeer);
				} catch (final IOException ioe) {
					// Do nothing
				}
			}
		}

		return peers;
	}

	/**
	 * @param message
	 */
	private static List<WhiteboardCore> extractWhiteboards(final String[] message, final int numBoards) {
		final List<WhiteboardCore> whiteboards = new ArrayList<WhiteboardCore>(numBoards);

		// Loop through message getting whiteboards
		if (0 < numBoards) {
			for (int i = 0; (i + 3) < message.length; i+=5) {
				try {
					// Get the whiteboard name
					final String boardName = message[i];
					
					// Get the permission level
					int permLevel = Integer.valueOf(message[i+1]);
					WhiteboardCore.WB_PERM_LEVEL permissionLevel = null;
					
					for (WhiteboardCore.WB_PERM_LEVEL wbPermLevel : WhiteboardCore.WB_PERM_LEVEL.values()) {
						if (wbPermLevel.ordinal() == permLevel) {
							permissionLevel = wbPermLevel;
							break;
						}
					}

					// Get the whiteboard information
					final String headName = message[i+2];
					int whiteboardPort = Integer.parseInt(message[i+3]);
					
					Peer headPeer = MarsProtocol.createPeer(headName, new byte[4], -1);
					final int headIndex = MarsProtocol.getPeers().indexOf(headPeer);

					if (-1 != headIndex) {
						final byte[] ip = MarsProtocol.getPeers().get(headIndex).getAddress().getAddress();
	
						// Add the whiteboard to the list
						try {
							whiteboards.add(new WhiteboardCore(boardName, permissionLevel, peer.getName(), ip, whiteboardPort));
						} catch (final IOException ioe) {
							// Do nothing
						}
					}
				} catch (IOException ioe) {
					// Do nothing
				}
			}
		}

		return whiteboards;
	}
}
