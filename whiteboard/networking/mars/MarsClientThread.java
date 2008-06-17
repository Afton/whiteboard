/**
 * 
 */
package whiteboard.networking.mars;

import java.io.IOException;
import java.util.TimerTask;

import whiteboard.gui.startlist.ConnectWindow;
import whiteboard.networking.Peer;
import whiteboard.networking.StreamPeer;

/**
 * @author patrick
 *
 */
public class MarsClientThread extends TimerTask {
	public static final long TIMEOUT = 0;

	private ConnectWindow window;
	private String name;
	private Peer peer;
	
	public MarsClientThread(ConnectWindow window, String name, String host) throws IOException {
		this(window, name, host, MarsProtocol.PORT);
	}

	public MarsClientThread(ConnectWindow window, String name, String host, int port) throws IOException {
		this(name, new StreamPeer(host, port));
		this.window = window;
	}
	
	public MarsClientThread(String name, Peer peer) {
		this.name = name;
		this.peer = peer;
	}

	@Override
	public void run() {
		try {
			// Set the name
			MarsClient.name = name;
			
			// Get update from peer
			if(peer != null) {
				MarsClient.getUpdateFrom(peer);
				window.setIsConnected(true);
			} else {
				this.cancel();
			}
		} catch (IOException ioe) {
			window.showMessage("Error", "Timed out trying to connect to host.");
			window.setIsConnected(false);
		}
	}
}
