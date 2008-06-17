/**
 * 
 */
package whiteboard.networking.venus;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import whiteboard.core.CloseableThread;
import whiteboard.core.entities.BytePacker;
import whiteboard.core.transaction.NetworkTransactionManager;
import whiteboard.networking.Peer;
import whiteboard.networking.WhiteboardPeer;
import whiteboard.networking.mars.MarsProtocol;


/**
 * @author patrick
 */
public class VenusServer extends CloseableThread {
	/** list of venus receive threads running in the server */
	private List<CloseableThread> threads = new ArrayList<CloseableThread>();
	/** the socket that's connected to the head of the whiteboard */
	private ServerSocket serverSocket;
	/** the protocol containing the list of peers */
	private VenusProtocol venusProtocol;
	/** the peer for this user */
	private WhiteboardPeer userPeer;
	/** the transaction manager responsible for sequencing */
	private NetworkTransactionManager man;

	/** is the thread still running */
	private boolean isStopped = false;

	public VenusServer(WhiteboardPeer user, VenusProtocol venusProtocol, NetworkTransactionManager man) throws IOException {
		this(user, VenusProtocol.PORT, venusProtocol, man);
	}

	public VenusServer(WhiteboardPeer user, int port, VenusProtocol venusProtocol, NetworkTransactionManager man) throws IOException {
		super("VenusServer");

		this.userPeer = user;
		serverSocket = new ServerSocket(port);
		this.venusProtocol = venusProtocol;
		this.man = man;
	}

	/** Check if there's a password */
	public boolean checkPassword(final String message[]) {
		final char[] boardPassword = venusProtocol.window.getCore().getPassword();
		if (boardPassword == null  || boardPassword.length == 0)
			return true;

		if ( message.length < 5 || "".equals(message[3]))
			return false;
		
		if (null != boardPassword) {
			final char[] peerPassword = message[4].toCharArray();
			
			// Check password
			return Arrays.equals(peerPassword, boardPassword);
		}
		return true;
	}

	/**
	 * Sends the list of known peers to a connecting peer
	 * 
	 * @param connectingPeer the peer to reply to
	 * @throws IOException
	 */
	public void replyPeers(Peer connectingPeer) throws IOException {
		// Create body
		StringBuffer bodyBuffer = new StringBuffer();

		// Loop through whiteboards
		List<WhiteboardPeer> peers = venusProtocol.getPeers();
		int size = peers.size();

		for (WhiteboardPeer peer : peers) {
			// Remove this peer from the list
			if (!peer.getName().equalsIgnoreCase(venusProtocol.getName())) {

				bodyBuffer.append(peer.getName() + VenusProtocol.LINE_END);
				bodyBuffer.append(peer.getIP() + VenusProtocol.LINE_END);
				bodyBuffer.append(peer.getPermissionLevel().ordinal() + VenusProtocol.LINE_END);

				// Only add the port if it's not the default
				if (VenusProtocol.PORT != peer.getPort()) {
					bodyBuffer.append(peer.getPort() + VenusProtocol.LINE_END);
				}

				bodyBuffer.append(MarsProtocol.LINE_END);
			}
		}

		// Create header
		StringBuffer headerBuffer = new StringBuffer();
		headerBuffer.append("PEERS" + VenusProtocol.LINE_END);
		headerBuffer.append((size - 1) + VenusProtocol.LINE_END);
		headerBuffer.append(bodyBuffer.toString().length() + VenusProtocol.LINE_END);

		// Indiciate end of header
		headerBuffer.append(VenusProtocol.LINE_END);

		// Send header
		byte[] packet = headerBuffer.toString().getBytes();
		connectingPeer.send(packet);

		// Send body
		packet = bodyBuffer.toString().getBytes();
		connectingPeer.send(packet);
	}

	public void replyHistory(Peer peer) throws IOException {
		// Get history
		List<byte[]> shapes = man.getHistory();

		// Send header
		byte[] packet = new byte[VenusProtocol.SHAPES_HEADER.length() + 4];

		// copy over the header-string
		for (int i = 0; i < VenusProtocol.SHAPES_HEADER.length(); ++i) {
			packet[i] = VenusProtocol.SHAPES_HEADER.getBytes()[i];
		}

		List<Byte> numAsBytes = BytePacker.convertIntToBytes(shapes.size());
		BytePacker.insertInto_byte_Array(numAsBytes, packet, VenusProtocol.SHAPES_HEADER.getBytes().length);
		peer.send(packet);

		// Send history
		for (byte[] shape : shapes) {
			peer.send(VenusProtocol.appendVenusPacketHeader(shape));
		}
	}

	@Override
	public void run() {

		// Loop until socket is closed
		while ((null != serverSocket) && (!serverSocket.isClosed()) && (!isStopped)) {
			try {
				// Accept connection
				Socket socket = serverSocket.accept();

				// Create peer
				WhiteboardPeer peer = new WhiteboardPeer(socket);

				if (replyHandshake(peer)) {
					// If peer is already in list, remove them
					venusProtocol.removePeer(peer);

					if (userPeer.getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.OWNER)) {
						// Wait for peer request
						byte[] packet = VenusFactory.createPacket();

						int bytesReceived = peer.receive(packet);

						String[] message = VenusFactory.getMessage(packet, bytesReceived);
						processPeerRequest(peer, message);

						// Wait for whiteboard shape request
						bytesReceived = peer.receive(packet);

						message = VenusFactory.getMessage(packet, bytesReceived);
						processShapeRequest(peer, message);
					}

					// Add peer to list
					if (venusProtocol.addPeer(peer)) {
						// Start a listening thread for this peer
						VenusReceiveThread thread = new VenusReceiveThread(peer, venusProtocol, man);
						thread.start();
						threads.add(thread);
					}
				}
				else {
					socket.close();
				}
			} catch (IOException ioe) {
				if (!ioe.getMessage().equals("socket closed"))
					System.err.println("VenusServer: run: " + ioe.getMessage());
			}
		}
	}

	/**
	 * @param peer
	 * @param message
	 * @throws IOException
	 */
	private void processPeerRequest(WhiteboardPeer peer, String[] message) throws IOException {
		// Check if it's a valid response
		if (message[0].equals("GET PEERS")) {
			replyPeers(peer);
		}
	}

	/**
	 * @param connectedPeer
	 * @param message
	 */
	private void processShapeRequest(WhiteboardPeer peer, String[] message) throws IOException {
		// Check if it's a valid response
		if (message[0].equals("GET WHITEBOARD SHAPES")) {
			replyHistory(peer);
		}
	}

	/**
	 * @param peer
	 */
	private boolean replyHandshake(WhiteboardPeer peer) throws IOException {
		// Get handshake greeting
		byte[] packet = VenusFactory.createPacket();
		final int bytesReceived = peer.receive(packet);

		final String[] message = VenusFactory.getMessage(packet, bytesReceived);

		// Check message
		int i = 0;
		if (message[i++].equals("HELLO " + VenusProtocol.PROTOCOL)) {
			// Check password
			if (!checkPassword(message)) {
				final StringBuffer messageBuffer = new StringBuffer();
				messageBuffer.append(VenusProtocol.PROTOCOL + " FAILED" + VenusProtocol.LINE_END);
				messageBuffer.append(VenusProtocol.LINE_END);
				
				// Get message as bytes
				packet = messageBuffer.toString().getBytes();
				
				// Send message to peer
				peer.send(packet);

				return false;
			}

			// Get the peer name
			final String peerName = message[i++];
			peer.setName(peerName);

			// Get the peer port
			final int peerPort = Integer.parseInt(message[i++]);
			peer.setPort(peerPort);

			// Get the peer permission level
			final WhiteboardPeer.PERM_LEVEL permLevel = WhiteboardPeer.PERM_LEVEL.values()[Integer.parseInt(message[i++])];

			// Create message
			final Peer head = venusProtocol.getWhiteboardConfig().getHead();

			final StringBuffer messageBuffer = new StringBuffer();
			messageBuffer.append(VenusProtocol.PROTOCOL + " OK" + VenusProtocol.LINE_END);
			messageBuffer.append(head.getName() + VenusProtocol.LINE_END);
			messageBuffer.append(head.getAddress().getHostAddress() + VenusProtocol.LINE_END);
			messageBuffer.append(head.getPort() + VenusProtocol.LINE_END);
			messageBuffer.append(venusProtocol.getWhiteboardConfig().getDefaultJoinUserPermLevel().ordinal() + VenusProtocol.LINE_END);
			if(userPeer.getName().equals(venusProtocol.getWhiteboardConfig().getHead().getName())) {
				peer.setPermissionLevel(venusProtocol.getWhiteboardConfig().getDefaultJoinUserPermLevel());
			} else {
				peer.setPermissionLevel(permLevel);
			}

			// Indicate end of message
			messageBuffer.append(VenusProtocol.LINE_END);

			// Get message as bytes
			packet = messageBuffer.toString().getBytes();

			// Send message to peer
			peer.send(packet);
		} else {
			return false;
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.venus.VenusThread#close()
	 */
	@Override
	public void close() {
			// Stop all child threads
			for (CloseableThread venusThread : threads) {
				venusThread.close();
			}
			try {
			// Try to do tidy shutdown
			isStopped = true;
			serverSocket.close();
		} catch (IOException ioe) {
			// Untidy shutdown
			serverSocket = null;
		}
	}
}
