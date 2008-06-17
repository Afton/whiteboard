/**
 * 
 */
package whiteboard.networking.venus;

import java.io.IOException;

import whiteboard.core.CloseableThread;
import whiteboard.core.entities.ShapeConstants;
import whiteboard.core.transaction.NetworkTransactionManager;
import whiteboard.networking.WhiteboardPeer;

/**
 * @author patrick
 */
public class VenusReceiveThread extends CloseableThread {
	private WhiteboardPeer peer;
	private VenusProtocol venusProtocol;
	private NetworkTransactionManager man;

	public VenusReceiveThread(WhiteboardPeer peer, VenusProtocol venusProtocol, NetworkTransactionManager man) {
		super("VenusShapeReceiveThread");

		this.peer = peer;
		this.venusProtocol = venusProtocol;
		this.man = man;
	}

	@Override
	public void run()
	{
		try {
			while (true)
			{
				// recieve packet
				byte[] packet = VenusProtocol.receivePacket(peer);
				
				// route packet to appropriate queue
				String packetHeader = new String(packet).split("\n")[0];
				if(packetHeader.equals(VenusProtocol.ANSWER)) {
					venusProtocol.stopElectionTimeout();
				} else if(packetHeader.equals(VenusProtocol.ELECT)) {
					man.setElection(true);
					venusProtocol.callElection(peer, man);
				} else if(packetHeader.equals(VenusProtocol.COORD)) {
					String[] msg = new String(packet).split("\n");
					man.setEpoch(Integer.parseInt(msg[2]));
					venusProtocol.stopElectionTimeout();
					venusProtocol.electNewHead(msg[1]);
				} else if(packetHeader.equals(VenusProtocol.KICK)) {
					venusProtocol.kickPeer(new String(packet).split("\n")[1]);
				} else if(ShapeConstants.WB_REQUEST_TYPE.CHAT.ordinal() == packet[0]) {
					man.pushToLocalChat(peer, packet);
				} else if(ShapeConstants.WB_REQUEST_TYPE.WB_PERM_CHANGE.ordinal() == packet[0]) {
					venusProtocol.changePeerPermLevel(packet);
				} else {
					// Push it on to the queue for processing
					man.pushToLocalShape(packet);
				}
			}
		} catch (IOException e) {
			venusProtocol.removePeer(peer);
			close(); // if we get an IOException, the pipe is broken, right?
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see whiteboard.networking.venus.VenusThread#close()
	 */
	@Override
	public void close() {
		if (null != peer) {
			try {
				// Attempt tidy shutdown
				peer.close();
			} catch (Exception e) {
				// Do nothing
			}
			peer = null;
		}
	}
}
