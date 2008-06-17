/**
 * File: ConnectList.java
 * Author: Kyle Porter
 * Date: Sept 26th, 2006
 */

package whiteboard.gui.startlist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import whiteboard.core.WhiteboardCore;
import whiteboard.gui.GuiUtilities;
import whiteboard.networking.mars.MarsProtocol;

/**
 * This class is the list of whiteboards initially available for a user to
 * connect to.
 */
@SuppressWarnings("serial")
public class ConnectList extends JList {
	// LIST ICONS
	protected ImageIcon listLockedIcon;
	protected ImageIcon listUnlockedIcon;
	protected ImageIcon listInviteIcon;

	/** the window the list is in */
	private ConnectWindow window;

	/**
	 * constructor
	 * @param window - the window the list is in
	 */
	public ConnectList(ConnectWindow window) {
		this.window = window;
		initializeVariables();
	}

	/** initialize any variables that are needed on startup */
	private void initializeVariables() {
		// initialize the icons
		listLockedIcon = GuiUtilities.createImageIcon("list_locked.gif");
		listUnlockedIcon = GuiUtilities.createImageIcon("list_unlocked.gif");
		listInviteIcon = GuiUtilities.createImageIcon("list_invite.gif");

		// set the model for the list
		MarsProtocol model = new MarsProtocol();
		this.setModel(model);

		// set the selection mode
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// add the cell renderer to show the little lock icons
		this.setCellRenderer(new ListRenderer());

		// add the mouse click listener
		this.addMouseListener(new MouseAdapter() {
			@Override
			@SuppressWarnings("synthetic-access")
			public void mouseClicked(MouseEvent e) {
				// detects double clicks and launches a whiteboard
				if (e.getClickCount() == 2) {
					window.launchWhiteboard();
				}
			}
		});
	}

	/**
	 * change the font and propagate to sub windows
	 * @param font - the font to change to
	 */
	public void updateFont(Font font) {
		setFont(font);
	}

	/**
	 * This class renders the whiteboard connection list, adding the icons with text.
	 */
	@SuppressWarnings("serial")
	private class ListRenderer extends JLabel implements ListCellRenderer {
		/** constructor */
		public ListRenderer() {
			setOpaque(true);
			setVerticalAlignment(SwingConstants.CENTER);
		}

		/**
		 * This method finds the image and text corresponding to the selected
		 * value and returns the label, set up to display the text and image.
		 */
		public Component getListCellRendererComponent(JList list, Object value, @SuppressWarnings("unused") int index, boolean isSelected, @SuppressWarnings("unused") boolean cellHasFocus) {
			// eventually there will be some object here which will allow
			// detection of status
			setFont(list.getFont());
			
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}

			setText(value.toString());

			if (value instanceof WhiteboardCore) {
				WhiteboardCore whiteboard = (WhiteboardCore) value;
				
				switch(whiteboard.getPermissionLevel()) {
					case UNLOCKED:
						setIcon(listUnlockedIcon);
						break;
					case LOCKED:
						setIcon(listLockedIcon);
						break;
					case INVITE:
						setForeground(Color.GRAY);
						setIcon(listInviteIcon);
						break;
					default:
						break;
				}
			}

			return this;
		}
	}
}