/**
 * File: PasswordDialog.java
 * Author: Kyle Porter
 * Date: Sept 27th, 2006
 */

package whiteboard.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import whiteboard.gui.whiteboard.WhiteboardWindow;

/**
 * This class asks for the password to enter a whiteboard from the connectList.
 */
@SuppressWarnings("serial")
public class PasswordDialog extends BasicDialog {
	/** the field holding the password for the whiteboard */
	private JPasswordField passwordField;
	/** true if user cancelled dialog */
	private boolean isCancelled;
	/** the password */
	private String password = "";
	

	/** 
	 * constructor
	 * @param parent - the parent frame of this dialog 
	 */
	public PasswordDialog(WhiteboardWindow window) {
		super(window, "Please enter password", true);

		isCancelled = false;

		initializeLayout();
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(@SuppressWarnings("unused") WindowEvent e) {
				isCancelled = true;
				close();
			}
		});

		packCenterOpen();
	}

	/** create and set the layout for the dialog */
	private void initializeLayout() {
		//set to have a borderlayout
		this.getContentPane().setLayout(new BorderLayout());

		//add the message
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Please enter password:  ");
		label.setFont(((BasicFrame) getParent()).getConfig().getFont());
		panel.add(label);
		
		//add the password form
		passwordField = new JPasswordField(15);
		panel.add(passwordField);
		this.getContentPane().add(panel, BorderLayout.CENTER);

		//add the ok & cancel buttons
		panel = new JPanel();
		JButton button = new JButton("OK");
		button.setFont(((BasicFrame) getParent()).getConfig().getFont());
		this.getRootPane().setDefaultButton(button);
		button.addActionListener(this);
		panel.add(button);
		
		button = new JButton("Cancel");
		button.setFont(((BasicFrame) getParent()).getConfig().getFont());
		button.addActionListener(this);
		panel.add(button);
		this.getContentPane().add(panel, BorderLayout.SOUTH);
	}

	public String getPassword() {
		return password;
	}

	/** action taken when OK button is pressed */
	@Override
	protected boolean actionButton() {
		if(passwordField.getPassword().length == 0) {
			((BasicFrame) getParent()).showMessage("Error", "Invalid empty password.");
			return false;
		}
		password = new String(passwordField.getPassword());
		return true;
	}

	/** action listener for buttons */
	@Override
	public void actionPerformed(ActionEvent e){
		if (e.getActionCommand().trim().equalsIgnoreCase("Cancel")) {
			isCancelled = true;
			close();
		} else {
			super.actionPerformed(e);
		}
	}

	public boolean isCancelled() {
		return isCancelled;
	}
}