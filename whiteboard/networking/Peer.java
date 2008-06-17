/**
 * 
 */
package whiteboard.networking;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * Abstract class for peers
 * 
 * @author patrick
 */
public abstract class Peer {
	protected String name;
	protected InetAddress addr;
	protected int port;

	/**
	 * @param addr
	 * @param port
	 * @throws UnknownHostException
	 */
	public Peer(byte[] addr, int port) throws UnknownHostException {
		this(null, addr, port);
	}

	/**
	 * @param addr
	 * @param port
	 * @throws UnknownHostException
	 */
	public Peer(InetAddress addr, int port) {
		this(null, addr, port);
	}

	/**
	 * @param name
	 * @param addr
	 * @param port
	 * @throws UnknownHostException
	 */
	public Peer(String name, byte[] addr, int port) throws UnknownHostException {
		this.name = name;
		this.addr = InetAddress.getByAddress(addr);
		this.port = port;
	}

	/**
	 * @param name
	 * @param addr
	 * @param port
	 * @throws UnknownHostException
	 */
	public Peer(String name, InetAddress addr, int port) {
		this.name = name;
		this.addr = addr;
		this.port = port;
	}

	/**
	 * @param host
	 * @param port
	 * @throws UnknownHostException
	 */
	public Peer(String host, int port) throws UnknownHostException {
		this(null, host, port);
	}

	/**
	 * @param name
	 * @param host
	 * @param port
	 * @throws UnknownHostException
	 */
	public Peer(String name, String host, int port) throws UnknownHostException {
		this.name = name;
		addr = InetAddress.getByName(host);
		this.port = port;
	}

	/**
	 * @throws IOException
	 */
	public abstract void close() throws IOException;

	/**
	 * @throws IOException
	 */
	public abstract void connect() throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (null != o) {
			if (o instanceof String) {
				return this.equals((String) o);
			} else if (o instanceof Peer) {
				return this.equals((Peer) o);
			}
		}

		return false;
	}
	
	public boolean equals(Peer o){
		if ((null != name) && (null != o.name))
			return name.equals(o.name);
		
		return false;
	}
	 
	
	public boolean equals(String o)	{		
		if (null != o)
			return name.equals(o);

		return false;
	}

	/**
	 * @return
	 */
	public InetAddress getAddress() {
		return addr;
	}

	/**
	 * @return the host name of the peer
	 */
	public String getHostName() {
		return addr.getHostName();
	}

	/**
	 * @return
	 */
	public String getIP() {
		return addr.getHostAddress();
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return
	 */
	public abstract boolean isClosed();

	/**
	 * @return
	 */
	public abstract boolean isConnected();

	/**
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public abstract int receive(byte[] data) throws IOException;
	
	/**
	 * @param buf
	 * @throws IOException
	 */
	public abstract void send(byte[] buf) throws IOException;

	/**
	 * @param string
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param portNum
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (null == name) {
			return "null";
		}

		return name;
	}
}
