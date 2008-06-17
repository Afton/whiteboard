/**
 * File: ConnectionDialog.java
 * Author: Kyle Porter
 * Date: Oct 1st, 2006
 */

package whiteboard.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import whiteboard.gui.startlist.ConnectWindow;
import whiteboard.networking.mars.MarsProtocol;

/**
 * This class is used to connect to a whiteboard network.
 */
@SuppressWarnings("serial")
public class ConnectionDialog extends BasicDialog {
	/** the textfield for the ip/hostname */
	private JTextField ipTextField;
	/** the textfield for the port number */
	private JTextField portTextField;
	
	/** the window this dialog belongs to */
	private ConnectWindow window;

	/**
	 * constructor
	 * @param window - the window this dialog belongs to
	 */
	public ConnectionDialog(ConnectWindow window) {
		super(window, true);
		setTitle("Connect To Network");
		this.window = window;
		
		initializeLayout();
		initializeVariables();
		
		super.packCenterOpen();
	}

	/** initialize and set the layout for the dialog */
	private void initializeLayout() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		//add the hostname/ip text field
		JPanel panel = new JPanel(new BorderLayout());
		ipTextField = new JTextField();
		ipTextField.setColumns(13);
		ipTextField.setFont(this.getFont());
		ipTextField.addKeyListener(new ErrorKeyListener());
		JLabel label = new JLabel("IP/Hostname: ");
		label.setFont(this.getFont());
		panel.add(label, BorderLayout.WEST);
		panel.add(ipTextField, BorderLayout.EAST);
		mainPanel.add(panel);
		
		//add the port text field
		panel = new JPanel(new BorderLayout());
		portTextField = new JTextField(Integer.toString(MarsProtocol.PORT));
		portTextField.setFont(this.getFont());
		portTextField.setColumns(13);
		portTextField.addKeyListener(new ErrorKeyListener());
		label = new JLabel("Port: ");
		label.setFont(this.getFont());
		panel.add(label, BorderLayout.WEST);
		panel.add(portTextField, BorderLayout.EAST);
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

	/** initialize fields */
	private void initializeVariables() {
		ipTextField.setText(window.getConfig().getDefaultHost());
		portTextField.setText(window.getConfig().getDefaultHostPort()+"");
	}

	/** inherited method, handles OK button press */
	protected boolean actionButton() {
		try {
			String ipStr = ipTextField.getText().trim();
			//do validation of port number input
			int portNum = Integer.parseInt(portTextField.getText().trim());
			if(portNum < 0 || portNum > Integer.MAX_VALUE)
				throw new NumberFormatException();
			
			InetAddress.getByName(ipStr);
			window.getConfig().setDefaultHost(ipStr);
			window.getConfig().setDefaultHostPort(portNum);
			// create a settings.cfg file with set data
			window.getConfig().saveToDefaultFile();
			window.connect();
		} catch(NumberFormatException nfe) {
			//show the error at the port field
			window.showMessage("Error", "Invalid port number");
			ErrorKeyListener.setError(portTextField);
			return false;
		} catch (UnknownHostException unhe) {
			//show the error at the port field
			window.showMessage("Error", "Invalid host address");
			ErrorKeyListener.setError(ipTextField);
			return false;
		} catch (IOException ioe) {
			window.showMessage("Error", "Error saving configuration file.");
		}
		
		return true;
	}
	
	/** basic actionPerformed, handles cancel button */
	public void actionPerformed(ActionEvent e){
		if (e.getActionCommand().trim().equalsIgnoreCase("Cancel")) {
			close();
		} else {
			super.actionPerformed(e);
		}
	}
}