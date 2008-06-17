/**
 * File: BasicDialog.java
 * Author: Kyle Porter
 * Date: Sept 25th, 2006
 */

package whiteboard.gui.dialogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;

/** 
 * An abstract class containing a few basic methods for dialogs.
 */
public abstract class BasicDialog extends JDialog implements ActionListener {
	protected GridBagConstraints gbc = null;
	protected GridBagLayout gbl = null;

	/** represents size of insets */
	private int insetSize;
	/** string for button action, ignores case (defaults to "OK") */
	private String buttonString = "OK";
	
	/**
	 * basic constructor
	 * @param parent - parent of this dialog
	 * @param title - title of the dialog
	 * @param isModal - is the dialog a modal dialog or not
	 * @param insetSize - size of space around edges of dialog
	 */
	public BasicDialog(JFrame parent, String title, boolean isModal, int insetSize) {
		super(parent, title, isModal);
		setInsetSize(insetSize);
	}
	
	/**
	 * basic constructor
	 * @param parent - parent of this dialog
	 * @param isModal - is the dialog a modal dialog or not
	 * @param insetSize - size of space around edges of dialog
	 */
	public BasicDialog(JFrame parent, boolean isModal, int insetSize) {
		super(parent, isModal);
		setInsetSize(insetSize);
	}
	
	/**
	 * basic constructor
	 * @param parent - parent of this dialog
	 * @param title - title of the dialog
	 * @param isModal - is the dialog a modal dialog or not
	 */
	public BasicDialog(JFrame parent, String title, boolean isModal) {
		super(parent, title, isModal);
		setInsetSize(0);
	}
	
	/**
	 * basic constructor
	 * @param parent - parent of this dialog
	 * @param isModal - is the dialog a modal dialog or not
	 */
	public BasicDialog(JFrame parent, boolean isModal) {
		super(parent, isModal);
		setInsetSize(0);
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

	/** this method calls setVisible(false) and dispose to close window */
	protected void close() {
		setVisible(false);
		dispose();
	}
	
	/** 
	 * sets the inset size to number >= 0
	 * @param size - size of inset
	 */
	private final void setInsetSize(int size) {
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
	
	/** 
	 * adjusts dialog to make sure that it stays on the screen
	 * @param loc - the location to move the dialog to 
	 */
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
	
	/** 
	 * sets the string to use for the "OK" command (defaults to OK)
	 * @param okString - the string to use for the actionCommand
	 */
	protected final void setButtonString(String buttonString) {
		this.buttonString = buttonString;
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
	
	/** basic actionPerformed, triggers actionButton when button pressed */
	public void actionPerformed(ActionEvent e){
		if (e.getActionCommand().trim().equalsIgnoreCase(buttonString) && actionButton()) {
			close();
		}
	}
	
	/** method to be performed when OK is triggered by actionEvent */
	protected abstract boolean actionButton();
}