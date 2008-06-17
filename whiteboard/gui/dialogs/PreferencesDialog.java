package whiteboard.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import whiteboard.core.Configuration;
import whiteboard.core.WhiteboardConfiguration;
import whiteboard.core.WhiteboardCore;
import whiteboard.gui.startlist.ConnectWindow;
import whiteboard.gui.whiteboard.WhiteboardWindow;
import whiteboard.networking.WhiteboardPeer;

@SuppressWarnings("serial")
public class PreferencesDialog extends BasicDialog {
	/** text field to take username */
	private JTextField userNameField;
	/** text field to take default listening port */
	private JTextField defaultListenPortField;
	/** text field to take default connection host */
	private JTextField defaultHostField;
	/** text field to take default connection host port */
	private JTextField defaultHostPortField;
	/** drop down to set the default permission level for joining users */
	private JComboBox defaultUserPermLevel;

	/** true if user cancelled dialog */
	private boolean isCancelled;
	
	/** the parent window for this dialog */
	private BasicFrame window;
	
	/**
	 * constructor
	 * @param window - the parent window of this dialog
	 */
	public PreferencesDialog(ConnectWindow window) {
		super(window, "Change Preferences", true);
		initialize(window);
	}
	
	/**
	 * constructor
	 * @param window - the parent window of this dialog
	 */
	public PreferencesDialog(WhiteboardWindow window) {
		super(window, "Change Preferences", true);
		initialize(window);
	}

	/**
	 * common initialization for constructors
	 * @param wbWindow - parent window of this dialog
	 */
	private void initialize(BasicFrame wbWindow) {
		this.isCancelled = true;
		this.window = wbWindow;
		this.setFont(wbWindow.getConfig().getFont());

		initializeLayout();
		//select the correct combobox items for the window font
		initializeVariables();
	}
	
	public void open() {
		super.packCenterOpen();
	}

	/** initialize any variables needed for dialog start */
	private void initializeVariables() {
		//will need to get items from config to populate the textfields
		Configuration config = window.getConfig();
		if(window instanceof ConnectWindow) {
			userNameField.setText(config.getUserPeer().getName());
			this.defaultHostField.setText(config.getDefaultHost());
			this.defaultHostPortField.setText(config.getDefaultHostPort()+"");
			this.defaultListenPortField.setText(config.getDefaultListenPort()+"");
		} else {
			if(((WhiteboardConfiguration) window.getConfig()).getUserPeer().getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.OWNER)) {
				switch(((WhiteboardConfiguration) window.getConfig()).getDefaultJoinUserPermLevel()) {
					case EDITOR:
						this.defaultUserPermLevel.setSelectedItem("Editor");
						break;
					case VIEWER:
						this.defaultUserPermLevel.setSelectedItem("Viewer");
						break;
				}
			}
		}
	}

	/** initialize and set the layout for the dialog */
	private void initializeLayout() {
		JPanel panel = null;
		JLabel label = null;
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		if(window instanceof ConnectWindow) {
			//add the user name text field
			panel = new JPanel(new BorderLayout());
			userNameField = new JTextField();
			if((window instanceof ConnectWindow) && ((ConnectWindow) window).isConnected()) {
				userNameField.setEnabled(false);
			}
			userNameField.setColumns(13);
			userNameField.setFont(this.getFont());
			userNameField.addKeyListener(new ErrorKeyListener());
			label = new JLabel("Username: ");
			label.setFont(this.getFont());
			panel.add(label, BorderLayout.WEST);
			panel.add(userNameField, BorderLayout.EAST);
			mainPanel.add(panel);

			//add the default listn port text field
			panel = new JPanel(new BorderLayout());
			defaultListenPortField = new JTextField();
			defaultListenPortField.setColumns(13);
			defaultListenPortField.setFont(this.getFont());
			defaultListenPortField.addKeyListener(new ErrorKeyListener());
			label = new JLabel("Default Listen Port: ");
			label.setFont(this.getFont());
			panel.add(label, BorderLayout.WEST);
			panel.add(defaultListenPortField, BorderLayout.EAST);
			mainPanel.add(panel);
	
			//add the default host text field
			panel = new JPanel(new BorderLayout());
			defaultHostField = new JTextField();
			defaultHostField.setColumns(13);
			defaultHostField.setFont(this.getFont());
			defaultHostField.addKeyListener(new ErrorKeyListener());
			label = new JLabel("Default Host IP: ");
			label.setFont(this.getFont());
			panel.add(label, BorderLayout.WEST);
			panel.add(defaultHostField, BorderLayout.EAST);
			mainPanel.add(panel);
	
			//add the default host port text field
			panel = new JPanel(new BorderLayout());
			defaultHostPortField = new JTextField();
			defaultHostPortField.setColumns(13);
			defaultHostPortField.setFont(this.getFont());
			defaultHostPortField.addKeyListener(new ErrorKeyListener());
			label = new JLabel("Default Host Port: ");
			label.setFont(this.getFont());
			panel.add(label, BorderLayout.WEST);
			panel.add(defaultHostPortField, BorderLayout.EAST);
			mainPanel.add(panel);
		}
		if(window instanceof WhiteboardWindow) {
			if(((WhiteboardConfiguration) window.getConfig()).getUserPeer().getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.OWNER)) {
				panel = new JPanel(new BorderLayout());
				String[] permLevels = {"Viewer", "Editor"};
				defaultUserPermLevel = new JComboBox(permLevels);
				defaultUserPermLevel.setFont(this.getFont());
				defaultUserPermLevel.setPreferredSize(new Dimension(147, 23));
				label = new JLabel("Default User Level: ");
				label.setFont(this.getFont());
				panel.add(label, BorderLayout.WEST);
				panel.add(defaultUserPermLevel, BorderLayout.EAST);
				mainPanel.add(panel);

				//add in a blank space
				mainPanel.add(new JLabel(" "));
			}
			if(((WhiteboardConfiguration) window.getConfig()).getPermissionLevel() == WhiteboardCore.WB_PERM_LEVEL.LOCKED) {
				//display the password
				panel = new JPanel(new BorderLayout());
				JTextField textField = new JTextField(new String(((WhiteboardConfiguration) window.getConfig()).getPassword()));
				textField.setColumns(13);
				textField.setFont(this.getFont());
				textField.setEditable(false);
				label = new JLabel("Current Password: ");
				label.setFont(this.getFont());
				panel.add(label, BorderLayout.WEST);
				panel.add(textField, BorderLayout.EAST);
				mainPanel.add(panel);
			}
			panel = new JPanel(new BorderLayout());
			JTextField textField = new JTextField(((WhiteboardConfiguration) window.getConfig()).getPort()+"");
			textField.setColumns(13);
			textField.setFont(this.getFont());
			textField.setEditable(false);
			label = new JLabel("Whiteboard Port: ");
			label.setFont(this.getFont());
			panel.add(label, BorderLayout.WEST);
			panel.add(textField, BorderLayout.EAST);
			mainPanel.add(panel);
		}

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

	/** inherited method, handles OK button press */
	protected boolean actionButton() {
		try {
			if(window instanceof ConnectWindow) {
				String host = null;
				int hostPortNum = 0, listenPortNum = 0;
				String name = userNameField.getText().trim();
				//check to ensure it isn't a blank name
				if(name.equals("")) {
					throw new Exception("Invalid Username.");
				}

				//do validation of listen port number input
				listenPortNum = Integer.parseInt(defaultListenPortField.getText().trim());
				if(listenPortNum < 0 || listenPortNum > 65535) {
					throw new NumberFormatException("listen");
				}
				
				host = defaultHostField.getText().trim();
				//do validation of listen port number input
				hostPortNum = Integer.parseInt(defaultHostPortField.getText().trim());
				if(hostPortNum < 0 || hostPortNum > 65535) {
					throw new NumberFormatException("host");
				}

				Configuration config = window.getConfig();
				config.getUserPeer().setName(name);
				config.setDefaultHost(host);
				config.setDefaultHostPort(hostPortNum);
				config.setDefaultListenPort(listenPortNum);
				try {
					//create a settings.cfg file with set data
					window.getConfig().saveToDefaultFile();
				} catch(IOException ioe) {
					window.showMessage("Error", "Error saving configuration file.");
				}
			} else {
				if(((WhiteboardConfiguration) window.getConfig()).getUserPeer().getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.OWNER)) {
					if(defaultUserPermLevel.getSelectedItem().toString().equals("Editor")) {
						((WhiteboardConfiguration) window.getConfig()).setDefaultJoinUserPermLevel(WhiteboardPeer.PERM_LEVEL.EDITOR);
					} else {
						((WhiteboardConfiguration) window.getConfig()).setDefaultJoinUserPermLevel(WhiteboardPeer.PERM_LEVEL.VIEWER);
					}
				}
			}
		} catch(NumberFormatException nfe) {
			//show the error at the port field
			window.showMessage("Error", "Invalid port number.");
			if(nfe.getLocalizedMessage().equals("listen"))
				ErrorKeyListener.setError(defaultListenPortField);
			else
				ErrorKeyListener.setError(defaultHostPortField);
			return false;
		} catch(Exception e) {
			//show the error in the name field
			window.showMessage("Error", e.getLocalizedMessage());
			ErrorKeyListener.setError(userNameField);
			return false;
		}

		isCancelled = false;
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
	
	public JTextField getDefaultListenPortField() {
		return defaultListenPortField;
	}
	
	public void setDefaultListenPortError() {
		ErrorKeyListener.setError(defaultListenPortField);
	}

	public boolean isCancelled() {
		return isCancelled;
	}
}