/**
 * 
 */
package whiteboard.networking;

import whiteboard.networking.mars.MarsProtocol;


/**
 * @author patrick
 *
 */
public final class NetworkingUtils {

	public final static byte[] convertIPtoArray(final String str) {
		final String[] ipStr = str.split("[.]");
		final byte[] ip = new byte[ipStr.length];
		
		for (int i = 0; i < ipStr.length; i++) {
			ip[i] = (byte) Integer.parseInt(ipStr[i]);
		}
		
		return ip;
	}

	/**
	 * Convert a packet (byte array) into a message (String array)
	 * 
	 * @param packet The packet to convert
	 * @param length The length of valid data
	 * @return String array with each element a line from the packet
	 */
	public static String[] getMessage(final byte[] packet, final int length) {
		if(length <= 0)
			return new String[0];
		final String p = new String(packet, 0, length);
		
		if (p.equals(""))
			return new String[0];
		
		return p.split(MarsProtocol.LINE_END);
	}
}
