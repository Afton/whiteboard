/**
 * File: AboutDialog.java
 * Author: Kyle Porter
 * Date: Oct 6th, 2006
 */

package whiteboard.gui.dialogs;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This class is a dialog for displaying information about the application.
 */
@SuppressWarnings("serial")
public class AboutDialog extends BasicDialog {

	/** 
	 * constructor
	 * @param parent - the parent frame of this dialog 
	 */
	public AboutDialog(JFrame parent) {
		super(parent, "About", true, 2);

		initializeLayout();

		packCenterOpen();
	}

	/** create and set the layout for the dialog */
	private void initializeLayout() {
		this.setResizable(false);

		//set to have a grid layout
		JPanel mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		mainPanel.setLayout(new GridLayout(5, 1));

		//add the program title
		JLabel label = new JLabel(((BasicFrame) getParent()).getProgramTitle());
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(((BasicFrame) getParent()).getConfig().getFont());
		mainPanel.add(label);
		
		//add the program version
		label = new JLabel("Version: " + ((BasicFrame) getParent()).getVersion());
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(((BasicFrame) getParent()).getConfig().getFont());
		mainPanel.add(label);
		
		//add the program version date
		label = new JLabel("Last Modified: " + ((BasicFrame) getParent()).getVersionDate());
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(((BasicFrame) getParent()).getConfig().getFont());
		mainPanel.add(label);
		
		//add the program authors
		label = new JLabel("Authors: " + ((BasicFrame) getParent()).getAuthors());
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(((BasicFrame) getParent()).getConfig().getFont());
		mainPanel.add(label);

		//add the ok button
		JPanel panel = new JPanel();
		JButton buttonOk = new JButton("OK");
		buttonOk.setFont(((BasicFrame) getParent()).getConfig().getFont());
		buttonOk.addActionListener(this);
		panel.add(buttonOk);
		mainPanel.add(panel);

		this.getContentPane().add(mainPanel);
	}
	
	protected boolean actionButton() {
		return true;
	}
}