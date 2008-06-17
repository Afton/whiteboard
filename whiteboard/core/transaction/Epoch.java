package whiteboard.core.transaction;

import java.util.*;
import java.util.PriorityQueue;

/***
 * 
 * @author Afton
 * Each epoch holds all the info we need to keep track of missing packets, 
 * including keeping track of packets recieved but that we can't act on yet. 
 *
 */
public class Epoch {
	
	/** give the epoch some introspection */
	public final Integer epoch;

	/** Everything goes in the history. Avoids multiply processing the same packet	 */
	public LinkedList<ShapePacket> processedShapes = new LinkedList<ShapePacket>();
	
	/** packets that are waiting for an earlier packet before they can be processed */
	public PriorityQueue<ShapePacket> pending = new PriorityQueue<ShapePacket>();		
		
	public Epoch (Integer epoch)
	{
		this.epoch = new Integer(epoch);
	}

	// simple linear search
	// we don't need to the pending packets, just ones that have already been serviced.
	public List<byte[]> findAll( Integer objId )
	{
		List<byte[]> history = new LinkedList<byte[]>();
		for (ShapePacket p: processedShapes )
		{
			if (p.hashCode() == objId.hashCode() )
			{
				history.add( p.packet );
			}
		}
		return history;
	}
	
	// simple linear search
	public ShapePacket findPacket(Integer sequenceNumber) {
		for(ShapePacket p : processedShapes) {
			if(p.sequence.equals(sequenceNumber))
				return p;
		}
		return null;
	}
}
