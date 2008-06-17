package whiteboard.gui.whiteboard;

import whiteboard.core.Pair;
import whiteboard.core.entities.WB_Chat;
import whiteboard.networking.Peer;
import whiteboard.networking.WhiteboardPeer;

/**
 * Thread for polling the queues to push data to the canvas, public and private chats.
 */
public class NetworkChatPollThread extends Thread {
	private WhiteboardWindow window;
	private boolean isRunning = true;

	/**
	 * constructor
	 * @param window - window containing the panels to update for chat
	 */
	public NetworkChatPollThread(WhiteboardWindow window) {
		super("NetworkChatPollThread");
		this.window = window;
	}

	public void run() {
		Pair<Peer, WB_Chat> pair;
		while(isRunning) {
			// poll the processQueue
			while((window.getTransactionManager() != null) && ((pair = window.getTransactionManager().pullFromNetworkChat()) != null)) {
				switch(pair.getSecond().getChatType()) {
				case CHAT_PRIVATE:
					window.getUserListPanel().appendPrivateTextMessage((WhiteboardPeer) pair.getFirst(), pair.getSecond().getChatMsg());
					break;
				case CHAT_PUBLIC:
					window.getChatPanel().appendText((WhiteboardPeer) pair.getFirst(), pair.getSecond().getChatMsg());
					break;
				}
			}
			try {
				sleep(100);
			} catch (InterruptedException e) {}
		}
	}

	public void quit() {
		isRunning = false;
	}
}
