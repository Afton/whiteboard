/**
 * 
 */
package whiteboard.networking;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;


/**
 * This class is for connecting to peers using datagram packets.
 * 
 * @author patrick
 */
public class DatagramPeer extends Peer {
	private DatagramSocket socket;

	/**
	 * @param name The name of the peer
	 * @param addr The IP address of the peer
	 * @param port The port to connect to on peer
	 * @throws UnknownHostException
	 */
	public DatagramPeer(String name, byte[] addr, int port) throws UnknownHostException, SocketException {
		super(name, addr, port);
		init();
	}

	/**
	 * @param host The host name or IP string of peer
	 * @param port The port to connect to on peer
	 */
	public DatagramPeer(String host, int port) throws UnknownHostException, SocketException {
		super(host, port);
		init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#close()
	 */
	@Override
	public void close() {
		socket.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#connect()
	 */
	@Override
	public void connect() {
		socket.connect(addr, port);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#isClosed()
	 */
	@Override
	public boolean isClosed() {
		return socket.isClosed();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return socket.isConnected();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#receive()
	 */
	@Override
	public int receive(byte[] data) throws IOException {
		DatagramPacket packet = new DatagramPacket(data, data.length);
		socket.receive(packet);

		return packet.getLength();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#send(byte[])
	 */
	@Override
	public void send(byte[] buf) throws IOException {
		DatagramPacket packet = new DatagramPacket(buf, buf.length, addr, port);
		socket.send(packet);
	}

	/**
	 * Initialise the peer
	 */
	private void init() throws SocketException {
		socket = new DatagramSocket(null);
	}

}
