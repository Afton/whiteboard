/**
 * 
 */
package whiteboard.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author patrick
 *
 */
public class WhiteboardPeer extends StreamPeer implements Comparable<WhiteboardPeer> {
	public static enum PERM_LEVEL {VIEWER, EDITOR, OWNER}
	private PERM_LEVEL permissionLevel;

	public WhiteboardPeer(Peer peer) throws UnknownHostException, IOException {
		this(peer.name, peer.addr.getAddress(), peer.port);
	}
	
	/**
	 * copy constructor
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public WhiteboardPeer(WhiteboardPeer peer) throws UnknownHostException, IOException {
		this(peer.getName(), peer.getAddress().getAddress(), peer.getPort(), peer.getPermissionLevel());
	}

	/**
	 * constructor
	 * @param name - name of the peer
	 * @param permissionLevel - permission level of the peer (OWNER, EDITOR, VIEWER)
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public WhiteboardPeer(String name, PERM_LEVEL permissionLevel) throws UnknownHostException, IOException {
		this(name, InetAddress.getLocalHost().getHostName(), 1, permissionLevel);
	}

	/**
	 * @param name - name of the peer
	 * @param addr
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public WhiteboardPeer(String name, byte[] addr, int port) throws UnknownHostException, IOException {
		this(name, addr, port, PERM_LEVEL.VIEWER);
	}

	/**
	 * @param name - name of the peer
	 * @param addr
	 * @param port
	 * @param permissionLevel - permission level of the peer (OWNER, EDITOR, VIEWER)
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public WhiteboardPeer(String name, byte[] addr, int port, PERM_LEVEL permissionLevel) throws UnknownHostException, IOException {
		super(name, addr, port);
		this.permissionLevel = permissionLevel;
	}

	/**
	 * @param host
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public WhiteboardPeer(String host, int port) throws UnknownHostException, IOException {
		this(null, host, port);
	}

	/**
	 * @param name - name of the peer
	 * @param host
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public WhiteboardPeer(String name, String host, int port) throws UnknownHostException, IOException {
		this(name, host, port, PERM_LEVEL.VIEWER);
	}

	/**
	 * @param name - name of the peer
	 * @param host
	 * @param port
	 * @param permissionLevel - permission level of the peer (OWNER, EDITOR, VIEWER)
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public WhiteboardPeer(String name, String host, int port, PERM_LEVEL permissionLevel) throws UnknownHostException, IOException {
		super(name, host, port);
		this.permissionLevel = permissionLevel;
	}

	/**
	 * @param addr
	 * @param port
	 */
	public WhiteboardPeer(InetAddress addr, int port) {
		this(addr, port, PERM_LEVEL.VIEWER);
	}

	/**
	 * @param addr
	 * @param port
	 * @param permissionLevel - permission level of the peer (OWNER, EDITOR, VIEWER)
	 */
	public WhiteboardPeer(InetAddress addr, int port, PERM_LEVEL permissionLevel) {
		super(addr, port);
		this.permissionLevel = permissionLevel;
	}

	/**
	 * @param socket
	 * @throws IOException
	 */
	public WhiteboardPeer(Socket socket) {
		this(socket, PERM_LEVEL.VIEWER);
	}

	/**
	 * @param socket
	 * @param permissionLevel - permission level of the peer (OWNER, EDITOR, VIEWER)
	 * @throws IOException
	 */
	public WhiteboardPeer(Socket socket, PERM_LEVEL permissionLevel) {
		super(socket);
		this.permissionLevel = permissionLevel;
	}

	/**
	 * @return the permissionLevel (OWNER, EDITOR, VIEWER)
	 */
	public PERM_LEVEL getPermissionLevel() {
		return permissionLevel;
	}

	/**
	 * @param permissionLevel - permission level of the peer (OWNER, EDITOR, VIEWER)
	 */
	public void setPermissionLevel(PERM_LEVEL permissionLevel) {
		this.permissionLevel = permissionLevel;
	}

	public int compareTo(WhiteboardPeer peer) {
		if (permissionLevel.equals(peer.getPermissionLevel())) {
			return name.compareToIgnoreCase(peer.getName());
		}
		return peer.getPermissionLevel().ordinal() - permissionLevel.ordinal();
	}

	public String getDebugInfo() {
		String info = " WhiteboardPeer: " + this.getName() + " HostName: " + 
					  "\n	Permissions: " + permissionLevel + 
					  "\n	Socket: " +
					  "\n		connected: " + this.socket.isConnected() +
					  "\n 		open: " + !this.socket.isClosed() + 
					  "\n	HostName/InetAddress: " + this.addr.toString();
		
		
		return info;
	}
}