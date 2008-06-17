/**
 * 
 */
package whiteboard.networking.venus;

import java.io.IOException;

import whiteboard.core.CloseableThread;
import whiteboard.core.transaction.NetworkTransactionManager;
import whiteboard.networking.WhiteboardPeer;

/**
 * @author patrick
 *
 */
public class VenusSendThread extends CloseableThread {
	private boolean done = false;
	private NetworkTransactionManager man;
	private VenusProtocol protocol;

	public VenusSendThread(VenusProtocol protocol, NetworkTransactionManager man) {
		super("VenusShapeSendThread");
		
		this.protocol = protocol;
		this.man = man;
	}
	
	@Override
	public void run() {
		// Get objects
		while (!done) {
			// Get next data packet
			byte[] shapeBytes = man.pullFromLocal();
			
			int numSent = 0;
			// Only process valid packets
			if (null != shapeBytes && numSent < 10) {
				byte[] packet = VenusProtocol.appendVenusPacketHeader(shapeBytes);

				// Send the packet to all peers
				for (WhiteboardPeer wbPeer : protocol.getPeers()) {
					try {
						if (!wbPeer.getName().equalsIgnoreCase(protocol.getName())) {
							wbPeer.send(packet);
						}
					} catch (IOException ioe) {
						System.err.println("VENUSSND: " + ioe.getMessage());
					}
					numSent++;
				}
			} else {
				try {
					sleep(100);
					
				} catch (InterruptedException e) {
					// Do nothing
				}
				numSent = 0;
			}
		}
		
	}
	
	/* (non-Javadoc)
	 * @see whiteboard.networking.venus.VenusThread#close()
	 */
	@Override
	public void close() {
		done = true;
	}
}
