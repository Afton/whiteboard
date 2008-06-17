/**
 * File: BasicFrame.java
 * Author: Kyle Porter
 * Date: Sept 26th, 2006
 */

package whiteboard.gui.dialogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JComponent;
import javax.swing.JFrame;

import whiteboard.core.Configuration;

/** 
 * An abstract class containing a few basic methods for frames.
 */
public abstract class BasicFrame extends JFrame {
	protected GridBagConstraints gbc = null;
	protected GridBagLayout gbl = null;

	/** the version of the program */
	private String version;
	/** the date of the last version */
	private String versionDate;
	/** the title of this program */
	private String programTitle;
	/** the authors */
	protected final static String authors = "Kyle Porter, Afton Lewis & Patrick Colp";

	/** represents size of insets */
	private int insetSize;
	
	/** settings of the window (connectionWindow or whiteboardWindow) */
	private Configuration config;

	/** constructor */
	public BasicFrame() {
		super();
		initialize(0);
	}
	
	/**
	 * constructor
	 * @param insetSize - size of the insets for this frame
	 */
	public BasicFrame(int insetSize) {
		super();
		initialize(insetSize);
	}
	
	/**
	 * constructor
	 * @param title - title for this frame
	 */
	public BasicFrame(String title) {
		super(title);
		initialize(0);
	}
	
	/**
	 * constructor
	 * @param title - title for this frame
	 * @param insetSize - size of the insets for this frame
	 */
	public BasicFrame(String title, int insetSize) {
		super(title);
		initialize(insetSize);
	}
	
	private void initialize(int iSize) {
		config = new Configuration();
		setInsetSize(iSize);
		initializeProgramVariables();
	}

	/** set the program title, version and version date in this method */
	protected abstract void initializeProgramVariables();

	protected String getProgramTitle() {
		return programTitle;
	}
	
	protected void setProgramTitle(String title) {
		programTitle = title;
	}
	
	protected String getVersion() {
		return version;
	}
	
	protected void setVersion(String version) {
		this.version = version;
	}
	
	protected String getVersionDate() {
		return versionDate;
	}
	
	protected void setVersionDate(String date) {
		versionDate = date;
	}
	
	protected String getAuthors() {
		return authors;
	}
	
	/** this method packs, centers and then sets the dialog to visible */
	protected final void packCenterOpen() {
		pack();
		centerWindow();
		setVisible(true);
	}
	
	/**
	 * this method sets the size to the given dimensions, then centers and sets to visible
	 * @param width - the width of the dialog
	 * @param height - the height of the dialog
	 */
	protected final void centerOpen(int width, int height) {
		setSize(width, height);
		centerWindow();
		setVisible(true);
	}

	/**
	 * open this frame with given height and width at center of screen
	 * @param height - the height of the frame
	 * @param width - the width of the frame
	 * @param show - show the frame or not
	 */
	protected void open(int width, int height, boolean visible) {
		setSize(width, height);
		centerWindow();
		setVisible(visible);
	}

	/**
	 * open this frame with given height and width at center of screen
	 * @param height - the height of the frame
	 * @param width - the width of the frame
	 */
	protected void open(int width, int height) {
		open(width, height, true);
	}
	
	/** this method calls setVisible(false) and dispose to close window */
	protected void close() {
		setVisible(false);
		dispose();
	}

	/** returns the configuration item  for this window */
	public final Configuration getConfig() {
		return config;
	}
	
	/**
	 * set the configuration item for this window
	 * @param config - the config item to set for this window
	 */
	public final void setConfig(Configuration config) {
		this.config = config;
	}
	
	/** 
	 * sets the inset size to number >= 0
	 * @param size - size of inset
	 */
	private void setInsetSize(int size) {
		if(insetSize < 0)
			this.insetSize = 0;
		else
			this.insetSize = size;
	}
	
	/** centers the window in the screen */
	protected final void centerWindow() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Point centerPosition = new Point(screenSize.width / 2, screenSize.height / 2);
		Dimension windowSize = getSize();
		setLocation(centerPosition.x - windowSize.width / 2, centerPosition.y - windowSize.height / 2);
	}
	
	/** adjusts frame to make sure that it stays on the screen */
	protected final void ensureOnScreen(Point loc) {
		if(loc != null){
			int x = loc.x; 
			int y = loc.y;
			//if dialog will go off the screen, adjust the position so it remains
			//onscreen with a small padding near the edge
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			if(x + getSize().width > screenSize.width){
				x = screenSize.width - getSize().width;
			}
			if(y + getSize().height > screenSize.height){
				y = screenSize.height - getSize().height - 60;
			}

			setLocation(x, y);
		} else { 
			centerWindow();
		}
	}
	
	/** Adds a component to the gridbag. */
	protected final void addComponent(JComponent c, Container p, int row, int column, int width, int height, double wx, double wy) {
		if(( gbc == null) || (gbl == null))
			return;
		// set gridx and gridy
		gbc.gridx = column;
		gbc.gridy = row;

		// set gridwidth and gridheight
		gbc.gridwidth = width;
		gbc.gridheight = height;

		// set weights
		gbc.weightx = wx;
		gbc.weighty = wy;

		gbl.setConstraints(c, gbc);
		p.add(c);
	}
	
	/** allow for spacing around edge of frame, overrides a super method */
	public final Insets getInsets() {
		Insets ins = (Insets) super.getInsets().clone();
		ins.left += insetSize;
		ins.right += insetSize;
		ins.bottom += insetSize;
		ins.top += insetSize;
		return ins;
	}
	
	/**
	 * displays a message with given text
	 * @param title - the title of the dialog box
	 * @param msg - the message to be shown in the box
	 */
	public final void showMessage(String title, String msg) {
		new MessageDialog(this, title, msg);
	}

	/** abstract class to update the font for the frame, and any subframes/dialogs */
	public abstract void updateFont(Font font);
}