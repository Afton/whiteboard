/**
 * 
 */
package whiteboard.core;

/**
 * @author patrick
 *
 */
public abstract class CloseableThread extends Thread {
	public CloseableThread(String name) {
		super(name);
	}
	
	public abstract void close(); 
}
