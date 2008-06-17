/**
 * 
 */
package whiteboard.networking.venus;

import java.io.IOException;

import whiteboard.networking.WhiteboardPeer;

/**
 * @author patrick
 *
 */
public class VenusFactory {

	public static byte[] createPacket() {
		return new byte[VenusProtocol.PACKET_SIZE];
	}

	public static byte[] createPacket(int length) {
		return new byte[length];
	}

	/**
	 * @param packet
	 * @param length
	 * @return
	 */
	public static String[] getMessage(byte[] packet, int length) {
		return new String(packet, 0, length).split(VenusProtocol.LINE_END);
	}

	/**
	 * @param name
	 * @param ip
	 * @param port
	 * @return
	 */
	public static WhiteboardPeer createPeer(String name, byte[] ip, int port) throws IOException {
		return new WhiteboardPeer(name, ip, port);
	}

}
