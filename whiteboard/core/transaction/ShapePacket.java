package whiteboard.core.transaction;

import whiteboard.core.entities.*;
import whiteboard.core.entities.ShapeConstants.WB_REQUEST_TYPE;

/***
 * 
 * @author Afton 
 * parses the header out of the packet for ease of reference to type, 
 * sequenceNum, and epochNum
 * 
 */
public class ShapePacket implements Comparable<ShapePacket>{

	final public ShapeConstants.WB_REQUEST_TYPE type;
	final public Integer epoch;
	final public Integer sequence;
	public final Long hash;
	public final byte[] packet;
	public final int objectReference;
	public final Integer creationEpoch;
	public final Integer creationSequence;
	public boolean userCreated = false;
	
	public ShapePacket(byte[] packet) throws IllegalPacketSizeException
	{
		if (packet.length < ShapeConstants.PACKET_BASE_HEADER_OFFSET) {
			throw new IllegalPacketSizeException();
		}
		
		this.type = ShapeConstants.WB_REQUEST_TYPE.values()[packet[0]];
		this.epoch = BytePacker.convertBytesToInt(packet, ShapeConstants.PACKET_EPOCH_OFFSET);
		this.sequence = BytePacker.convertBytesToInt(packet, ShapeConstants.PACKET_SEQUENCE_NUMBER_OFFSET);
		this.packet = packet;
		this.hash = (((long) epoch) << 32) + sequence;
		
		if (type == WB_REQUEST_TYPE.EPOCH_SEQUENCE_REQUEST)
			objectReference = -1;
		else
			objectReference = BytePacker.convertBytesToInt(packet, ShapeConstants.PACKET_EXP_HEADER_OFFSET);
		
		switch (type)
		{
			case OBJECT_CREATION :
			case EPOCH_SEQUENCE_REQUEST : 
			{
				this.creationEpoch = this.epoch;
				this.creationSequence = this.sequence;
				break;
			}
			default:
			{
				if (packet.length < ShapeConstants.PACKET_EXP_HEADER_OFFSET)
					throw new IllegalPacketSizeException();
				this.creationEpoch = BytePacker.convertBytesToInt(packet, ShapeConstants.PACKET_CREATION_EPOCH);
				this.creationSequence = BytePacker.convertBytesToInt(packet, ShapeConstants.PACKET_CREATION_SEQUENCE);
				break;
			}
		}
	}
	
	/***
	 * 
	 * @param packet
	 * @param type
	 * @param epoch
	 * @param sequence
	 * This is a deadly packet handle with care. It is used only for dead requests and 
	 * this constructor should be either modified, or changed before it sees general use.
	 */
	public ShapePacket(byte[] packet, ShapeConstants.WB_REQUEST_TYPE type, int epoch, int sequence)
	{
		this.type = type;
		this.epoch = new Integer(epoch); 
		this.sequence = new Integer(sequence);
		this.packet = packet;
		this.hash = (((long) epoch) << 32) + sequence;
		objectReference = -1;
		this.creationEpoch = null;
		this.creationSequence = null;
	}
	

	
	@Override
	public boolean equals(Object o)
	{
		if (! (o instanceof ShapePacket))
			return false;
		return this.hashCode() == o.hashCode();		
	}
	
	@Override
	public int hashCode()
	{
		return this.hash.hashCode();
		
	}


	public int compareTo(ShapePacket arg0) {
		int epochs = this.epoch - arg0.epoch;
		if (0 == epochs)
		{
			return this.sequence - arg0.sequence;
		}
		return epochs;
	}
	
	
	
}
