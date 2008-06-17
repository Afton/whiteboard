/**
 * 
 */
package whiteboard.networking.eris;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import whiteboard.core.CloseableThread;
import whiteboard.networking.NetworkingUtils;
import whiteboard.networking.Peer;
import whiteboard.networking.StreamPeer;

/**
 * @author patrick
 */
public class ErisServer extends SequenceServer {
	public class ErisListenThread extends CloseableThread {
		private Peer peer;
		private boolean listenDone = false;
		
		public ErisListenThread(Peer peer) {
			super("ErisListenThread");
			
			this.peer = peer;
		}
		
		@Override
		public void run() {
			while (!listenDone && (null != peer) && peer.isConnected() && !peer.isClosed()) {
				try {
					byte[] packet = ErisProtocol.receivePacket((StreamPeer) peer);
					
					String[] message = NetworkingUtils.getMessage(packet, packet.length);
					int num = -1;
					if ((1 <= message.length) && (message[0].equals("GET SEQUENCE"))) {
						// Get the next sequence number
						num = getSequenceNum();
					} else if ((1 <= message.length) && (message[0].equals("PEEK SEQUENCE"))) {
						// Get the next sequence number
						num = peekSequenceNum();
					}
					if(num >= 0) {
						// Create reply message
						StringBuffer messageBuffer = new StringBuffer();
						messageBuffer.append("SEQUENCE" + ErisProtocol.LINE_END);
						messageBuffer.append(num + ErisProtocol.LINE_END);
						messageBuffer.append(ErisProtocol.LINE_END);
						
						// Send reply to peer
						ErisProtocol.wrapAndSend(peer, messageBuffer.toString().getBytes());
					}
				} catch (SocketException se) {
					listenDone = true;
				} catch (IOException ioe) {
					listenDone = true;
				}
			}
		}

		/* (non-Javadoc)
		 * @see whiteboard.networking.venus.VenusThread#close()
		 */
		@Override
		public void close() {
			try {
				// Attempt tidy shutdown
				peer.close();
			} catch (IOException ioe) {
				// Untidy shutdown
				peer = null;
			}

			listenDone = true;
		}

	}
	
	
	private ServerSocket serverSocket; 
	private boolean done = false;
	private int sequenceNum = 0;
	private List<CloseableThread> threads = new ArrayList<CloseableThread>();

	public ErisServer(int epochNum) {
		super("ErisServer");

		setEpochNum(epochNum);
	}

	public ErisServer() {
		this(0);
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(ErisProtocol.PORT);

			while (!done && serverSocket.isBound() && !serverSocket.isClosed()) {
				// Wait for connections
				Socket socket = serverSocket.accept();

				// Create peer
				Peer peer = new StreamPeer(socket);

				// Get handshake
				if (replyHandshake(peer)) {
					// Start listening thread
					CloseableThread thread = new ErisListenThread(peer);
					thread.start();
					threads.add(thread);
				}
			}
		} catch (IOException ioe) {
			return; // wait for externals to shut us down.
		}
	}

	public boolean replyHandshake(Peer peer) throws IOException {
		if (peer == null)
			throw new IOException();

		boolean result = false;

		// Get handshake greeting
		byte[] packet = ErisFactory.createPacket();
		int packetLength = peer.receive(packet);

		String[] message = NetworkingUtils.getMessage(packet, packetLength);

		// Check message
		if ((message.length) > 0 && message[0].equals("HELLO " + ErisProtocol.PROTOCOL)) {
			// Create message
			StringBuffer messageBuffer = new StringBuffer();
			messageBuffer.append(ErisProtocol.PROTOCOL + " OK" + ErisProtocol.LINE_END);
			messageBuffer.append(getEpochNum() + ErisProtocol.LINE_END);

			// Indiciate end of message
			messageBuffer.append(ErisProtocol.LINE_END);

			// Get message as bytes
			packet = messageBuffer.toString().getBytes();

			// Send message to peer
			peer.send(packet);

			result = true;
		}

		return result;
	}

	@Override
	public synchronized int getSequenceNum() {
		return sequenceNum++;
	}

	@Override
	public synchronized int peekSequenceNum() {
		return sequenceNum;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.venus.VenusThread#close()
	 */
	@Override
	public void close() {
		try {
			if(serverSocket != null) {
				// Close the server socket
				serverSocket.close();
			}
		} catch (IOException ioe) {
			// Untidy shutdown
			serverSocket = null;
		}
		
		// Stop all child threads
		for (CloseableThread thread : threads) {
			thread.close();
		}

		done = true;
	}
}