/**
 * File: UserListPanel.java
 * Author: Kyle Porter
 * Date: Sept 25th, 2006
 */

package whiteboard.gui.whiteboard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import whiteboard.core.entities.ShapeConstants;
import whiteboard.gui.GuiUtilities;
import whiteboard.gui.privatechat.PrivateChatWindow;
import whiteboard.networking.WhiteboardPeer;
import whiteboard.networking.venus.VenusProtocol;

/**
 * This class contains the user list panel for all users in this whiteboard.
 */
@SuppressWarnings("serial")
public class UserListPanel extends JPanel implements ActionListener {
	// LIST ICONS
	protected ImageIcon listOwnerIcon;
	protected ImageIcon listEditorIcon;
	protected ImageIcon listViewerIcon;
	
	/** list holding all the open private chats */
	private ArrayList<PrivateChatWindow> privateChatList;

	/** popup menu for user list */
	private JPopupMenu listPopup;
	
	/** the actual list with the clients in it */
	private JList userList;

	/** the window the whitboard belongs to */
	private WhiteboardWindow window;
	
	private VenusProtocol venusProtocol;

	/** constructor */
	public UserListPanel(WhiteboardWindow window, VenusProtocol venusProtocol) {
		//initialize any variables
		initializeVariables(window, venusProtocol);
		//create the user list
		initializeUserList();
		//set the correct fonts
		updateFont(window.getConfig().getFont());
		
		//set the layout for the panel
		initializeLayout();
	}

	/**
	 * the method that initializes any variables that need initialization on
	 * program start
	 */
	private void initializeVariables(WhiteboardWindow parentWindow, @SuppressWarnings("hiding") VenusProtocol venusProtocol) {
		this.window = parentWindow;
		this.venusProtocol = venusProtocol;

		// initialize the icons for the list
		listOwnerIcon = GuiUtilities.createImageIcon("user_owner.gif");
		listEditorIcon = GuiUtilities.createImageIcon("user_editor.gif");
		listViewerIcon = GuiUtilities.createImageIcon("user_viewer.gif");

		// create the list to hold all open private chats
		privateChatList = new ArrayList<PrivateChatWindow>();

		addListDataListener(new ListDataListener() {
			public void contentsChanged(@SuppressWarnings("unused") ListDataEvent arg0) {
				repaint();
			}

			public void intervalAdded(@SuppressWarnings("unused") ListDataEvent arg0) {}
			public void intervalRemoved(@SuppressWarnings("unused") ListDataEvent arg0) {}
		});
	}

	/** creates the popup menu for the user list */
	private void createPopupMenu() {
		// create the list popup menu
		listPopup = new JPopupMenu();
		listPopup.setBorder(BorderFactory.createTitledBorder("List Options"));

		JMenuItem mItem = new JMenuItem("Launch Chat...");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		listPopup.add(mItem);

		// if user is owner of the room, then can kick/ban users
		if(window.getConfig().getUserPeer().getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.OWNER)) {
			listPopup.addSeparator();

			if(((WhiteboardPeer) userList.getSelectedValue()).getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.EDITOR)) {
				mItem = new JMenuItem("Change User to Viewer");
			} else {
				mItem = new JMenuItem("Change User to Editor");
			}
			mItem.setActionCommand(mItem.getText());
			mItem.addActionListener(this);
			listPopup.add(mItem);

			listPopup.addSeparator();

			mItem = new JMenuItem("Kick");
			mItem.setActionCommand(mItem.getText());
			mItem.addActionListener(this);
			listPopup.add(mItem);

//			mItem = new JMenuItem("Ban");
//			mItem.setActionCommand(mItem.getText());
//			mItem.addActionListener(this);
//			listPopup.add(mItem);
		}
	}
	
	/** create and initialize the user list */
	private void initializeUserList() {
		// initialize the list		
		userList = new JList(venusProtocol);
		userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		userList.setCellRenderer(new UserListRenderer());
		userList.setSelectedIndex(-1);
		//add the current user
		addUser(window.getConfig().getUserPeer());
		
		// add the mouse click listener
		userList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// detects double clicks and launches a private chat
				if (e.getClickCount() == 2) {
					launchPrivateChat();
				}
			}
		});
		// add a listener for the popup menu
		userList.addMouseListener(new PopupListener());
	}

	/** create and set the layout for the panel */
	private void initializeLayout() {
		this.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(userList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(100, 1));
		this.add(scrollPane, BorderLayout.CENTER);
	}
	/**
	 * add a user to the user list
	 * @param userName - the name of the user
	 * @param userLevel - the level of the user
	 */
	protected void addUser(WhiteboardPeer user) {
		venusProtocol.addPeer(user);
	}
	
	/**
	 * remove a user from the list
	 * @param userName - the name of the user to remove
	 */
	protected void removeUser(WhiteboardPeer user) {
		venusProtocol.removePeer(user);
	}

	/**
	 * append the message to the private chat, else open a new one if no open chat
	 * @param client - the client the chat message is from
	 * @param message - the chat message
	 */
	public void appendPrivateTextMessage(WhiteboardPeer peer, String message) {
		PrivateChatWindow curWindow = null;
		for(int i = privateChatList.size()-1; i >= 0; --i) {
			curWindow = privateChatList.get(i);
			if(!curWindow.isVisible()) {
				privateChatList.remove(i);
			}
			if(curWindow.getWhiteboardPeer().equals(peer)) {
				if(!curWindow.isVisible())
					curWindow = null;
				break;
			}
		}
		if(curWindow == null) {
			curWindow = new PrivateChatWindow(this, peer, window.getConfig().getFont());
			privateChatList.add(curWindow);
		}
		curWindow.appendText(message);
	}
	
	/** launches a private chat with the selected client */
	protected void launchPrivateChat() {
		//ensure that something is selected
		if(!userList.isSelectionEmpty() && !window.getConfig().getUserPeer().equals(userList.getSelectedValue())) {
			privateChatList.add(new PrivateChatWindow(this, (WhiteboardPeer) userList.getSelectedValue(), window.getConfig().getFont()));
		}
	}
	
	/**
	 * removes a chat window from the open chat list
	 * @param window - the private chat window to remove
	 */
	public void removePrivateChat(PrivateChatWindow privWindow) {
		privateChatList.remove(privWindow);
	}

	/** close all the open private chats */
	protected void closeAllPrivateChats() {
		//move from end to front, as when you exit the chat gets removed from the list
		while(!privateChatList.isEmpty()) {
			privateChatList.get(0).safeExit();
		}
	}
	
	/**
	 * change the font and propagate to sub windows
	 * @param font - the font to change to
	 */
	public void updateFont(Font font) {
		userList.setFont(new Font(font.getFamily(), Font.BOLD, font.getSize()));
		Iterator<PrivateChatWindow> itr = privateChatList.iterator();
		while(itr.hasNext()) {
			itr.next().updateFont(font);
		}
	}
	
	/*
	 * ACTION LISTENER
	 */

	/**
	 * called when an action is triggered
	 * @param action - the action that triggered this method call
	 */
	public void actionPerformed(ActionEvent action) {
		String arg = action.getActionCommand().trim();
		if(arg.equals("Launch Chat...")) {
			launchPrivateChat();
		} else if(arg.equals("Kick")) {
			//kick the user
			kickPeer((WhiteboardPeer) userList.getSelectedValue());
		} else if(arg.equals("Ban")) {
			//ban NOT DONE: the user implement this stuff
		} else if(arg.equals("Change User to Editor")) {
			//change to editor
			changePeerPermLevel((WhiteboardPeer) userList.getSelectedValue(), WhiteboardPeer.PERM_LEVEL.EDITOR);
		} else if(arg.equals("Change User to Viewer")) {
			//change to viewer
			changePeerPermLevel((WhiteboardPeer) userList.getSelectedValue(), WhiteboardPeer.PERM_LEVEL.VIEWER);
		}
	}

	/**
	 * send a message to the peer to change their permission level
	 * @param peer - the peer to change
	 * @param permLevel - the permission level to change that user to
	 */
	public void changePeerPermLevel(WhiteboardPeer peer, WhiteboardPeer.PERM_LEVEL permLevel) {
		//create the change permission level packet
		String header = VenusProtocol.LINE_END 
						+ peer.getName() + VenusProtocol.LINE_END
						+ permLevel.ordinal() + VenusProtocol.LINE_END + VenusProtocol.LINE_END;
		byte[] headerPacket = new byte[header.getBytes().length + 1];
		headerPacket[0] = new Integer(ShapeConstants.WB_REQUEST_TYPE.WB_PERM_CHANGE.ordinal()).byteValue();
		for(int i = 1; i < headerPacket.length; ++i) {
			headerPacket[i] = header.getBytes()[i-1];
		}
		byte[] packet = VenusProtocol.appendVenusPacketHeader(header.getBytes());
		// Send this packed message to the network
		List<WhiteboardPeer> peers = window.getUserListPanel().getUsers();
		for (WhiteboardPeer p : peers) {
			// Send all other users (not this user)
			if (!p.equals(window.getConfig().getUserPeer())) {
				try {
					p.send(packet);
				} catch (IOException e) {
					window.showMessage("Error", "Could not send change permission level to " + p.getName());
				}
			}
		}
		peer.setPermissionLevel(permLevel);
		venusProtocol.sortPeers();
		this.repaint();
	}

	/**
	 * kick the given peer from the whiteboard (close all connections)
	 * @param peer
	 */
	private void kickPeer(WhiteboardPeer peer) {
		//create the kick packet
		String header = VenusProtocol.KICK + VenusProtocol.LINE_END 
						+ peer.getName() + VenusProtocol.LINE_END + VenusProtocol.LINE_END;
		byte[] packet = VenusProtocol.appendVenusPacketHeader(header.getBytes());
		// Send this packed chat message to the network
		List<WhiteboardPeer> peers = window.getUserListPanel().getUsers();
		for(int i = peers.size()-1; i >= 0; --i) {
			WhiteboardPeer p = peers.get(i);
			// Send all other users (not this user)
			if (!p.equals(window.getConfig().getUserPeer())) {
				try {
					p.send(packet);
				} catch (IOException e) {
					window.showMessage("Error", "Could not send kick message to " + p.getName());
				}
			}
		}
		this.repaint();
	}
	
	/**
	 * Class to render the names of the users in the list. exists to display different
	 * client levels in different colours
	 */
	protected class UserListRenderer extends JLabel implements ListCellRenderer {
		/** constructor */
		public UserListRenderer() {
	        setOpaque(true);
	        setBorder(new EmptyBorder(1, 1,1,1));
	    }

		/** method which displays the name of the variables */
	    public Component getListCellRendererComponent(JList list, Object value, @SuppressWarnings("unused") int index, boolean isSelected, @SuppressWarnings("unused") boolean cellHasFocus) {
	    	setFont(list.getFont());
	    	setText(((WhiteboardPeer) value).getName());
	        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
	        switch(((WhiteboardPeer) value).getPermissionLevel()) {
		        case OWNER:
		        	setIcon(listOwnerIcon);
		        	setForeground(Color.RED);
		        	break;
		        case EDITOR:
		        	setIcon(listEditorIcon);
		        	setForeground(Color.BLUE);
		        	break;
		        case VIEWER:
		        	setIcon(listViewerIcon);
		        	setForeground(Color.GRAY);
	        	default:
	        		
	        }
	        return this;
	    }
	}

	/**
	 * Class to listen for mouse events to show the popup menu on the user list.
	 * This was taken from the Java Menu tutorial (http://java.sun.com/docs/books/tutorial/uiswing/components/menu.html#popup) 
	 */
	protected class PopupListener extends MouseAdapter {

	    @Override
		@SuppressWarnings("synthetic-access")
		public void mouseReleased(MouseEvent e) {
	    	if (SwingUtilities.isRightMouseButton(e)) {
	    		//select the right clicked menu item
	        	userList.setSelectedIndex(userList.locationToIndex(e.getPoint()));
	        	//ensure popup doesn't appear when you right click on yourself
	        	if(!window.getConfig().getUserPeer().equals(userList.getSelectedValue()))
	        	{
	        		createPopupMenu();
	        		listPopup.show(e.getComponent(), e.getX(), e.getY());
	        	}
	        }
	    }
	}

	public WhiteboardWindow getWindow() {
		return window;
	}
	
	/**
	 * Get the list of all users in the list, including this user
	 * 
	 * @return list of all users, including this user
	 */
	public List<WhiteboardPeer> getUsers() {
		return venusProtocol.getPeers();
	}

	/**
	 * add a list data listener to the user list
	 * @param l - the list data listener to add
	 */
	public void addListDataListener(ListDataListener l) {
		venusProtocol.addListDataListener(l);
	}
}