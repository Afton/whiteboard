/**
 * 
 */
package whiteboard.networking.eris;

/**
 * @author patrick
 *
 */
public class ErisFactory {
	public static byte[] createPacket() {
		return new byte[ErisProtocol.PACKET_SIZE];
	}
}
