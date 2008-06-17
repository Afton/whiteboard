/**
 * 
 */
package whiteboard.networking;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * This class is for connecting to peers using a stream.
 * 
 * @author patrick
 */
public class StreamPeer extends Peer {
	protected Socket socket;

	/**
	 * @param name The name of the peer
	 * @param addr The IP address of the peer
	 * @param port The port to connect to on the peer
	 * @throws UnknownHostException
	 */
	public StreamPeer(String name, byte[] addr, int port) throws UnknownHostException, IOException {
		super(name, addr, port);
		init();
	}

	/**
	 * @param host The host name or IP string of the peer
	 * @param port The port to connect to on the peer
	 * @throws UnknownHostException
	 */
	public StreamPeer(String host, int port) throws UnknownHostException, IOException {
		super(host, port);
		init();
	}

	/**
	 * @param name The name of the peer
	 * @param host The host name or IP string of the peer
	 * @param port The port to connect to on the peer
	 * @throws UnknownHostException
	 */
	public StreamPeer(String name, String host, int port) throws UnknownHostException, IOException {
		super(name, host, port);
		init();
	}

	/**
	 * @param inetAddress
	 * @param port
	 */
	public StreamPeer(InetAddress addr, int port) {
		super(addr, port);
		init();
	}

	/**
	 * @param inetAddress
	 * @param port
	 */
	public StreamPeer(String name, InetAddress addr, int port) {
		super(name, addr, port);
		init();
	}

	/**
	 * @param socket
	 */
	public StreamPeer(Socket socket) {
		super(socket.getInetAddress(), socket.getPort());

		this.socket = socket;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#close()
	 */
	@Override
	public void close() throws IOException {
		if (!socket.isClosed()) {
			socket.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#connect()
	 */
	@Override
	public void connect() throws IOException {
		if (socket.isClosed() || !socket.isConnected()) {
			socket = new Socket(addr, port);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#isClosed()
	 */
	@Override
	public boolean isClosed() {
		return (null == socket) ? true : socket.isClosed();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return (null == socket) ? false : socket.isConnected();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#receive()
	 */
	@Override
	public int receive(byte[] data) throws IOException {
		if (!isConnected()) {
			connect();
		}

		if(!socket.isClosed())
			return socket.getInputStream().read(data);
		return -1;
	}
	
	/***
	 * 
	 * @param buffer the buffer to read data into. 
	 * @param bytesToRead The number of bytes that will be read. Actually it's MIN(buffer.length, bytesToRead)
	 * @return true iff we've read exactly bytesToRead bytes. false otherwise.
	 * 
	 *  This method blocks until either an IOException is thrown, or the number of bytes is read.
	 */
//	@Override
	public boolean receive(byte[] buffer, int bytesToRead) throws IOException
	{
		if (!isConnected()) {
			connect();
		}
		// sanity check
		if (buffer.length < bytesToRead) {
			bytesToRead = buffer.length;
		}
						
		// loop until we've read enough data
		while ((bytesToRead > 0) && (!(socket.isClosed() && socket.isConnected())))
		{
			int recvd = socket.getInputStream().read(buffer, buffer.length - bytesToRead, bytesToRead);
			
			// if the pipe breaks, return how much we did read
			if (-1 == recvd) {
				throw new IOException("Socket closed for peer: " + this);
			}
			
			// update how much we have left to read
			bytesToRead -= recvd;								
		}
		
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.Peer#send(byte[])
	 */
	@Override
	public void send(byte[] buf) throws IOException {
		if (!isConnected()) {
			connect();
		}
		
		socket.getOutputStream().write(buf);
	}

	/**
	 * Initialise the peer
	 */
	private void init() {
		socket = new Socket();
	}
}
