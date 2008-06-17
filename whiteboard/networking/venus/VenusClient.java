/**
 * 
 */
package whiteboard.networking.venus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import whiteboard.core.CloseableThread;
import whiteboard.core.entities.BytePacker;
import whiteboard.core.entities.ShapeConstants;
import whiteboard.core.transaction.NetworkTransactionManager;
import whiteboard.gui.whiteboard.Canvas;
import whiteboard.networking.NetworkingUtils;
import whiteboard.networking.WhiteboardPeer;
import whiteboard.networking.mars.MarsProtocol;

/**
 * @author patrick
 */
public class VenusClient extends CloseableThread {
	public static enum CONNECT_TYPE { GOOD, BAD, NOT_HEAD, BAD_PASSWORD, KICKED}

	private final VenusProtocol venusProtocol;
	private final NetworkTransactionManager man;
	private final Canvas canvas;

	private final WhiteboardPeer head;
	private final int port;

	private String password = null;

	private List<CloseableThread> threads = new ArrayList<CloseableThread>();

	private final boolean isHead;

	public VenusClient(final WhiteboardPeer head, final int port, final boolean isHead, VenusProtocol venusProtocol, NetworkTransactionManager man, Canvas canvas) {
		super("VenusClient");

		this.man = man;
		this.head = head;
		this.port = port;
		this.venusProtocol = venusProtocol;
		this.isHead = isHead;
		this.canvas = canvas;
	}

	public VenusClient(final WhiteboardPeer head, final int port, final boolean isHead, VenusProtocol venusProtocol, NetworkTransactionManager man, Canvas canvas, String password) {
		this(head, port, isHead, venusProtocol, man, canvas);
		this.password = password;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.venus.VenusThread#close()
	 */
	@Override
	public void close() {
		for (CloseableThread thread : threads) {
			thread.close();
		}
		try {
			head.close();
		} catch(IOException e) {
			System.err.println(e.getLocalizedMessage());
		}
	}

	public CONNECT_TYPE connect() throws IOException {
		CONNECT_TYPE connected = CONNECT_TYPE.BAD;

		// Connect to peer
		head.connect();

		// Send handshake
		connected = initiateHandshake();

		return connected;
	}

	public void getPeers() throws IOException {
		// If peer is null, just return
		if (null == head) {
			return;
		}

		// Check if peer is closed, if so, open connection
		if (head.isClosed()) {
			// Open connection
			connect();
		}

		// Create message
		final StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append("GET PEERS" + VenusProtocol.LINE_END);

		// Indiciate end of message
		messageBuffer.append(VenusProtocol.LINE_END);

		// Get message as bytes
		byte[] packet = messageBuffer.toString().getBytes();

		// Send message to peer
		head.send(packet);

		// Get response header
		packet = VenusFactory.createPacket();
		int bytesReceived = head.receive(packet);

		String[] message = VenusFactory.getMessage(packet, bytesReceived);

		// Check message
		if (message[0].equals("PEERS")) {
			final int numPeers = Integer.parseInt(message[1]);
			final int length = Integer.parseInt(message[2]);

			// Get response body
			packet = VenusFactory.createPacket(length);
			bytesReceived = head.receive(packet);
			message = VenusFactory.getMessage(packet, bytesReceived);

			// Set the peers
			extractPeers(message, numPeers);

			// Add connected peer
			venusProtocol.addPeer(head);
			threads.add(new VenusReceiveThread(head, venusProtocol, man));
		} 
	}

	/**
	 * 
	 */
	public boolean getShapes() throws IOException {
		// If peer is null, just return
		if (null == head) {
			return false;
		}

		// Check if peer is closed, if so, open connection
		if (head.isClosed()) {
			// Open connection
			connect();
		}

		// Create message
		String message = "GET WHITEBOARD SHAPES" + VenusProtocol.LINE_END;

		// Get message as bytes
		byte[] packet = message.toString().getBytes();

		// Send message to peer
		head.send(packet);

		byte[] ShapesHeader = new byte[VenusProtocol.SHAPES_HEADER.getBytes().length + ShapeConstants.INTEGER_BYTE_SIZE];
		if (false == head.receive(ShapesHeader, VenusProtocol.SHAPES_HEADER.getBytes().length + ShapeConstants.INTEGER_BYTE_SIZE)) {
			return false;
		}

		int numShapes = BytePacker.convertBytesToInt(ShapesHeader, ShapesHeader.length - 4);

		// Get shapes
		for (int i = 0; i < numShapes; ++i) {
			byte[] shape = VenusProtocol.receivePacket(head);
			man.pushToLocalShape(shape);
		}

		return true;
	}

	public CONNECT_TYPE initiateHandshake() throws IOException {
		// Create message
		final StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append("HELLO " + VenusProtocol.PROTOCOL + VenusProtocol.LINE_END);

		messageBuffer.append(venusProtocol.getName() + VenusProtocol.LINE_END);
		messageBuffer.append(port + VenusProtocol.LINE_END);
		messageBuffer.append(venusProtocol.getWhiteboardConfig().getUserPeer().getPermissionLevel().ordinal() + VenusProtocol.LINE_END);

		// Insert password if needed
		if (null != password) {
			messageBuffer.append(password + VenusProtocol.LINE_END);
		}

		// Indiciate end of message
		messageBuffer.append(VenusProtocol.LINE_END);

		// Get message as bytes
		byte[] packet = messageBuffer.toString().getBytes();

		// Send message to peer
		head.send(packet);

		// Get handshake reply
		packet = VenusFactory.createPacket();
		final int bytesReceived = head.receive(packet);

		final String[] message = NetworkingUtils.getMessage(packet, bytesReceived);

		// Check message
		if ((5 <= message.length) && message[0].equals(VenusProtocol.PROTOCOL + " OK")) {
			final String headName = message[1];
			final byte[] headAddr = NetworkingUtils.convertIPtoArray(message[2]);
			final int headPort = Integer.parseInt(message[3]);
			final int permissionLevel = Integer.parseInt(message[4]);
			
			// Check that this is actually the head
			if (head.getName().equals(headName)) {
				head.setName(headName);
				head.setPermissionLevel(WhiteboardPeer.PERM_LEVEL.OWNER);
				venusProtocol.getWhiteboardConfig().getUserPeer().setPermissionLevel(WhiteboardPeer.PERM_LEVEL.values()[permissionLevel]);
				venusProtocol.setConnectedToHead(true);
				return CONNECT_TYPE.GOOD;
			}

			// If it's not the head, then connect to the head if we haven't already
			if (!venusProtocol.getConnectedToHead()) {
				VenusClient vc = new VenusClient(new WhiteboardPeer(headName, headAddr, headPort), headPort, true, venusProtocol, man, canvas);
				vc.start();
				return CONNECT_TYPE.NOT_HEAD;
			}

			return CONNECT_TYPE.GOOD;
		} else if ((1 <= message.length) && message[0].equals(VenusProtocol.PROTOCOL + " FAILED")) {
			return CONNECT_TYPE.BAD_PASSWORD;
		}

		return CONNECT_TYPE.BAD;
	}

	@Override
	public void run() {
		try {
			// Connect
			CONNECT_TYPE connected = connect();
			
			switch (connected) {
			case GOOD:
				if (isHead) {
					// Get peers
					getPeers();
	
					// Get whiteboard shapes
					if (false == getShapes()) {
						return;
					}
	
					// Start listening threads for each peer
					for (Thread thread : threads) {
						thread.start();
					}
				} else {
					venusProtocol.addPeer(head);
					CloseableThread thread = new VenusReceiveThread(head, venusProtocol, man);
					thread.start();
					threads.add(thread);
				}

				canvas.getWindow().setVisible(true);
				
				break;
			case NOT_HEAD:
				break;
			case BAD:
			case BAD_PASSWORD:
			default:
				canvas.getWindow().safeExit(connected, false);
				close();
				break;
			}
		} catch (final IOException ioe) {
			System.err.println(ioe.getMessage());
			
			// Connect failed, so kill whiteboard and remove it from the list
			canvas.getWindow().safeExit(CONNECT_TYPE.BAD, false);
			MarsProtocol.removeWhiteboard(canvas.getWindow().getCore());
		}
	}

	private List<WhiteboardPeer> extractPeers(final String[] message, final int numPeers) {
		final List<WhiteboardPeer> peers = new ArrayList<WhiteboardPeer>(numPeers);

		// Loop through message getting whiteboards
		if (0 < numPeers) {
			for (int i = 0; i < message.length; i++) {
				// Get the peer name
				final String peerName = message[i++];

				// Get the peer's ip
				final byte[] peerAddr = NetworkingUtils.convertIPtoArray(message[i++]);

				// Get the peer's permission level
				WhiteboardPeer.PERM_LEVEL permissionLevel = WhiteboardPeer.PERM_LEVEL.values()[Integer.parseInt(message[i++])];

				// Get the port
				int peerPort = VenusProtocol.PORT;

				// Check if there's a port line
				if (3 < message.length) {
					peerPort = Integer.parseInt(message[i++]);
				}

				// Add the peer to the list
				try {
					// Add peer to list
					final WhiteboardPeer peer = VenusFactory.createPeer(peerName, peerAddr, peerPort);
					peer.setPermissionLevel(permissionLevel);
					peers.add(peer);

					// Create connection thread
					CloseableThread thread = new VenusClient(peer, peerPort, false, venusProtocol, man, canvas);
					threads.add(thread);
				} catch (final IOException ioe) {
					System.err.println(peerName + ": " + ioe.getMessage());
				}
			}
		}

		return peers;
	}

}
