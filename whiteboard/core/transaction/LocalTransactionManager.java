package whiteboard.core.transaction;

import whiteboard.core.Pair;
import whiteboard.core.entities.*;
import whiteboard.networking.Peer;

public interface LocalTransactionManager {
	
	public void pushToNetwork(byte[] b, ShapeConstants.WB_REQUEST_TYPE type );
	
	public ShapePacket pullShapeFromNetwork();

	public Pair<Peer, WB_Chat> pullFromNetworkChat();
}
