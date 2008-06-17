/**
 * 
 */
package whiteboard.networking.eris;
import java.io.IOException;

import whiteboard.core.entities.BytePacker;
import whiteboard.networking.Peer;
import whiteboard.networking.StreamPeer;
import whiteboard.networking.venus.VenusProtocol;

/**
 * @author patrick
 *
 */
public class ErisProtocol {
	public static final int PORT = 55555;
	public static final int PACKET_SIZE = 4096;
	public static final String CHARSET = "US-ASCII";

	public static final String NAME = "ERIS";
	public static final String VERSION = "1.0";
	public static final String PROTOCOL = NAME + "/" + VERSION;
	

	public static final String LINE_END = VenusProtocol.LINE_END;
	public static final String HEADER_PREFIX = VenusProtocol.SHAPE_HEADER;
	public static final int HEADER_SIZE = VenusProtocol.HEADERSIZE; 
	
	public static final byte[] initHandshake = ("HELLO " + PROTOCOL + LINE_END +LINE_END).getBytes();
	public static final String replyHandshakePREFIX = (PROTOCOL + "OK" + LINE_END + LINE_END);
	public static byte[] receivePacket(StreamPeer peer) throws IOException {
		return VenusProtocol.receivePacket(peer);
	}
	
	/**
	 * 
	 * @param peer
	 * @param replyAsBytes
	 * @throws IOException
	 * 
	 * Wrappes the packet with a PACKET header and the size of the packet as a 4 byte int, then
	 * sends it to the specified peer
	 */
	public static void wrapAndSend(Peer peer, byte[] replyAsBytes) throws IOException {
		byte[] wrappedPacket = new byte[replyAsBytes.length + HEADER_SIZE];
		for (int i=0; i<HEADER_PREFIX.getBytes().length; ++i)
		{
			wrappedPacket[i] = HEADER_PREFIX.getBytes()[i];
		}
		BytePacker.insertInto_byte_Array(BytePacker.convertIntToBytes(replyAsBytes.length), 
										 wrappedPacket, HEADER_PREFIX.getBytes().length);
		for (int i=0; i < replyAsBytes.length; ++i)
		{
			wrappedPacket[i+ HEADER_SIZE] = replyAsBytes[i];
		}
		peer.send(wrappedPacket);
	}
}
