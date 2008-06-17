/**
 * 
 */
package whiteboard.networking.eris;

import java.io.IOException;

import whiteboard.networking.NetworkingUtils;
import whiteboard.networking.Peer;
import whiteboard.networking.StreamPeer;

/**
 * @author patrick
 *
 */
public class ErisClient extends SequenceServer {
	private Peer server;
	
	public ErisClient(Peer server) {
		super("ErisClient");
		
		this.server = server;
	}
	
	/**
	 * Returns the next sequence number
	 * 
	 * @return the next sequence number, or -1 on failure
	 */
	@Override
	public int getSequenceNum() {
		return getSequence("GET");
	}

	/**
	 * Returns the last sequence number
	 * 
	 * @return the last sequence number, or -1 on failure
	 */
	@Override
	public int peekSequenceNum() {
		return getSequence("PEEK");
	}

	private int getSequence(String type) {
		int sequenceNum = -1;
		
		// Create request message
		StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append(type + " SEQUENCE" + ErisProtocol.LINE_END);
		messageBuffer.append(ErisProtocol.LINE_END);
		
		// Create request packet
		byte[] packet = messageBuffer.toString().getBytes();
		
		try {
			// Send request packet
			ErisProtocol.wrapAndSend(server, packet);
			
			// Get reply
			byte[] reply = ErisProtocol.receivePacket( (StreamPeer)server);
			
			// Process reply
			String[] message = NetworkingUtils.getMessage(reply, reply.length);
			
			if ((2 >= message.length) && message[0].equals("SEQUENCE")) {
				sequenceNum = Integer.parseInt(message[1]);
			}
		} catch (IOException ioe) {
			return sequenceNum;
		}

		return sequenceNum;
	}
	
	private boolean initiateHandshake() throws IOException {
		boolean result = false;
		
		// Create message
		final StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append("HELLO " + ErisProtocol.PROTOCOL + ErisProtocol.LINE_END);
		messageBuffer.append(ErisProtocol.LINE_END);

		// Get message as bytes
		byte[] packet = messageBuffer.toString().getBytes();

		// Send message to server
		if(server == null)
			throw new IOException("null server on send");
		server.send(packet);

		// Get handshake reply
		
		packet = ErisFactory.createPacket();
		final int packetLength = server.receive(packet);

		final String[] message = NetworkingUtils.getMessage(packet, packetLength);

		// Check message
		if ((2 <=message.length) && message[0].equals(ErisProtocol.PROTOCOL + " OK")) {
			setEpochNum(Integer.parseInt(message[1]));
			result = true;
		}

		return result;
	}

	public boolean connect() throws IOException {
		boolean connected = false;

		if (!server.isConnected()) {
			// Connect to peer
			server.connect();

			// Send handshake
			connected = initiateHandshake();
		}

		return connected;
	}

	@Override
	public void run() {
		try {
			// Connect
			connect(); // throw away return value. 
			
		} catch (final IOException ioe) {
			//swallow exception
		}
	}

	/* (non-Javadoc)
	 * @see whiteboard.networking.venus.VenusThread#close()
	 */
	@Override
	public void close() {
		// Close connection to peer
		try {
			if (server != null)
				server.close();
		} catch (IOException ioe) {
			// Do nothing
		}

		server = null;
	}
}
