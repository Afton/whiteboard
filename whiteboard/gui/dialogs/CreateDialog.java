/**
 * 
 */
package whiteboard.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.BindException;
import java.net.ServerSocket;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import whiteboard.core.WhiteboardCore;
import whiteboard.core.exceptions.InvalidPasswordException;
import whiteboard.gui.startlist.ConnectWindow;
import whiteboard.networking.mars.MarsProtocol;
import whiteboard.networking.venus.VenusProtocol;

/**
 * @author patrick
 *
 */
@SuppressWarnings("serial")
public class CreateDialog extends BasicDialog {
	/** the textfield for the ip/hostname */
	private JTextField nameTextField;
	/** the textfield for the port number */
	private JTextField portTextField;
	/** combobox for what kind of whiteboard is being created (locked, unlocked, invite-only) */
	private JComboBox typeComboBox;
	/** textfield to store password */
	private JPasswordField passwordField;
	
	/** the window this dialog belongs to */
	private ConnectWindow window;
	/** true if the user cancelled the creation */
	private boolean isCancelled = true;

	/**
	 * constructor
	 * @param window - the window this dialog belongs to
	 */
	public CreateDialog(ConnectWindow window) {
		super(window, "Create Whiteboard", true);
		this.window = window;
		
		initializeLayout();
		
		super.packCenterOpen();
	}

	/** initialize and set the layout for the dialog */
	private void initializeLayout() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		//add the whiteboard name text field
		JPanel panel = new JPanel(new BorderLayout());
		nameTextField = new JTextField("Whiteboard");
		nameTextField.setColumns(13);
		nameTextField.setFont(this.getFont());
		nameTextField.addKeyListener(new ErrorKeyListener());
		JLabel label = new JLabel("Name: ");
		label.setFont(this.getFont());
		panel.add(label, BorderLayout.WEST);
		panel.add(nameTextField, BorderLayout.EAST);
		mainPanel.add(panel);
		
		//add the port text field
		panel = new JPanel(new BorderLayout());
		portTextField = new JTextField(Integer.toString(VenusProtocol.PORT));
		portTextField.setFont(this.getFont());
		portTextField.setColumns(13);
		portTextField.addKeyListener(new ErrorKeyListener());
		label = new JLabel("Port: ");
		label.setFont(this.getFont());
		panel.add(label, BorderLayout.WEST);
		panel.add(portTextField, BorderLayout.EAST);
		mainPanel.add(panel);
		
		//add the whiteboard permission type
		panel = new JPanel(new BorderLayout());
		String[] permissionLevel = {"Public", "Password Protected" };//, "Invite Only"};
		typeComboBox = new JComboBox(permissionLevel);
		typeComboBox.setPreferredSize(new Dimension(147, 23));
		typeComboBox.setFont(this.getFont());
		typeComboBox.addActionListener(this);
		label = new JLabel("Permission Level: ");
		label.setFont(this.getFont());
		panel.add(label, BorderLayout.WEST);
		panel.add(typeComboBox, BorderLayout.EAST);
		mainPanel.add(panel);

		//add the password text field
		panel = new JPanel(new BorderLayout());
		passwordField = new JPasswordField();
		passwordField.setFont(this.getFont());
		passwordField.setColumns(13);
		passwordField.addKeyListener(new ErrorKeyListener());
		passwordField.setEditable(false);
		label = new JLabel("Password: ");
		label.setFont(this.getFont());
		panel.add(label, BorderLayout.WEST);
		panel.add(passwordField, BorderLayout.EAST);
		mainPanel.add(panel);
		
		//add the buttons
		panel = new JPanel();
		JButton button = new JButton("OK");
		button.setFont(this.getFont());
		button.addActionListener(this);
		this.getRootPane().setDefaultButton(button);
		panel.add(button);
		
		button = new JButton("Cancel");
		button.setFont(this.getFont());
		button.addActionListener(this);
		panel.add(button);
		mainPanel.add(panel);
		
		this.getContentPane().add(mainPanel);
	}

	@Override
	protected boolean actionButton() {
		try {
			String name = nameTextField.getText().trim();
			//check to ensure it isn't a blank name
			if(name.equals("")) {
				throw new Exception("Invalid Whiteboard name.");
			}
			//do validation of port number input
			int portNum = Integer.parseInt(portTextField.getText().trim());
			
			if(portNum < 0 || portNum > Integer.MAX_VALUE) {
				throw new NumberFormatException();
			}
			//check to make sure the port isn't in use (by attempting to briefly open a socket on it)
			new ServerSocket(portNum).close();
			
			// confirm that the port isn't in use in our list of peers (warning: latency)
			if (!MarsProtocol.isPortFree(portNum))
			{
				throw new BindException();
			}
			
			String permString = typeComboBox.getSelectedItem().toString();
			WhiteboardCore.WB_PERM_LEVEL permLevel;
			if(permString.equals("Public")) {
				permLevel = WhiteboardCore.WB_PERM_LEVEL.UNLOCKED;
			} else if(permString.equals("Password Protected")) {
				permLevel = WhiteboardCore.WB_PERM_LEVEL.LOCKED;
				if(passwordField.getPassword().length == 0)
					throw new InvalidPasswordException("Invalid password.");
			} else {
				permLevel = WhiteboardCore.WB_PERM_LEVEL.INVITE;
			}

			// Create the whiteboard & notify peers
			WhiteboardCore core = new WhiteboardCore(name, permLevel, passwordField.getPassword(), window.getConfig().getUserPeer().getName(), window.getConfig().getUserPeer().getAddress()/*InetAddress.getLocalHost()*/, portNum);
			if(!window.launchWhiteboard(true, core)) {
				window.showMessage("Error", "Whiteboard name already exists.");
				ErrorKeyListener.setError(nameTextField);
				return false;
			}
		} catch(InvalidPasswordException ipe) {
			//show the error at the password field
			window.showMessage("Error", ipe.getLocalizedMessage());
			ErrorKeyListener.setError(passwordField);
			return false;
		} catch(NumberFormatException nfe) {
			//show the error at the port field
			window.showMessage("Error", "Invalid port number.");
			ErrorKeyListener.setError(portTextField);
			return false;
		} catch(BindException be) {
			//show the error at the port field
			window.showMessage("Error", "Port already in use.");
			ErrorKeyListener.setError(portTextField);
			return false;
		} catch(Exception e) {
			//show the error
			window.showMessage("Error", "CreateDialog - " + e.getLocalizedMessage());
			return false;
		}
		
		isCancelled = false;
		return true;
	}
	
	
	/** basic actionPerformed, handles cancel button */
	@Override
	public void actionPerformed(ActionEvent e){
		if (e.getActionCommand().trim().equalsIgnoreCase("Cancel")) {
			close();
		} else if(e.getSource() instanceof JComboBox) {
			passwordField.setEditable(((JComboBox) e.getSource()).getSelectedItem().toString().equals("Password Protected"));
		} else {
			super.actionPerformed(e);
		}
	}

	public boolean isCancelled() {
		return isCancelled;
	}
}