/**
 * File: MessageDialog.java
 * Author: Kyle Porter
 * Date: Sept 26th, 2006
 */

package whiteboard.gui.dialogs;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This class is a dialog for displaying any normal GUI message.
 */
@SuppressWarnings("serial")
public class MessageDialog extends BasicDialog {
	/** 
	 * constructor
	 * @param parent - the parent frame of this dialog 
	 * @param title - the title for the dialog
	 * @param msg - the message to be displayed in the dialog
	 */
	public MessageDialog(JFrame parent, String title, String msg) {
		super(parent, title, true, 2);

		initializeLayout(msg);

		packCenterOpen();
	}

	/**
	 * create and set the layout for the dialog 
	 * @param msg - the message to be displayed in the dialog
	 */
	private void initializeLayout(String msg) {
		//set to have a grid layout
		JPanel mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		mainPanel.setLayout(new GridLayout(2, 1));

		//add the message
		JLabel label = new JLabel(msg);
		label.setFont(((BasicFrame) getParent()).getConfig().getFont());
		mainPanel.add(label);

		//add the ok button
		JPanel panel = new JPanel();
		JButton buttonOk = new JButton("OK");
		buttonOk.setFont(((BasicFrame) getParent()).getConfig().getFont());
		buttonOk.addActionListener(this);
		this.getRootPane().setDefaultButton(buttonOk);
		panel.add(buttonOk);
		mainPanel.add(panel);

		this.getContentPane().add(mainPanel);
	}
	
	protected boolean actionButton() {
		return true;
	}
}