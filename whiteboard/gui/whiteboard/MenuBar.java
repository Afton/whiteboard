/**
 * File: MenuBar.java
 * Author: Kyle Porter
 * Date: Sept 25th, 2006
 */

package whiteboard.gui.whiteboard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import whiteboard.gui.dialogs.AboutDialog;
import whiteboard.gui.dialogs.FontDialog;
import whiteboard.gui.dialogs.PreferencesDialog;
import whiteboard.networking.WhiteboardPeer;
import whiteboard.networking.venus.VenusClient.CONNECT_TYPE;

/**
 * This class represents the toolbar that the whiteboard application uses.
 */
@SuppressWarnings("serial")
public class MenuBar extends JMenuBar implements ActionListener {
	/** reference to mainWindow that has this toolbar */
	private WhiteboardWindow window;

	public MenuBar(WhiteboardWindow window) {
		this.window = window;
		add(createFileMenu());
		add(createEditMenu());
		add(createViewMenu());
		add(createHelpMenu());
	}

	/** create the file menu of the menubar */
	protected JMenu createFileMenu() {
		JMenu menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);

		JMenuItem mItem = new JMenuItem("Save...");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		mItem.setMnemonic(KeyEvent.VK_S);
		menu.add(mItem);

		mItem = new JMenuItem("Load...");
		mItem.setActionCommand(mItem.getText());
		mItem.setMnemonic(KeyEvent.VK_L);
		mItem.addActionListener(this);
		menu.add(mItem);
		
		menu.addSeparator();

		mItem = new JMenuItem("Print...");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		mItem.setMnemonic(KeyEvent.VK_P);
		menu.add(mItem);

		mItem = new JMenuItem("Exit");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		mItem.setMnemonic(KeyEvent.VK_X);
		menu.add(mItem);

		return menu;
	}
	
	protected JMenu createEditMenu() {
		JMenu menu = new JMenu("Edit");
		menu.setMnemonic(KeyEvent.VK_E);

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

	/** create the view menu of the menubar */
	protected JMenu createViewMenu() {
		JMenu menu = new JMenu("View");
		menu.setMnemonic(KeyEvent.VK_V);

		JMenuItem mItem = new JMenuItem("Autoscale");
		mItem.setActionCommand(mItem.getText());
		mItem.setMnemonic(KeyEvent.VK_A);
		mItem.addActionListener(this);
		menu.add(mItem);

		menu.addSeparator();

		JCheckBoxMenuItem checkItem = new JCheckBoxMenuItem("Enable Anti-Aliasing", false);
		checkItem.setMnemonic(KeyEvent.VK_E);
		checkItem.setDisplayedMnemonicIndex(7);
		checkItem.addActionListener(this);
		menu.add(checkItem);

		checkItem = new JCheckBoxMenuItem("Show Button Panel", true);
		checkItem.setMnemonic(KeyEvent.VK_B);
		checkItem.addActionListener(this);
		menu.add(checkItem);
		
		checkItem = new JCheckBoxMenuItem("Show Chat Panel", true);
		checkItem.setMnemonic(KeyEvent.VK_C);
		checkItem.addActionListener(this);
		menu.add(checkItem);

		checkItem = new JCheckBoxMenuItem("Show User List", true);
		checkItem.setMnemonic(KeyEvent.VK_U);
		checkItem.addActionListener(this);
		menu.add(checkItem);

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
	 * called when an action is triggered
	 * @param action - the action that triggered this method call
	 */
	public void actionPerformed(ActionEvent action) {
		String arg = action.getActionCommand().trim();
		if(arg.equals("Save...")) {
			//call save
			window.save();
		} else if(arg.equals("Load...")) {
			 if(window.getConfig().getUserPeer().getPermissionLevel() == WhiteboardPeer.PERM_LEVEL.VIEWER) {
				 window.showMessage("Error", "Viewers cannot load into whiteboards.");
			 } else {
				//call load
				window.load();
			 }
		} else if(arg.equals("Print...")) {
			//call print
			window.getCanvas().print();
		} else if(arg.equals("Exit")) {
			//call exit
			window.safeExit(CONNECT_TYPE.BAD, true);
		} else if(arg.equals("Change Font Properties...")) {
			//call change font dialog
			new FontDialog(window);
		} else if(arg.equals("Change Preferences...")) {
			//create new preferences dialog
			new PreferencesDialog(window).open();
		} else if(arg.equals("Autoscale")) {
			//call autoscale
			window.getCanvas().autoscale();
		} else if(arg.equals("Enable Anti-Aliasing")) {
			//toggle anti-aliasing
			window.getCanvas().setAntiAliasingEnabled(((JCheckBoxMenuItem) action.getSource()).isSelected());
		} else if(arg.equals("Show Button Panel")) {
			//toggle button panel
			window.setButtonPanelShowing(((JCheckBoxMenuItem) action.getSource()).isSelected());
		} else if(arg.equals("Show Chat Panel")) {
			//toggle chat panel
			window.setChatPanelShowing(((JCheckBoxMenuItem) action.getSource()).isSelected());
		} else if(arg.equals("Show User List")) {
			//toggle user list
			window.setUserListShowing(((JCheckBoxMenuItem) action.getSource()).isSelected());
		} else if(arg.equals("About")) {
			//show about dialog
			new AboutDialog(window);
		}
	}
}