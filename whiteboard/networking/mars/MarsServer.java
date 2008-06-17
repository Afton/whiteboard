/**
 * 
 */
package whiteboard.networking.mars;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import whiteboard.core.WhiteboardCore;
import whiteboard.gui.startlist.ConnectWindow;
import whiteboard.networking.NetworkingUtils;
import whiteboard.networking.Peer;
import whiteboard.networking.StreamPeer;

/**
 * @author patrick
 */
public class MarsServer extends Thread {
	private static int port;
	private ServerSocket serverSocket;
	private MarsDatagramThread datagramThread;
	private String name;
	private ConnectWindow window;

	public MarsServer(ConnectWindow window, final String name) throws IOException {
		this(window, name, MarsProtocol.PORT);
	}

	public MarsServer(ConnectWindow window, final String name, final int port) throws IOException {
		super("MarsServer");
		
		this.window = window;
		this.name = name;
		MarsServer.port = port;
		serverSocket = new ServerSocket(port);
		datagramThread = new MarsDatagramThread(name, port);
	}

	/**
	 * @return the port
	 */
	public static int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public static void setPort(final int port) {
		MarsServer.port = port;
	}

	public void close() throws IOException {
		if ((null != serverSocket) && (!serverSocket.isClosed())) {
			serverSocket.close();
		}
		
		datagramThread.close();
	}
	
	public String getPeerName() {
		return name;
	}

	public void processMessage(final Peer peer, final String[] message) throws IOException {
		// Check if it's a valid response
		if (message[0].equals("GET PEERS")) {
			replyPeers(peer);
		} else if (message[0].equals("GET WHITEBOARDS")) {
			replyWhiteboards(peer);
		}
	}

	public boolean removePeer(final String peerName) {
		// Create message
		final StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append("REMOVE" + MarsProtocol.LINE_END);
		messageBuffer.append(peerName + MarsProtocol.LINE_END);

		// Indiciate end of message
		messageBuffer.append(MarsProtocol.LINE_END);

		// Get message as bytes
		final byte[] packet = messageBuffer.toString().getBytes();

		// Send message
		MarsProtocol.send(packet);

		// Remove peer from list
		return MarsProtocol.removePeer(peerName);
	}

	public Peer replyHandshake(final Peer peer) throws IOException {
		// TODO: password validation
		Peer returnPeer = null;

		// Get handshake greeting
		byte[] packet = MarsProtocol.createPacket();
		final int bytesReceived = peer.receive(packet);

		final String[] message = NetworkingUtils.getMessage(packet, bytesReceived);

		// Check message
		if (message[0].equals("HELLO " + MarsProtocol.PROTOCOL)) {
			final String peerName = message[1];
			final int peerPort = Integer.parseInt(message[2]);
			final byte[] peerAddr = peer.getAddress().getAddress();

			// Create message
			final StringBuffer messageBuffer = new StringBuffer();
			messageBuffer.append(MarsProtocol.PROTOCOL + " OK" + MarsProtocol.LINE_END);
			messageBuffer.append(name + MarsProtocol.LINE_END);

			// Indiciate end of message
			messageBuffer.append(MarsProtocol.LINE_END);

			// Get message as bytes
			packet = messageBuffer.toString().getBytes();

			// Send message to peer
			peer.send(packet);

			returnPeer = MarsProtocol.createPeer(peerName, peerAddr, peerPort);
		} else {
			System.err.println("MARSSRVR: Received an invalid handshake.");
		}

		return returnPeer;
	}

	public void replyPeers(final Peer peer) throws IOException {
		// Create body
		final StringBuffer bodyBuffer = new StringBuffer();

		// Loop through whiteboards
		for (final Peer p : MarsProtocol.getPeers()) {
			bodyBuffer.append(p.getName() + MarsProtocol.LINE_END);
			bodyBuffer.append(p.getIP() + MarsProtocol.LINE_END);
			bodyBuffer.append(p.getPort() + MarsProtocol.LINE_END);

			bodyBuffer.append(MarsProtocol.LINE_END);
		}

		// Create header
		final StringBuffer headerBuffer = new StringBuffer();
		headerBuffer.append("PEERS" + MarsProtocol.LINE_END);
		headerBuffer.append(MarsProtocol.getPeers().size() + MarsProtocol.LINE_END);
		headerBuffer.append(bodyBuffer.toString().length() + MarsProtocol.LINE_END);

		// Indiciate end of header
		headerBuffer.append(MarsProtocol.LINE_END);

		// Send header
		byte[] packet = headerBuffer.toString().getBytes();
		peer.send(packet);

		// Send body
		packet = bodyBuffer.toString().getBytes();
		peer.send(packet);
	}

	public void replyWhiteboards(final Peer peer) throws IOException {
		// Create body
		final StringBuffer bodyBuffer = new StringBuffer();

		// Loop through whiteboards
		for (final WhiteboardCore whiteboard : MarsProtocol.getWhiteboards()) {
			bodyBuffer.append(whiteboard.getName() + MarsProtocol.LINE_END);
			bodyBuffer.append(whiteboard.getPermissionLevel().ordinal() + MarsProtocol.LINE_END);
			bodyBuffer.append(whiteboard.getHead().getName() + MarsProtocol.LINE_END);
			bodyBuffer.append(whiteboard.getHead().getPort() + MarsProtocol.LINE_END);

			bodyBuffer.append(MarsProtocol.LINE_END);
		}

		// Create header
		final StringBuffer headerBuffer = new StringBuffer();
		headerBuffer.append("WHITEBOARDS" + MarsProtocol.LINE_END);
		headerBuffer.append(MarsProtocol.getWhiteboards().size() + MarsProtocol.LINE_END);
		headerBuffer.append(bodyBuffer.toString().length() + MarsProtocol.LINE_END);

		// Indiciate end of header
		headerBuffer.append(MarsProtocol.LINE_END);

		// Send header
		byte[] packet = headerBuffer.toString().getBytes();
		peer.send(packet);

		// Send body
		packet = bodyBuffer.toString().getBytes();
		peer.send(packet);
	}

	@Override
	public void run() {
		datagramThread.start();

		while (!serverSocket.isClosed()) {
			try {
				// Accept connection
				final Socket socket = serverSocket.accept();

				// Create peer
				final StreamPeer streamPeer = new StreamPeer(socket);

				// Wait for handshake
				final Peer peer = replyHandshake(streamPeer);

				if (null == peer) {
					streamPeer.close();
					continue;
				}
				
				// If peer is already in list, remove them
				MarsProtocol.removePeer(peer);

				// Wait for request for information
				final byte[] packet = MarsProtocol.createPacket();

				int bytesReceived = streamPeer.receive(packet);

				while (0 < bytesReceived) {
					final String[] message = NetworkingUtils.getMessage(packet, bytesReceived);
					processMessage(streamPeer, message);

					bytesReceived = streamPeer.receive(packet);
				}

				streamPeer.close();

				// Add peer to list
				MarsProtocol.addPeer(peer);
				MarsProtocol.sendPeer(peer);
				window.serverSetConnected();
			} catch (final IOException ioe) {
				if(!ioe.getMessage().equals("socket closed"))
					System.err.println("MARSSRVR: " + ioe.getMessage());
			}
		}
	}
}
