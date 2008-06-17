/**
 * 
 */
package whiteboard.networking.venus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import whiteboard.core.CloseableThread;
import whiteboard.core.WhiteboardConfiguration;
import whiteboard.core.WhiteboardCore;
import whiteboard.core.entities.BytePacker;
import whiteboard.core.entities.ShapeConstants;
import whiteboard.core.transaction.NetworkTransactionManager;
import whiteboard.gui.whiteboard.WhiteboardWindow;
import whiteboard.networking.StreamPeer;
import whiteboard.networking.WhiteboardPeer;
import whiteboard.networking.mars.MarsProtocol;
import whiteboard.networking.venus.VenusClient.CONNECT_TYPE;


/**
 * @author patrick
 *
 */
public class VenusProtocol implements ListModel {
	public static final int PORT = 12345;
	public static final int PACKET_SIZE = 4096;
	public static final String CHARSET = "US-ASCII";

	public static final String NAME = "VENUS";
	public static final String VERSION = "1.0";
	public static final String PROTOCOL = NAME + "/" + VERSION;

	public static final String ELECT = "ELECT";
	public static final String ELECT_MSG = VenusProtocol.ELECT + VenusProtocol.LINE_END + VenusProtocol.LINE_END;
	public static final String ANSWER = "ANSWER";
	public static final String ANSWER_MSG = VenusProtocol.ANSWER + VenusProtocol.LINE_END + VenusProtocol.LINE_END;
	public static final String COORD = "COORD";
	public static final String KICK = "KICK";

	public static final String LINE_END = "\n";
	public static final String SHAPE_HEADER = "PACKET"+LINE_END;
	public static final int SHAPE_HEADER_SIZE = VenusProtocol.SHAPE_HEADER.getBytes().length;
	
	public static final String SHAPES_HEADER = "WHITEBOARD SHAPES"+LINE_END ;
	
	public static final int HEADERSIZE = SHAPE_HEADER_SIZE + ShapeConstants.INTEGER_BYTE_SIZE;
	
	private String userName;

	private final List<WhiteboardPeer> peers = new ArrayList<WhiteboardPeer>();

	private final List<ListDataListener> listDataListeners = new ArrayList<ListDataListener>();
	private WhiteboardConfiguration config;
	private ElectionSendTimer electionTimer;
	
	private boolean connectedToHead;
	protected WhiteboardWindow window;
	
	/***
	 * HACK: the transmanager is used only during elections. 
	 * It is null when an election is not in progress. DON'T USE
	 * FOR ANYTHING ELSE OR THE DEVIL WILL COME FOR YOUR CHILDREN
	 */
	private NetworkTransactionManager transMan = null;

	public VenusProtocol(WhiteboardConfiguration wConfig, WhiteboardWindow window) {
		this.window = window;
		this.userName = wConfig.getUserPeer().getName();
		config = wConfig;
		transMan = window.getTransactionManager();
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.ListModel#addListDataListener(javax.swing.event.ListDataListener)
	 */
	public void addListDataListener(ListDataListener l) {
		listDataListeners.add(l);
	}

	/* (non-Javadoc)
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	public Object getElementAt(int index) {
		return peers.get(index);
	}

	/* (non-Javadoc)
	 * @see javax.swing.ListModel#getSize()
	 */
	public int getSize() {
		return peers.size();
	}
	
	public void setConnectedToHead(boolean connected) {
		connectedToHead = connected;
	}
	
	public boolean getConnectedToHead() {
		return connectedToHead;
	}

	/* (non-Javadoc)
	 * @see javax.swing.ListModel#removeListDataListener(javax.swing.event.ListDataListener)
	 */
	public void removeListDataListener(ListDataListener l) {
		listDataListeners.remove(l);
	}

	/**
	 * @param peer
	 * @return
	 */
	public boolean removePeer(WhiteboardPeer peer) {
		// Get the index of the the whiteboard and remove it
		int index = peers.indexOf(peer);
		boolean result = peers.remove(peer);

		// If the whiteboard was added, notify listeners
		if (result) {
			Collections.sort(peers);

			// Create event & notify listeners
			notifyListeners(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index));
		}

		return result;
	}

	/**
	 * change the permission level of a peer, and send a notice out to listeners
	 * that the contents have changed in the list
	 * @param packet - the change permlevel packet
	 */
	public void changePeerPermLevel(byte[] packet) {
		String[] packetStr = new String(packet).split("\n");
		WhiteboardPeer peer = getPeer(packetStr[1]);
		if(!peers.contains(peer)) {
			return;
		}
		peer.setPermissionLevel(WhiteboardPeer.PERM_LEVEL.values()[Integer.parseInt(packetStr[2])]);
		Collections.sort(peers);
		int index = peers.indexOf(peer);
		notifyListeners(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index, index));
	}

	public void sortPeers() {
		Collections.sort(peers);
	}

	/**
	 * @param peer
	 * @return
	 */
	public boolean addPeer(WhiteboardPeer peer) {
		boolean result = false;
		
		// Add the whiteboard
		if (!peers.contains(peer)) {
			result = peers.add(peer);
			Collections.sort(peers);
		}

		// If the peer was added, notify listeners
		if (result) {
			// Get the index of the newly added board
			int index = peers.indexOf(peer);

			// Create event & notify listeners
			notifyListeners(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index));
		}

		return result;
	}

	/**
	 * notify all list listeners of event
	 * @param lde - the list event to send to all listeners
	 */
	private void notifyListeners(ListDataEvent lde) {
		// Notify all listeners
		for (ListDataListener listDataListener : listDataListeners) {
			if(lde.getType() == ListDataEvent.INTERVAL_ADDED)
				listDataListener.intervalAdded(lde);
			else if(lde.getType() == ListDataEvent.INTERVAL_REMOVED)
				listDataListener.intervalRemoved(lde);
			listDataListener.contentsChanged(lde);
		}
	}

	/**
	 * Get name
	 *
	 * @return the name
	 */
	public synchronized String getName() {
		return userName;
	}

	/**
	 * Set name
	 *
	 * @param name the name to set
	 */
	public synchronized void setName(String name) {
		this.userName = name;
	}

	/**
	 * Get peers
	 *
	 * @return the peers
	 */
	public synchronized List<WhiteboardPeer> getPeers() {
		return peers;
	}

	/**
	 * return the peer with the given name
	 * @param peerName - the name of the peer
	 * @return the whiteboardpeer with the given name
	 */
	public synchronized WhiteboardPeer getPeer(String peerName) {
		for(WhiteboardPeer p : peers) {
			if(p.getName().equals(peerName))
				return p;
		}
		return null;
	}

	/**
	 * Set peers
	 *
	 * @param peers the peers to set
	 */
	public synchronized void setPeers(List<WhiteboardPeer> peers) {
		this.peers.clear();
		this.peers.addAll(peers);
	}

	/**
	 * appends the venus PACKET header to a byte array
	 * @param packet - the packet to wrap in the header
	 * @return the packet wrapped in the PACKET header
	 */
	public static byte[] appendVenusPacketHeader(byte[] packet) {
		int entireHeaderSize = VenusProtocol.SHAPE_HEADER_SIZE + Integer.SIZE/8;
		byte[] headerPacket = new byte[packet.length + entireHeaderSize];
		for(int i = 0; i < headerPacket.length; ++i) {
			if(i < VenusProtocol.SHAPE_HEADER_SIZE)
				headerPacket[i] = VenusProtocol.SHAPE_HEADER.getBytes()[i];
			else if(i >= entireHeaderSize)
				headerPacket[i] = packet[i-entireHeaderSize];
		}
		BytePacker.insertInto_byte_Array(BytePacker.convertIntToBytes(packet.length), headerPacket, VenusProtocol.SHAPE_HEADER_SIZE);
		return headerPacket;
	}
	
	/***
	 * 
	 * @param peer the peer to do the receiveing from
	 * @return the byte-array
	 * @throws IOException if the connection to the peer is broken.
	 */
	public static byte[] receivePacket(StreamPeer peer) throws IOException
	{	
		byte[] shapeHeader = new byte[VenusProtocol.HEADERSIZE];
		if(peer == null)
			throw new IOException("Null peer");
		peer.receive(shapeHeader, VenusProtocol.HEADERSIZE);

		String head = new String(shapeHeader, 0, VenusProtocol.SHAPE_HEADER.getBytes().length );
		if (!head.equals(VenusProtocol.SHAPE_HEADER))
			throw new IOException(peer.getName());
		// index into array := minus int-size				
		int numBytes = (BytePacker.convertBytesToInt(shapeHeader, VenusProtocol.HEADERSIZE-ShapeConstants.INTEGER_BYTE_SIZE));
		byte[] newPacket = new byte[numBytes];
		
		peer.receive(newPacket, numBytes);
		return newPacket;
	}

	public WhiteboardConfiguration getWhiteboardConfig() {
		return config;
	}

	/**
	 * called when you receive an elect message
	 * @param peer
	 * @param man
	 */
	public void callElection(WhiteboardPeer peer, NetworkTransactionManager man) {
		//send answer to peer
		try {
			peer.send(VenusProtocol.appendVenusPacketHeader(VenusProtocol.ANSWER_MSG.getBytes()));
		} catch (IOException e) {
			// Do nothing
		}

		if(electionTimer != null && electionTimer.isRunning())
			return;
		//call a new election
		callElection(man);
	}

	/**
	 * called when you detect you have no head
	 * @param man
	 */
	public void callElection(NetworkTransactionManager man) {
		this.transMan = man;
		int index = peers.indexOf(config.getUserPeer());
		if (index == 0) {
			if(config.getUserPeer().getPermissionLevel() == WhiteboardPeer.PERM_LEVEL.OWNER)
				return;		//we're already the head, so this call isn't needed
			sendElectionCoord();
		} else {
			byte[] electPacket = VenusProtocol.appendVenusPacketHeader(ELECT_MSG.getBytes());
			electionTimer = new ElectionSendTimer("ElectionTimer");
			electionTimer.start();
			for (int i=0; i< index; ++i)
			{
				try {
					peers.get(i).send(electPacket);
				} catch (IOException e) {
					// nothing to do but ignore this one.
				}
			}
		}
	}

	protected void sendElectionCoord() {		
		stopElectionTimeout();
		if(transMan == null) {
			//something has gone wrong, as transman is only null when no election happening
			//so return, as this shouldn't happen
			return;
		}
		int newEpoch = transMan.getEpoch()+1;
		transMan.setEpoch(newEpoch);
		String coord = VenusProtocol.COORD 
					 + VenusProtocol.LINE_END 
					 + config.getUserPeer().getName() 
					 + VenusProtocol.LINE_END
					 + newEpoch
					 + VenusProtocol.LINE_END + VenusProtocol.LINE_END;
		byte[] coordPacket = VenusProtocol.appendVenusPacketHeader(coord.getBytes());

		electNewHead(config.getUserPeer().getName());
		for (WhiteboardPeer p : peers)
		{
			if (!config.getUserPeer().equals(p))
			{
				try {
					p.send(coordPacket);
				} catch (IOException e) {
					//swallow
				}
			}
		}
	}

	public void stopElectionTimeout() {
		if (electionTimer != null) {
			electionTimer.close();
			electionTimer.interrupt();
		}
	}

	public void kickPeer(String peer) {
		WhiteboardPeer wPeer = getPeer(peer);
		if(wPeer == null) {
			return;
		}
		if(wPeer.equals(config.getUserPeer())) {
			for(WhiteboardPeer p : getPeers()) {
				try {
					if(!p.equals(wPeer) && !p.getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.OWNER)) {
						p.close();
					}
				} catch(IOException e) {
					e.printStackTrace();
					// Do nothing
				}
			}
			window.safeExit(CONNECT_TYPE.KICKED, false);
		} else {
			try {
				wPeer.close();
			} catch (IOException e) {
				// Do nothing
			}
		}
		removePeer(wPeer);
	}

	/***
	 * 
	 * @author Afton
	 * A timer. If after the timout period we're still running
	 * we must be the head, so call an election.
	 *
	 */
	public class ElectionSendTimer extends CloseableThread {
		private boolean isRunning = false;
		public ElectionSendTimer(String name)
		{
			super(name);
		}
		
		@Override
		public void run()
		{
			try {
				isRunning = true;
				sleep(10000);
				if(isRunning)
					sendElectionCoord();
			} catch (InterruptedException e) {
				// swallow exception.
			}
			isRunning = false;
		}

		public boolean isRunning() {
			return isRunning;
		}

		@Override
		public void close() {
			isRunning = false;
		}
	}

	public void electNewHead(String headName) {
		WhiteboardPeer head = getPeer(headName);
		head.setPermissionLevel(WhiteboardPeer.PERM_LEVEL.OWNER);
		config.setHead(head);
		
		WhiteboardCore core = new WhiteboardCore(config.getName(), config.getPermissionLevel(), config.getPassword(), head);
		MarsProtocol.updateHead(config.getName(), core);

		notifyListeners(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, 0));
		
		// Update our peers from head.
		if(!headName.equals(config.getUserPeer().getName())) {
			// While we use TCP, doing nothing is fine, but as soon as we switch to UPD
			// we'll need to update our list of peers from the new head.
		}
		else 
		{
			MarsProtocol.send(MarsProtocol.updateMessage(config));
		}

		transMan = window.getTransactionManager();
		window.getCore().setPassword( core.getPassword() );
		if (transMan != null)
		{
			try {
				transMan.setElection(true);
				transMan.updateSequenceServer();
				transMan.setElection(false);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				callElection(transMan);
				return;
			}
//			transMan = null;
		}
	}
	
	

}