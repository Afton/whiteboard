package whiteboard.core.transaction;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import whiteboard.core.exceptions.UpdateException;
import whiteboard.networking.Peer;

/**
 * 
 * @author Afton
 * provides basic functionality for a transaction manager. 
 *
 */
public interface NetworkTransactionManager {

	public List<byte[]> getHistory();
	
	/***
	 * 
	 * @param b the data protocol for an object request. 
	 * the user should make no assumptions about when this object
	 * will be added to the canvas, as this depends on a variety of 
	 * contextual factors
	 */
	public void pushToLocalShape(byte[] b);

	/**
	 * @param peer - the peer who sent this chat msg
	 * @param b - the chat data
	 */
	public void pushToLocalChat(Peer peer, byte[] b);

	/***
	 * 
	 * @return returns a possibly null byte array representing
	 * the data protocol level application data. Null if there are no
	 * current packets to send.
	 */
	public byte[] pullFromLocal();
	
	public void setElection(boolean election);
	public void updateSequenceServer() throws UpdateException, UnknownHostException, IOException;

	public void setEpoch(int epoch);
	public int getEpoch();
}
