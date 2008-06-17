/**
 * 
 */
package whiteboard.networking.eris;

import whiteboard.core.CloseableThread;

/**
 * @author patrick
 *
 */
public abstract class SequenceServer extends CloseableThread {
	private int epochNum;
	
	public SequenceServer(String name) {
		super(name);
	}
	
	public abstract int getSequenceNum();
	public abstract int peekSequenceNum();
	
	@Override
	public abstract void close();

	/**
	 * @return
	 */
	public int getEpochNum() {
		return epochNum;
	}
	
	public void setEpochNum(int epochNum) {
		this.epochNum = epochNum;
	}
}
