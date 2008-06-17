package whiteboard.gui.whiteboard;

import whiteboard.core.transaction.ShapePacket;

/**
 * Thread for polling the queues to push data to the canvas, public and private chats.
 */
public class NetworkShapePollThread extends Thread {
	private Canvas canvas;
	private boolean isRunning = true;

	/**
	 * constructor
	 * @param canvas - canvas to update
	 */
	public NetworkShapePollThread(Canvas canvas) {
		super("NetworkShapePollThread");
		this.canvas = canvas;
	}

	public void run() {
		ShapePacket b;
		while(isRunning) {
			// poll the processQueue
			while((canvas.getTransactionManager() != null) && ((b = canvas.getTransactionManager().pullShapeFromNetwork()) != null)) {
				canvas.addPacket(b);
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
