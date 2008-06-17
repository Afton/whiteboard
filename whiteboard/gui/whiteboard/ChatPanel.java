/**
 * File: ChatPanel.java
 * Author: Kyle Porter
 * Date: Sept 25th, 2006
 */

package whiteboard.gui.whiteboard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import whiteboard.core.entities.BytePacker;
import whiteboard.core.entities.ShapeConstants;
import whiteboard.core.entities.WB_Chat;
import whiteboard.networking.WhiteboardPeer;
import whiteboard.networking.venus.VenusProtocol;

@SuppressWarnings("serial")
public class ChatPanel extends JPanel implements KeyListener {
	/** text area for displaying all chat messages */
	private JTextArea chatDisplayTextArea;
	/** text field for entering user chat messages */
	private JTextField chatEnterTextField;

	/** list of recently entered items */
	private ArrayList<String> history;
	/** index into history for pressing up/down */
	private int historyIndex;
	/** the limit to the number of history items to store */
	private final int HISTORY_LIMIT = 50;
	/** string holding text that was erased when user pressed up to enter history */
	private String savedUserText;

	/** parent window of this panel */
	private WhiteboardWindow window;

	/** constructor */
	public ChatPanel(WhiteboardWindow window) {
		initializeLayout(window.getConfig().getFont());
		initializeVariables(window);
	}

	/** initialize any variables that need it */
	private void initializeVariables(WhiteboardWindow wbWindow) {
		this.window = wbWindow;
		history = new ArrayList<String>(5);
		historyIndex = 0;
		savedUserText = null;
	}
	
	/** create and set the layout of the chat panel */
	private void initializeLayout(Font font) {
		//create the area to display all chat msgs
		chatDisplayTextArea = new JTextArea();
		chatDisplayTextArea.setEditable(false);
		//set a small margin at left side of text area
		chatDisplayTextArea.setMargin(new Insets(0,10,0,0));
		//set lines to wrap in area, at word boundaries
		chatDisplayTextArea.setLineWrap(true);
		chatDisplayTextArea.setWrapStyleWord(true);
		chatDisplayTextArea.setFont(font);

		//create the text field for entering own msgs
		chatEnterTextField = new JTextField();
		chatEnterTextField.addKeyListener(this);
		chatEnterTextField.setFont(font);

		//create scrollpane to hold chat area, and add text field too
		this.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(chatDisplayTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(1, 125));
		add(scrollPane, BorderLayout.CENTER);
		add(chatEnterTextField, BorderLayout.SOUTH);
	}

	/**
	 * this method moves the item at history index to the last entry in the history
	 * @param histIndex - index of the item to place at the end of the history
	 */
	private void historyMoveToLast(int histIndex) {
		String copy;
		//shifts item at historyIndex to the end of the history
		for(int i = histIndex; i < history.size()-1; i++) {
			copy = history.get(i);
			history.set(i, history.get(i+1));
			history.set(i+1, copy);
		}
	}

	/** trims the history to a set number of items by removing the oldest items */
	private void historyTrim() {
		//while history is over limit
		while(history.size() > HISTORY_LIMIT) {
			//remove the oldest item in the history
			history.remove(0);
		}
	}
	
	/**
	 * change the font and propagate to sub windows
	 * @param font - the font to change to
	 */
	public void updateFont(Font font) {
		chatDisplayTextArea.setFont(font);
		chatEnterTextField.setFont(font);
	}

	/**
	 * add the given text to the chat display area
	 * @param text - the text to append to the chat
	 */
	public void appendText(WhiteboardPeer peer, String text) {
		chatDisplayTextArea.append(peer.getName() + "> ");
		chatDisplayTextArea.append(text);
		chatDisplayTextArea.append("\n");
		//scroll down
		chatDisplayTextArea.setCaretPosition(chatDisplayTextArea.getText().length());
	}
	
	/** pack up the msg into chat protocol and send to network */
	private void packAndSendMessage(String chatMsg) {
		//make sure it isn't sending more than Integer.MAX_VALUE per msg
		//send multiple msgs if more than that number of characters in the msg
		String text = chatMsg;
		while(!text.equals("")) {
			WB_Chat msg = new WB_Chat(text.substring(0, Math.min(text.length(), Integer.MAX_VALUE)), ShapeConstants.CHAT_TYPE.CHAT_PUBLIC);
			text = text.substring(Math.min(text.length(), Integer.MAX_VALUE));
			
			// Send this packed chat message to the network
			List<WhiteboardPeer> peers = window.getUserListPanel().getUsers();

			for (WhiteboardPeer peer : peers) {
				// Send all other users (not this user)
				if (!peer.equals(window.getConfig().getUserPeer())) {
					try {
						peer.send(VenusProtocol.appendVenusPacketHeader(BytePacker.convertTo_byte_Array(msg.pack())));
					} catch (IOException e) {
						chatDisplayTextArea.append("** Error sending " + msg.getChatMsg() + " to " + peer.getName());
					}
				}
			}
		}
	}

	public void keyPressed(KeyEvent arg0) {
		switch(arg0.getKeyCode()) {
			case KeyEvent.VK_ENTER:
				//only action here is enter being pressed on chatEnterTextField
				chatDisplayTextArea.append(window.getConfig().getUserPeer().getName() + "> " + chatEnterTextField.getText() + "\n");
				//pack up and send the text
				packAndSendMessage(chatEnterTextField.getText());
				//scroll down
				chatDisplayTextArea.setCaretPosition(chatDisplayTextArea.getText().length());
				//add to the history
				if((historyIndex == history.size()) || !history.get(historyIndex).equals(chatEnterTextField.getText())) {
					//new entry
					history.add(chatEnterTextField.getText());
					//trim the history to keep it from becoming too big
					historyTrim();
				} else {
					//old entry, move to the last position in the history
					historyMoveToLast(historyIndex);
				}
				//clear any saved user text
				savedUserText = null;
				//update the history index
				historyIndex = history.size();
				//clear any entered text
				chatEnterTextField.setText("");
				break;
			case KeyEvent.VK_UP:
				if(historyIndex > 0) {
					if(savedUserText == null) {
						//save the text the user was typing out
						savedUserText = chatEnterTextField.getText();
					}
					//move to next oldest item in history
					chatEnterTextField.setText(history.get(--historyIndex));
				}
				break;
			case KeyEvent.VK_DOWN:
				if(historyIndex < history.size()-1) {
					//move to next item in history
					chatEnterTextField.setText(history.get(++historyIndex));
				} else if(savedUserText != null) {
					//move to end of history
					historyIndex++;
					//set the chat field to the saved user text
					chatEnterTextField.setText(savedUserText);
					//reset the saved user text
					savedUserText = null;
				}
				break;
		}
	}
	public void keyReleased(@SuppressWarnings("unused") KeyEvent arg0) {}
	public void keyTyped(@SuppressWarnings("unused") KeyEvent arg0) {}
}