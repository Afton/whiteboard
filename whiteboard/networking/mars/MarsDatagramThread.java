/**
 * 
 */
package whiteboard.networking.mars;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

import whiteboard.core.CloseableThread;
import whiteboard.core.WhiteboardCore;
import whiteboard.networking.NetworkingUtils;
import whiteboard.networking.Peer;
import whiteboard.networking.WhiteboardPeer;

/**
 * @author patrick
 */
public class MarsDatagramThread extends CloseableThread {
	private DatagramSocket datagramSocket;
	private String peerName;

	public MarsDatagramThread(String peerName) throws SocketException {
		this(peerName, MarsProtocol.PORT);
	}

	public MarsDatagramThread(String peerName, final int port) throws SocketException {
		super("DatagramThread");
		datagramSocket = new DatagramSocket(port);
		this.peerName = peerName;
	}

	@Override
	public void close() {
		if ((null != datagramSocket) && (!datagramSocket.isClosed())) {
			datagramSocket.close();
		}
	}

	@Override
	public void run() {
		while (!datagramSocket.isClosed()) {
			try {
				boolean found = false;
				// Receive a packet
				final byte[] buf = MarsProtocol.createPacket();
				final DatagramPacket packet = new DatagramPacket(buf, buf.length);
				datagramSocket.receive(packet);

				// Validate peer
				InetAddress packetAddr = packet.getAddress();
				List<Peer> peers = MarsProtocol.getPeers();

				for (int i = 0; (i < peers.size()) && !found; ++i) {
					Peer peer = peers.get(i);
					if (peer.getAddress().equals(packetAddr)) {
						found = true;
					}
				}

				if (found) {
					// Get message
					final byte[] bytes = packet.getData();
					final String[] message = NetworkingUtils.getMessage(bytes, bytes.length);

					// Process message
					if (message[0].equals("CREATE")) {
						final String boardName = message[1];
						final int permissionLevel = Integer.parseInt(message[2]);
						final String headName = message[3];
						int boardPort = Integer.parseInt(message[4]);

						// Lookup head
						final int headIndex = MarsProtocol.getPeers().indexOf(MarsProtocol.createPeer(headName, new byte[4], boardPort));
	
						if (-1 != headIndex) {
							final byte[] addr = MarsProtocol.getPeers().get(headIndex).getAddress().getAddress();
							MarsProtocol.addWhiteboard(new WhiteboardCore(boardName, WhiteboardCore.WB_PERM_LEVEL.values()[permissionLevel], headName, addr, boardPort));
						}
					} else if (message[0].equals("DELETE")) {
						final String boardName = message[1];
						MarsProtocol.removeWhiteboard(boardName);
					} else if (message[0].equals(MarsProtocol.UPDATE)) {
						WhiteboardCore core = MarsProtocol.getWhiteboard(message[1]);
						if(core != null)
							core.setHead(new WhiteboardPeer(message[2], packetAddr.getAddress(), Integer.parseInt(message[4]), WhiteboardPeer.PERM_LEVEL.OWNER));
					} else if (message[0].equals("ADD")) {
						final String name = message[1];
						final byte[] addr = NetworkingUtils.convertIPtoArray(message[2]);
						final int port = Integer.parseInt(message[3]);
						
						// If it's not us, remove and add peer
						if (!peerName.equals(name)) {
							MarsProtocol.removePeer(name);
							MarsProtocol.addPeer(MarsProtocol.createPeer(name, addr, port));
						}
					}
				}
			} catch (final IOException ioe) {
				if(!ioe.getMessage().equals("socket closed"))
					System.err.println("MARSDATA: " + ioe.getMessage());
			} catch(NumberFormatException nfe) {
				//swallow exception
			}
		}
	}
}
