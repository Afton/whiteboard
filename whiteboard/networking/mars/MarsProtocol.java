/**
 * 
 */
package whiteboard.networking.mars;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import whiteboard.core.WhiteboardConfiguration;
import whiteboard.core.WhiteboardCore;
import whiteboard.networking.DatagramPeer;
import whiteboard.networking.Peer;

/**
 * Singleton class for common and low-level Mars Protocol operations
 * 
 * @author patrick
 */
public class MarsProtocol implements ListModel {
	public static final int PORT = 17376;
	public static final int PACKET_SIZE = 4096;
	public static final String CHARSET = "US-ASCII";

	public static final String NAME = "MARS";
	public static final String VERSION = "1.0";
	public static final String PROTOCOL = NAME + "/" + VERSION;

	public static final String LINE_END = "\n";
	public static final String UPDATE = "UPDATE";

	private static final MarsProtocol instance = new MarsProtocol();

	private static final List<Peer> peers = new ArrayList<Peer>();
	private static final List<WhiteboardCore> whiteboards = new ArrayList<WhiteboardCore>();

	private static final List<ListDataListener> listDataListeners = new ArrayList<ListDataListener>();

	/**
	 * Add a peer to the list of known peers
	 * 
	 * @param peer the peer to add to the list
	 * @return true if peer added successfully, false otherwise
	 */
	public static synchronized boolean addPeer(Peer peer) {
		boolean result = false;

		if (!peers.contains(peer)) {
			try {
				peer.connect();
				result = peers.add(peer);
			} catch (IOException ioe) {
				// Don't have to do anything, it'll return false now
			}
		}

		return result;
	}

	/**
	 * Add a whiteboard to the list of known whiteboards
	 * 
	 * @param whiteboard the whiteboard to add
	 * @return true if whiteboard added successfully, false otherwise
	 */
	@SuppressWarnings("unchecked")
	public static synchronized boolean addWhiteboard(WhiteboardCore whiteboard) {
		boolean result = false;

		// Add the whiteboard
		if (!whiteboards.contains(whiteboard)) {
			result = whiteboards.add(whiteboard);
			Collections.sort(whiteboards);
		}

		// If the whiteboard was added, notify listeners
		if (result) {
			// Get the index of the newly added board
			int index = whiteboards.indexOf(whiteboard);

			// Create event
			ListDataEvent ldeAdded = new ListDataEvent(instance, ListDataEvent.INTERVAL_ADDED, index, index);

			// Notify all listeners
			for (ListDataListener listDataListener : listDataListeners) {
				listDataListener.intervalAdded(ldeAdded);
				listDataListener.contentsChanged(ldeAdded);
			}
		}

		return result;
	}

	/**
	 * Create a new packet of default length
	 * 
	 * @return byte array of default packet size
	 */
	public static byte[] createPacket() {
		return createPacket(PACKET_SIZE);
	}

	/**
	 * Create a new packet of specified length
	 * 
	 * @param length the length of the packet to create
	 * @return byte array of specified length
	 */
	public static byte[] createPacket(int length) {
		return new byte[length];
	}

	/**
	 * Create a new peer
	 * 
	 * @param name name of peer
	 * @param ip byte array representing the ip address of peer
	 * @param port the port to connect to on peer
	 * @return the newly created (Datagram) peer
	 * @throws IOException
	 */
	public static Peer createPeer(String name, byte[] ip, int port) throws IOException {
		return new DatagramPeer(name, ip, port);
	}

	/**
	 * Get the set of known peers
	 * 
	 * @return Set of all known peers
	 */
	public static List<Peer> getPeers() {
		return peers;
	}

	/**
	 * Get the list of known whiteboards
	 * 
	 * @return List of all known whiteboards
	 */
	public static List<WhiteboardCore> getWhiteboards() {
		return whiteboards;
	}

	public static WhiteboardCore getWhiteboard(String name) {
		for (WhiteboardCore c : whiteboards) {
			if (c.getName().equals(name)) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Remove a peer from the list of known peers
	 * 
	 * @param peer the peer to remove
	 * @return true if peer removed successfully, false otherwise
	 */
	public static synchronized boolean removePeer(Peer peer) {
		return peers.remove(peer);
	}

	/**
	 * Remove a peer from the list of known peers
	 * 
	 * @param name the name of peer to remove
	 * @return true if peer removed successfully, false otherwise
	 */
	public static synchronized boolean removePeer(String name) {
		return peers.remove(name);
	}

	@SuppressWarnings("unchecked")
	public static synchronized boolean removeWhiteboard(WhiteboardCore whiteboard) {

		// Get the index of the the whiteboard and remove it
		int index = whiteboards.indexOf(whiteboard);
		boolean result = whiteboards.remove(whiteboard);

		// If the whiteboard was added, notify listeners
		if (result) {
			Collections.sort(whiteboards);

			// Create events
			ListDataEvent ldeRemoved = new ListDataEvent(instance, ListDataEvent.INTERVAL_REMOVED, index, index);

			// Notify all listeners
			for (ListDataListener listDataListener : listDataListeners) {
				listDataListener.intervalAdded(ldeRemoved);
				listDataListener.contentsChanged(ldeRemoved);
			}
		}

		return result;
	}

	/**
	 * Remove a whiteboard from the list of known whiteboards
	 * 
	 * @param name The name of whiteboard to remove
	 * @return true if whiteboard removed successfully, false otherwise
	 */
	public static synchronized boolean removeWhiteboard(String name) {
		return removeWhiteboard(new WhiteboardCore(name, WhiteboardCore.WB_PERM_LEVEL.UNLOCKED, null));
	}

	/**
	 * Send a packet to all known peers
	 * 
	 * @param packet The packet (byte array) to send
	 */
	public static void send(byte[] packet) {
		for (Peer peer : peers) {
			try {
				peer.send(packet);
			} catch (IOException ioe) {
				// Do nothing
			}
		}
	}

	/**
	 * Send a (new) peer to all known peers
	 * 
	 * @param peer The peer to send
	 */
	public static void sendPeer(Peer peer) {
		// Create message
		StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append("ADD" + MarsProtocol.LINE_END);
		messageBuffer.append(peer.getName() + MarsProtocol.LINE_END);
		messageBuffer.append(peer.getIP() + MarsProtocol.LINE_END);
		messageBuffer.append(peer.getPort() + MarsProtocol.LINE_END);

		// Indiciate end of message
		messageBuffer.append(MarsProtocol.LINE_END);

		// Get message as bytes
		byte[] packet = messageBuffer.toString().getBytes();

		// Send message
		MarsProtocol.send(packet);
	}

	// public static void setListenPort(int port) {
	// listenPort = port;
	// }

	// public static void setName(String name ) {
	// MarsProtocol.name = name;
	// }

	/**
	 * Set the list of known peers
	 * 
	 * @param peers The new list of peers to use
	 */
	public static synchronized void setPeers(List<Peer> peers) {
		// Clear the list
		MarsProtocol.peers.clear();

		// Add the new peers
		MarsProtocol.peers.addAll(peers);
	}

	/**
	 * Set the list of known whiteboards
	 * 
	 * @param whiteboards The new list of whiteboards to use
	 */
	public static synchronized void setWhiteboards(List<WhiteboardCore> whiteboards) {
		// Clear the list
		MarsProtocol.whiteboards.clear();

		// Add the new whiteboards
		MarsProtocol.whiteboards.addAll(whiteboards);

		// Create event
		ListDataEvent ldeAdded = new ListDataEvent(instance, ListDataEvent.INTERVAL_ADDED, 0, MarsProtocol.whiteboards.size());

		// Notify all listeners
		for (ListDataListener listDataListener : listDataListeners) {
			listDataListener.intervalAdded(ldeAdded);
			listDataListener.contentsChanged(ldeAdded);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.ListModel#addListDataListener(javax.swing.event.ListDataListener)
	 */
	public synchronized void addListDataListener(ListDataListener listDataListener) {
		listDataListeners.add(listDataListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	public synchronized WhiteboardCore getElementAt(int index) {
		return MarsProtocol.getWhiteboards().get(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.ListModel#getSize()
	 */
	public synchronized int getSize() {
		return MarsProtocol.getWhiteboards().size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.ListModel#removeListDataListener(javax.swing.event.ListDataListener)
	 */
	public synchronized void removeListDataListener(ListDataListener listDataListener) {
		listDataListeners.remove(listDataListener);
	}

	public static byte[] updateMessage(WhiteboardConfiguration config) {
		try {
			String message = MarsProtocol.UPDATE + MarsProtocol.LINE_END + config.getName() + MarsProtocol.LINE_END + config.getHead().getName() + MarsProtocol.LINE_END + InetAddress.getLocalHost().getHostAddress() + MarsProtocol.LINE_END + config.getPort() + MarsProtocol.LINE_END + MarsProtocol.LINE_END;
			return message.getBytes();
		} catch (Exception e) {
			return null;
		}
	}

	/***************************************************************************
	 * @param oldWhiteboardName the name of the previously headless whiteboard
	 * @param newCore the core for the new core
	 */
	public synchronized static void updateHead(String oldWhiteboardName, WhiteboardCore newCore) {
		Iterator<WhiteboardCore> iter = whiteboards.iterator();
		while (iter.hasNext()) {
			if (oldWhiteboardName.equals(iter.next().getName())) {
				iter.remove();
			}
		}
		addWhiteboard(newCore);

	}
	
	public static boolean isPortFree(int port)
	{
		for (WhiteboardCore core : whiteboards )
		{
			if (core.getHead().getPort() == port)
					return false;
		}
		return true;
		
	}
	
	public static void clear()
	{
		whiteboards.clear();
		peers.clear();
	}
	
}
