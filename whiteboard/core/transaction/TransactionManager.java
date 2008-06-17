package whiteboard.core.transaction;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import whiteboard.core.Pair;
import whiteboard.core.entities.BytePacker;
import whiteboard.core.entities.ShapeConstants;
import whiteboard.core.entities.WB_Chat;
import whiteboard.core.entities.ShapeConstants.WB_REQUEST_TYPE;
import whiteboard.core.exceptions.UpdateException;
import whiteboard.gui.whiteboard.Canvas;
import whiteboard.networking.Peer;
import whiteboard.networking.StreamPeer;
import whiteboard.networking.WhiteboardPeer;
import whiteboard.networking.eris.ErisClient;
import whiteboard.networking.eris.ErisProtocol;
import whiteboard.networking.eris.ErisServer;
import whiteboard.networking.eris.SequenceServer;
import whiteboard.networking.venus.VenusProtocol;

/**
 * 
 * @author Afton
 * This implements a middle-layer between the raw requests of the networking and
 * the local whiteboard layers.
 * 
 * this is where you go to find the latest state of an object
 *
 */
public class TransactionManager implements NetworkTransactionManager, LocalTransactionManager {
	static final public Integer timeout_milliseconds = new Integer(3000);
	static final public Integer timeout_range = new Integer(2000);
	static final public Integer startSequence = 0;

	private SequenceServer sequenceServer;
	private Canvas canvas;
	private VenusProtocol venusProtocol;
	
	private BlockingQueue<byte[]> forNetwork = new LinkedBlockingQueue<byte[]>();
	private BlockingQueue<ShapePacket> forLocalShape = new PriorityBlockingQueue<ShapePacket>();
	private Queue<Pair<Peer, WB_Chat>> forLocalChat = new LinkedList<Pair<Peer, WB_Chat>>();
	
	private SequenceTracker sequenceTracker = new SequenceTracker();
	private volatile NetworkBuffer netBufferSender = new NetworkBuffer("bufferMonitor");
	
	private boolean electionInProgress = false;
	
	public TransactionManager(WhiteboardPeer head, Canvas canvas, VenusProtocol venusProtocol) throws UnknownHostException, IOException
	{
		if(canvas.getWindow().getConfig().getUserPeer().equals(head)) {
			sequenceServer = new ErisServer(0);
		} else {
			sequenceServer = new ErisClient(new StreamPeer(head.getName(), head.getAddress().getAddress(), ErisProtocol.PORT));
		}
		sequenceServer.start();
		this.canvas = canvas;
		this.venusProtocol = venusProtocol;
		this.netBufferSender.start();
	}

	public void stopThreads() {
		if(netBufferSender != null) {
			netBufferSender.stop_processing();
		}
		if(sequenceServer != null) {
			sequenceServer.close();
		}
	}

	public void updateSequenceServer() throws UpdateException, UnknownHostException, IOException {
		WhiteboardPeer head = venusProtocol.getPeers().get(0);
		if(!head.getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.OWNER)) {
			//the "head" isn't actually the head
			throw new UpdateException("Invalid head");
		} else if(!electionInProgress) {
			throw new UpdateException("No Election in progress");
		}

		sequenceServer.close();
		if(canvas.getWindow().getConfig().getUserPeer().equals(head)) {
			sequenceServer = new ErisServer(getNextEpochNum());
		} else {
			sequenceServer = new ErisClient(new StreamPeer(head.getName(), head.getAddress().getAddress(), ErisProtocol.PORT));
		}
		sequenceServer.start();
	}
	
	public void pushToLocalShape(byte[] b)
	{
		try {
			ShapePacket packet = new ShapePacket(b);
			sequenceTracker.addPacket(packet);
		} catch (IllegalPacketSizeException e) {
			// nothing to do but drop it and log it. 
			System.err.println("TRANSMAN: Recieved a garbled packet. contents: " + b.toString());
		}
	}

	public void pushToLocalChat(Peer peer, byte[] b) {
		WB_Chat c = new WB_Chat(b);
		Pair<Peer, WB_Chat> pair = new Pair<Peer, WB_Chat>(peer, c);
		if (!forLocalChat.contains(pair)) {
			forLocalChat.offer(pair);
		}
	}

	public Pair<Peer, WB_Chat> pullFromNetworkChat() {
		return forLocalChat.poll();
	}
	
	public byte[] pullFromLocal() {
		return forNetwork.poll();
	}			
	
	/***
	 * This pushes the data into a queue, where it is sent at the transaction manager's descretion
	 */
	public void pushToNetwork(byte[] shape, ShapeConstants.WB_REQUEST_TYPE type)
	{
		netBufferSender.add(new Pair<byte[],WB_REQUEST_TYPE>(shape,type));
	}

	/* (non-Javadoc)
	 * @see whiteboard.core.transaction.NetworkTransactionManager#getHistory()
	 */
	public List<byte[]> getHistory() {
		return sequenceTracker.getHistoryToCurrent();
	}

	public int getNextEpochNum() {
		return sequenceTracker.epochs.size();
	}

	public void setSequenceServer(SequenceServer sServer) {
		this.sequenceServer = sServer;
	}

	protected boolean sendShapeToAll(Pair<byte[],WB_REQUEST_TYPE> shapePair) throws IllegalPacketSizeException
	{
		byte[] packet = createShapePacket(shapePair);
		if (packet.length == 0)
		{
			return false;
		}
		ShapePacket fromLocal = new ShapePacket(packet);
		fromLocal.userCreated = true;
		sequenceTracker.addPacket(fromLocal);
		forNetwork.offer(packet);
		return true;
	}

	private byte[] createShapePacket(Pair<byte[], WB_REQUEST_TYPE> shapePair) {
		WB_REQUEST_TYPE type = shapePair.getSecond();
		byte[] shape = shapePair.getFirst();
		byte[] packet = new byte[ShapeConstants.PACKET_EXP_HEADER_OFFSET + shape.length];
		
		packet[0] = new Integer(type.ordinal()).byteValue();					
		int epochNum = sequenceServer.getEpochNum();
		int sequenceNum = sequenceServer.getSequenceNum();
		if (sequenceNum == -1) // head timeout, election in progress
		{
			return new byte[0];
		}

		int creationEpoch, creationSequence;
		if (type == WB_REQUEST_TYPE.OBJECT_CREATION) {
			creationEpoch = epochNum;
			creationSequence = sequenceNum;
		} else {
			//get the unique object id from the shape, find the creation epoch and seq num
			Pair<Integer, Integer> pair = sequenceTracker.getEpochSequence(BytePacker.convertBytesToInt(shape, 0));
			creationEpoch = pair.getFirst();
			creationSequence = pair.getSecond();
		}

		BytePacker.insertInto_byte_Array(BytePacker.convertIntToBytes(epochNum), packet, ShapeConstants.PACKET_EPOCH_OFFSET);
		BytePacker.insertInto_byte_Array(BytePacker.convertIntToBytes(sequenceNum), packet, ShapeConstants.PACKET_SEQUENCE_NUMBER_OFFSET);
		BytePacker.insertInto_byte_Array(BytePacker.convertIntToBytes(creationEpoch), packet, ShapeConstants.PACKET_CREATION_EPOCH);
		BytePacker.insertInto_byte_Array(BytePacker.convertIntToBytes(creationSequence), packet, ShapeConstants.PACKET_CREATION_SEQUENCE);

		for (int i=0; i<shape.length; ++i)
		{
			packet[i+ShapeConstants.PACKET_EXP_HEADER_OFFSET] = shape[i];
		}
		return packet;
	}
	
	public ShapePacket pullShapeFromNetwork()
	{			
		return forLocalShape.poll();
	}
		
	protected class SequenceTracker{

		private HashMap<Integer,Epoch> epochs = new HashMap<Integer, Epoch>();
		
		/** this is a map from epoch numbers to a map from sequenceNumbers to the thread that is looking for 
		 * that sequence number
		 */
		private HashMap<Integer, HashMap<Integer,MissingSequenceTracker>> missingPacketFinders = 
			new HashMap<Integer, HashMap<Integer,MissingSequenceTracker>>();

		public SequenceTracker (){}

		
		public List<byte[]> getHistoryToCurrent()
		{
			// get the list of epochs, and sort them into ascending order
			List<Integer> epochList = new ArrayList<Integer>( epochs.keySet() );
			Collections.sort(epochList);
		
			// for every epoch: first load the packets we've processed, then add 
			// the one's we have pending.
			List<byte[]> packets = new LinkedList<byte[]>(); 
			for (Integer era : epochList)
			{
				Epoch e = epochs.get(era);
				for (ShapePacket packet : e.processedShapes)
				{
					packets.add(packet.packet);					
				}
				for (ShapePacket packet : e.pending)
				{
					packets.add(packet.packet);
				}
			}
			
			return packets;
		}
		
		private List<byte[]> getHistory(int object_id )
		{
			// get the list of epochs, and sort them into ascending order			
			List<Integer> epochlist = new ArrayList<Integer>( epochs.keySet() );
			Collections.sort(epochlist);
		
			List<byte[]> objectHistory = new ArrayList<byte[]>();
			Iterator<Integer> it = epochlist.iterator();
			while ( it.hasNext() )
			{
				Integer index = it.next();
				objectHistory.addAll( epochs.get(index).findAll( object_id ) );
			}
			return objectHistory;
		}

		private Pair<Integer, Integer> getEpochSequence(int objRef) {
			for (Epoch e : epochs.values())
			{
				for(ShapePacket p : e.processedShapes) {
					if((p.type == ShapeConstants.WB_REQUEST_TYPE.OBJECT_CREATION) && (p.objectReference == objRef)) {
						return new Pair<Integer, Integer>(p.epoch, p.sequence);
					}
				}
			}
			
			return new Pair<Integer, Integer>(0, 0);
		}

		protected Pair<Integer, Integer> getLastEpochSequenceNum() {
			// get the list of epochs, and sort them into ascending order
			if((epochs == null) || (epochs.isEmpty()))
				return new Pair<Integer, Integer>(0, 0);
			List<Integer> epochList = new ArrayList<Integer>(epochs.size());
			for (Integer era : epochs.keySet()) {
				epochList.add(era);
			}
			Collections.sort(epochList);

			//get last sequence number for current epoch
			return new Pair<Integer, Integer>(epochList.get(epochList.size()-1), epochs.get(epochList.get(epochList.size()-1)).processedShapes.getLast().sequence);
		}
		
		/***
		 * 
		 * @param packet the packet to be added to the list
		 */
		public void addPacket(ShapePacket packet)
		{				
			//received a packet, so reset the sequence updater
			/* initialize the current epoch if necessary */
			Epoch current = epochs.get(packet.epoch); 
			if (current == null)
			{
				current = new Epoch(packet.epoch);
				epochs.put(packet.epoch, current);
				/* initialize the hash of sequence->seeking threads */
				missingPacketFinders.put(packet.epoch, new HashMap<Integer,MissingSequenceTracker>());
			}
			
			/** handle sequence requests */
			if (packet.type == WB_REQUEST_TYPE.EPOCH_SEQUENCE_REQUEST)
			{
				processSequenceRequest(packet, current);					
				return;
			}
			
			/*** FIXME: Nothing here ensures that all packets from an old epoch number 
			 * are processed before a newer epoch. A real fix requires recreating the
			 * history of packets when a new element from an old epoch arrives. See comment
			 * below for a similar problem with recovery of a previously-marked-missing packet
			 */
			
			
			LinkedList<ShapePacket> epochProcessedShapes = current.processedShapes;
			// If we've already processed this guy, drop it on the floor. we're done.
			// FIXME: Not fixing this yet, a better way to handle this would be to check
			// if the packet we have in our processed list is a null packet, if it is then
			// replace it with the new incoming packet. If not, then we can safely drop this
			// as a duplicate packet.
			if (epochProcessedShapes.contains(packet))
			{
					return;
			}			
			
			if (epochProcessedShapes.isEmpty())
			{
				if (packet.sequence == startSequence)
				{
					add(packet, epochProcessedShapes);
				}
				else
				{
					current.pending.add(packet);					
					seekMissingPackets(packet.epoch, packet.sequence);
					
				}
			}
			else
			{
				// if we're currently looking for the packet, stop the search.
				Thread n = missingPacketFinders.get(packet.epoch).get(packet.sequence);
				if (n != null)
				{
					// thread ends, then we remove reference to it. 
					n.interrupt();
					missingPacketFinders.get(packet.epoch).remove(packet.sequence);
					
				}

				if((packet.type != ShapeConstants.WB_REQUEST_TYPE.OBJECT_CREATION) && (epochs.get(packet.creationEpoch).findPacket(packet.creationSequence) == null)) {
					//mod packet, and original creation packet not found
					
					current.pending.add(packet);
					seekMissingPackets(packet.creationEpoch, packet.creationSequence);
				} else if(packet.sequence != (epochProcessedShapes.getLast().sequence+1)) {
					//if not next sequence, then add to pending and request
					
					current.pending.add(packet);
					seekMissingPackets(packet.epoch, packet.sequence);
				}
				else
				{
					add(packet, epochProcessedShapes);
				}

				checkPendingPackets(current, epochProcessedShapes);
			}										
		}


		private void checkPendingPackets(Epoch current, LinkedList<ShapePacket> epochProcessedShapes) {
			// does the pending queue have our next packet?
			if ( ! current.pending.isEmpty() && (epochProcessedShapes.size() > 0))
			{
				/** packets.add(packet.packet);
				 * While there are elements pending, and they're contiguous with
				 * the previous packets, pull 'em off, and add them to the canvas
				 */
				ShapePacket currentPacket = epochProcessedShapes.getLast(); 
				ShapePacket nextPacket = current.pending.peek();	
				while (nextPacket != null && nextPacket.sequence.equals(currentPacket.sequence+1) )
				{
					if((nextPacket.type != ShapeConstants.WB_REQUEST_TYPE.OBJECT_CREATION) 
							&& (nextPacket.type != ShapeConstants.WB_REQUEST_TYPE.NULL_REQUEST)
							&& (epochs.get(nextPacket.creationEpoch).findPacket(nextPacket.creationSequence) == null)) {
						//if it's a mod, ensure that the object it's on is created, if not request it
						seekMissingPackets(nextPacket.creationEpoch, nextPacket.creationSequence);
						break;
					}
					
					add(nextPacket, epochProcessedShapes);							
					current.pending.remove(nextPacket);
					currentPacket = nextPacket;
					nextPacket = current.pending.peek();
				}												
			}
		}

		protected void processSequenceRequest(ShapePacket packet, Epoch current) {
			if (current.processedShapes.contains(packet))
			{
				int i = current.processedShapes.indexOf(packet);
				forNetwork.add(current.processedShapes.get(i).packet);
			}
			else if (current.pending.contains(packet))
			{
				for (ShapePacket p : current.pending)
				{
					if ( p.equals(packet))
					{
						forNetwork.add(p.packet);
					}
				}
			}
		}

		private void add(ShapePacket packet, LinkedList<ShapePacket> epochProcessedShapes) {
			switch (packet.type)
			{
			case OBJECT_CREATION:
			case OBJECT_GEOM_MODIFICATION:
			case OBJECT_ATTIBUTE_MODIFICATION:
			case OBJECT_ATTRIBUTE_DELETION:
			case OBJECT_DELETION :
				if (!packet.userCreated) {
					forLocalShape.add(packet);
				}
				epochProcessedShapes.add(packet);
				break;
			case NULL_REQUEST:
				epochProcessedShapes.add(packet);
				break;
			default:
				System.err.println("TRANSMAN: SequenceTracker: Improper shapePacket added. New type perhaps?");
			}
		}

		/**
		 * 
		 * @param epoch the current epoch
		 * @param uptoSequence how far to seek. 
		 * This function seeks (processedShapes.last,uptoSequence)
		 */
		protected synchronized void seekMissingPackets(Integer epoch, Integer uptoSequence) 
		{
			Epoch current = epochs.get(epoch);
			int gapStart;
			if ( current.processedShapes.isEmpty())
			{
				gapStart = startSequence;
			}
			else
			{
				 gapStart = current.processedShapes.getLast().sequence+1;
			}
			HashMap<Integer, MissingSequenceTracker> map = missingPacketFinders.get(epoch);

			/* construct the list of sequence numbers in the gap. This will usually be quite small */
			if(uptoSequence-gapStart < 0) {
				//already have everything up to the gap, so don't do any seeking
				return;
			}
			List<Integer> interval = new ArrayList<Integer>(uptoSequence-gapStart);
			for (int i=gapStart ; i < uptoSequence; ++i)
			{
				interval.add(i);
			}
			
			/* remove deadnumbers or numbers we're already seeking on,
			 *  since we aren't going to seek on them.
			 */ 
			for (int i=interval.size()-1; i >= 0 ; --i)
			{
				if (map.containsKey(interval.get(i)))
				{
					interval.remove(i); // by index
				}
				
			}
			
			/* remove pending packets, since we've already found them. */
			for ( ShapePacket p : current.pending)
			{
				if (interval.contains(p.sequence))
				{
					interval.remove(p.sequence); // by object
				}
			}
			
			/* for every number left, start a seek thread */
			for (Integer i : interval)
			{
				MissingSequenceTracker find = new MissingSequenceTracker(current, i, venusProtocol );					
				map.put(i, find);
				find.start();
			}
		}
	}

	/***
	 * 
	 * @author Afton
	 * This class is used to launch a thread that tries to find a missing sequence number
	 * and eventually marks that number as a dead number. 
	 *
	 */
	protected class MissingSequenceTracker extends Thread {
		private List<WhiteboardPeer> peers = null;
		private byte[] missingPacket;
		private Epoch current;
		private Integer sequence;
		private final int minNumRetries = 5; 
		
		public MissingSequenceTracker(Epoch current, Integer sequence, VenusProtocol localNode)
		{
			peers = localNode.getPeers();
			this.current = current;
			this.sequence = sequence;
			
		}

		public void run() {
			try {				
				missingPacket = createMissingSequenceRequest(current.epoch, sequence);
				sleep(timeout_milliseconds + (int)((2*Math.random()-1)*timeout_range));
				
				/**
				 * randomize the list of peers, removing the current user from that list
				 * then send messages to the remaining peers one at a time
				 */
				WhiteboardPeer me = canvas.getWindow().getConfig().getUserPeer();
				List<WhiteboardPeer> randomPeers = new LinkedList<WhiteboardPeer>(peers);
				Collections.shuffle(randomPeers);		
				randomPeers.remove(me);
				int timesTried = 0;
				while(timesTried < minNumRetries && randomPeers.size() > 0) {
					for(int i = 0; i < randomPeers.size(); ++i) {
						WhiteboardPeer peer = randomPeers.get(i);
						if ( peer.isConnected() )
						{
							try{
								peer.send(missingPacket);
								++timesTried;
							}
							catch (IOException e) 
							{
								randomPeers.remove(peer);
								// swallow. move on to the next peer
							}
							sleep(timeout_milliseconds + (int)((2*Math.random()-1)*timeout_range));
						}
					}
				}
				
				declareDeadPacket();
			} catch (InterruptedException e) {
				// we must have been found; 
				return;
			}
		}

		private void declareDeadPacket() {
			// if we haven't been stopped yet, we ought to add
			// this number to the dead list. 
			current.pending.add(new ShapePacket(new byte[0], WB_REQUEST_TYPE.NULL_REQUEST, current.epoch, sequence));
			sequenceTracker.checkPendingPackets(current, current.processedShapes);
		}

		private byte[] createMissingSequenceRequest(int epoch, int sequenceNum)
		{
			byte[] packet = new byte[ShapeConstants.PACKET_BASE_HEADER_OFFSET];
			packet[0] = new Integer(ShapeConstants.WB_REQUEST_TYPE.EPOCH_SEQUENCE_REQUEST.ordinal()).byteValue();
			BytePacker.insertInto_byte_Array(BytePacker.convertIntToBytes(epoch), 
					 						 packet, 
					 						 ShapeConstants.PACKET_EPOCH_OFFSET);
			BytePacker.insertInto_byte_Array(BytePacker.convertIntToBytes(sequenceNum), 
											 packet, 
											 ShapeConstants.PACKET_SEQUENCE_NUMBER_OFFSET);		
			return packet;
		}
	}
	
	/***
	 * @param election : update the current election process.
	 */
	public void setElection(boolean election)
	{
		if (election != electionInProgress)
		{
			electionInProgress = election;
			if (!electionInProgress) // if an election just ended
			{
				netBufferSender.interrupt();
			}
		}
	}

	protected class NetworkBuffer extends Thread {
		
		private ConcurrentLinkedQueue<Pair<byte[], ShapeConstants.WB_REQUEST_TYPE>> buffer = 
			new ConcurrentLinkedQueue<Pair<byte[], ShapeConstants.WB_REQUEST_TYPE>>();
				
		public boolean isStopped = false;
		
		public NetworkBuffer(String name)
		{
			super(name);
		}
		
		public void add(Pair<byte[], WB_REQUEST_TYPE> p) {
			buffer.add(p);
			this.interrupt();
		}

		/***
		 * This method causes the thread to finish normally
		 *
		 */
		public void stop_processing()
		{
			// by interrupting itself, the thread should wake up from any sleep
			// and then setting isStopped to true will cause it to exit. 
			this.isStopped = true;
			this.interrupt();
		}
		
		public void run()
		{
			while (!isStopped)
			{
				try
				{
					/**
					 * As long as the buffer is empty, just sleep. 
					 */
					while (buffer.isEmpty())
						sleep(10000000);
					
					while(!buffer.isEmpty())
					{
						try
						{
							boolean sent = sendShapeToAll(buffer.peek());
							if (sent)
							{
								buffer.remove();
							}
						} catch (IllegalPacketSizeException e) {
							System.err.println("TRANSMAN: Illegal Packet. Not sending. In a perfect world we'd mark this sequence number dead"); 
						}
					}
				}
				catch (InterruptedException e)
				{
					// swallow and continue
				}
			}
			return;
		}
	}

	public int getEpoch() {
		return sequenceServer.getEpochNum();
	}
	
	public void setEpoch(int epoch) {
		sequenceServer.setEpochNum(epoch);
	}	
}