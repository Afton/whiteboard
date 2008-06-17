/**
 * File: ConnectMenuBar.java
 * Author: Kyle Porter
 * Date: Sept 26th, 2006
 */

package whiteboard.gui.startlist;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import whiteboard.gui.dialogs.AboutDialog;
import whiteboard.gui.dialogs.ConnectionDialog;
import whiteboard.gui.dialogs.FontDialog;
import whiteboard.gui.dialogs.PreferencesDialog;

/**
 * This class represents the toolbar that the whiteboard connection list uses.
 */
@SuppressWarnings("serial")
public class ConnectMenuBar extends JMenuBar implements ActionListener {
	/** reference to ConnectWindow that has this toolbar */
	private ConnectWindow window;
	
	/**
	 * constructor
	 * @param window - the window that holds this toolbar
	 */
	public ConnectMenuBar(ConnectWindow window) {
		this.window = window;
		add(createFileMenu());
		add(createOptionsMenu());
		add(createHelpMenu());
	}

	/** create the file menu of the menubar */
	protected JMenu createFileMenu() {
		JMenu menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		
		JMenuItem mItem = new JMenuItem("Connect...");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		mItem.setMnemonic(KeyEvent.VK_C);
		menu.add(mItem);
		
		mItem = new JMenuItem("Disconnect");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		mItem.setMnemonic(KeyEvent.VK_D);
		menu.add(mItem);
		
		menu.addSeparator();

		mItem = new JMenuItem("Create Whiteboard...");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		mItem.setMnemonic(KeyEvent.VK_W);
		menu.add(mItem);
		
		menu.addSeparator();
		
		mItem = new JMenuItem("Exit");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		mItem.setMnemonic(KeyEvent.VK_X);
		menu.add(mItem);

		return menu;
	}

	/** create the options menu of the menubar */
	protected JMenu createOptionsMenu() {
		JMenu menu = new JMenu("Options");
		menu.setMnemonic(KeyEvent.VK_O);

		JMenuItem mItem = new JMenuItem("Change Font Properties...");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		mItem.setMnemonic(KeyEvent.VK_F);
		menu.add(mItem);
		
		mItem = new JMenuItem("Change Preferences...");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		mItem.setMnemonic(KeyEvent.VK_P);
		menu.add(mItem);

		return menu;
	}

	/** create the help menu of the menubar */
	protected JMenu createHelpMenu() {
		JMenu menu = new JMenu("Help");
		menu.setMnemonic(KeyEvent.VK_H);

		JMenuItem mItem = new JMenuItem("About");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		mItem.setMnemonic(KeyEvent.VK_A);
		menu.add(mItem);

		return menu;
	}

	/**
	 * set connect/disconnect to enabled/disabled (only one can be enabled at any time)
	 * @param isConnected - true if connect to be enabled/disconnect disabled
	 */
	protected void setConnectedMenuItems(boolean isConnected) {
		this.getMenu(0).getMenuComponent(0).setEnabled(!isConnected);
		this.getMenu(0).getMenuComponent(1).setEnabled(isConnected);
	}

	/** disable the connect/disconnect menu items */
	protected void disableConnectMenuItems() {
		this.getMenu(0).getMenuComponent(0).setEnabled(false);
		this.getMenu(0).getMenuComponent(1).setEnabled(false);
	}

	/**
	 * called when an action is triggered
	 * @param action - the action that triggered this method call
	 */
	public void actionPerformed(ActionEvent action) {
		String arg = action.getActionCommand().trim();
		if(arg.equals("Exit")) {
			//call exit
			window.safeExit();
		} else if(arg.equals("Connect...")) {
			//show connection dialog
			new ConnectionDialog(window);
		} else if(arg.equals("Disconnect")) {
			//disconnect from network
			window.disconnect();
		} else if(arg.equals("Create Whiteboard...")) {
			// Create a new whiteboard
			window.openCreateDialog();
		} else if(arg.equals("Change Font Properties...")) {
			//call change font dialog
			new FontDialog(window);
		} else if(arg.equals("Change Preferences...")) {
			//create new preferences dialog
			new PreferencesDialog(window).open();
		} else if(arg.equals("About")) {
			//show about dialog
			new AboutDialog(window);
		}
	}
}