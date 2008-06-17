/**
 * File: ConnectWindow.java
 * Author: Kyle Porter
 * Date: Sept 26th, 2006
 */

package whiteboard.gui.startlist;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Timer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import whiteboard.core.Configuration;
import whiteboard.core.WhiteboardConfiguration;
import whiteboard.core.WhiteboardCore;
import whiteboard.gui.dialogs.BasicFrame;
import whiteboard.gui.dialogs.CreateDialog;
import whiteboard.gui.dialogs.ErrorKeyListener;
import whiteboard.gui.dialogs.PreferencesDialog;
import whiteboard.gui.whiteboard.WhiteboardWindow;
import whiteboard.networking.Peer;
import whiteboard.networking.WhiteboardPeer;
import whiteboard.networking.mars.MarsClient;
import whiteboard.networking.mars.MarsClientThread;
import whiteboard.networking.mars.MarsProtocol;
import whiteboard.networking.mars.MarsServer;
import whiteboard.networking.venus.VenusClient.CONNECT_TYPE;

/**
 * The class that is the initial window of the whiteboarding application. This
 * sets the layout of the list, and displays all available whiteboards.
 */
@SuppressWarnings("serial")
public class ConnectWindow extends BasicFrame implements ActionListener {
	public static final long TIMEOUT = 300000; // 5 minutes

	/** all created whiteboards */
	ArrayList<WhiteboardWindow> whiteboards;

	// GUI COMPONENTS
	private ConnectList connectList;
	private ConnectMenuBar menuBar;
	private JPopupMenu popup;

	// NETWORK
	private MarsServer marsServer;

	private boolean isConnected = false;
	
	private Timer updateTimer;

	/** constructor */
	public ConnectWindow() {
		super(1);

		initializeVariables();
		initializeLayout();
		updateFont(getConfig().getFont());		
		
		super.open(300, 500);
	}

	/**
	 * open a create whiteboard dialog, and move the created whiteboard
	 * to the front
	 */
	protected void openCreateDialog() {
		CreateDialog cd = new CreateDialog(this);
		// make sure that a whiteboard was actually created, then bring it to
		// the front
		if (!cd.isCancelled())
			whiteboards.get(whiteboards.size() - 1).toFront();
	}

	/**
	 * called when an action is triggered
	 * 
	 * @param action - the action that triggered this method call
	 */
	public void actionPerformed(ActionEvent action) {
		if (action.getActionCommand().equals("Launch Whiteboard")) {
			// connect or popup option
			launchWhiteboard();
		} else if (action.getActionCommand().equals("Create Whiteboard...")) {
			//launch a create dialog
			openCreateDialog();
		} else if (action.getActionCommand().equals("Show Connected Users")) {
			String users = (MarsProtocol.getPeers().size() > 0) ? MarsProtocol.getPeers().toString() : "[None]";
			showMessage("Connected Users", users);
		}
	}

	/** called to connect to a network of whiteboards */
	public void connect() {
		try {
			closeAllWhiteboards();
			// Create update timer
			menuBar.disableConnectMenuItems();
			updateTimer = new Timer(getName());
			updateTimer.scheduleAtFixedRate(new MarsClientThread(this, getConfig().getUserPeer().getName(), getConfig().getDefaultHost(), getConfig().getDefaultHostPort()), 0, TIMEOUT);
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}
	}

	public void setIsConnected(boolean isConnected) {
		// enable disconnect/disable connect options
		menuBar.setConnectedMenuItems(isConnected);
		if(isConnected && !this.isConnected)
			showMessage("Notice", "You are now connected.");
		this.isConnected = isConnected;
		if(!isConnected)
			updateTimer.cancel();
	}
	
	public void serverSetConnected() {
		menuBar.setConnectedMenuItems(true);
		this.isConnected = true;
	}

	/** returns true if connected to a network of whiteboards */
	public boolean isConnected() {
		return isConnected;
	}

	/** method to safely exit program, close all connections and windows, etc */
	public void safeExit() {
		try {
			// create a settings.cfg file with set data
			getConfig().saveToDefaultFile();
		} catch (IOException ioe) {
			showMessage("Error", "Error saving configuration file.");
		}
		if(updateTimer != null) {
			updateTimer.cancel();
		}

		disconnect();
		close();
	}

	private void closeAllWhiteboards() {
		for (int i = whiteboards.size() - 1; i >= 0; --i) {
			if (!whiteboards.get(i).isVisible()) {
				whiteboards.remove(whiteboards.get(i));
			} else {
				whiteboards.get(i).safeExit(CONNECT_TYPE.GOOD, true);
			}
		}
	}

	/**
	 * change the font and propagate to sub windows
	 * 
	 * @param font - the font to change to
	 */
	public void updateFont(Font font) {
		connectList.updateFont(font);
	}

	/** abstract method from superclass */
	protected void initializeProgramVariables() {
		setVersion("0.678");
		setVersionDate("Nov 24, 2006");
		setProgramTitle("Whiteboard Connection List");
	}

	private void createPopupMenu() {
		// create the list popup menu
		popup = new JPopupMenu();
		popup.setBorder(BorderFactory.createTitledBorder("Whiteboard Options"));
		
		JMenuItem mItem = new JMenuItem("Show Connected Users");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		popup.add(mItem);
		
		popup.addSeparator();

		mItem = new JMenuItem("Launch Whiteboard");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		popup.add(mItem);

		mItem = new JMenuItem("Create Whiteboard...");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		popup.add(mItem);
	}

	/** disconnect from the whiteboard network */
	protected void disconnect() {
		try {
			closeAllWhiteboards();

			// Close peer connections
			for (Peer peer : MarsProtocol.getPeers()) {
				peer.close();
			}
			
			// Stop the update timer
			if (null != updateTimer) {
				updateTimer.cancel();
			}

			// Stop mars server
			if (marsServer != null) {
				marsServer.close();
			}

			// enable disconnect/disable connect options
			isConnected = false;
			if (menuBar != null)
				menuBar.setConnectedMenuItems(false);
			
			MarsProtocol.clear();
			connectList.repaint();
		} catch (IOException ioe) {
			System.err.println("Error closing mars server: " + ioe.getMessage());
		}
	}

	/** the method responsible for laying out the parts of the main window */
	private void initializeLayout() {
		// set panel to have a borderlayout
		this.getContentPane().setLayout(new BorderLayout());

		// set the title of the program
		setTitle(getProgramTitle());

		// create and set the menu bar
		menuBar = new ConnectMenuBar(this);
		setJMenuBar(menuBar);
		menuBar.setConnectedMenuItems(false);

		// create and set the whiteboard list
		connectList = new ConnectList(this);
		this.add(new JScrollPane(connectList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

		// add a listener for the popup menu
		connectList.addMouseListener(new PopupListener());

		// create and set the connect button
		JButton button = new JButton("Launch Whiteboard");
		button.setActionCommand(button.getText());
		button.addActionListener(this);
		button.setToolTipText("Connect to selected whiteboard.");
		button.setFont(getConfig().getFont());
		this.getRootPane().setDefaultButton(button);
		this.add(button, BorderLayout.SOUTH);
	}

	/**
	 * the method that initializes any variables that need initialization on
	 * program start, and add any listeners to this frame.
	 */
	private void initializeVariables() {
		// load initial configuration
		this.setConfig(new Configuration());
		loadInitialConfig();

		whiteboards = new ArrayList<WhiteboardWindow>(1);

		// add an exit window listener
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(@SuppressWarnings("unused")
			WindowEvent e) {
				safeExit();
			}
		});

		// Create popup menu
		createPopupMenu();

		// Start the Mars Protocol server
		boolean isListening = false;
		while (!isListening) {
			try {
				marsServer = new MarsServer(this, getConfig().getUserPeer().getName(), getConfig().getDefaultListenPort());
				marsServer.start();
				isListening = true;
			} catch (BindException be) {
				showMessage("Error", "Port number is already in use.");
				PreferencesDialog pDialog = new PreferencesDialog(this);
				ErrorKeyListener.setError(pDialog.getDefaultListenPortField());
				pDialog.open();
				if (pDialog.isCancelled()) {
					// exit the application, as user cancelled preferences when
					// port open
					System.exit(0);
				}
			} catch (IOException ioe) {
				showMessage("Error", "Failed to start internal server");
				this.safeExit();
			}
		}
	}

	/** load the initial configuration file, or if not found create a default one */
	private void loadInitialConfig() {
		try {
			// try to load from settings.cfg file
			getConfig().loadFromDefaultFile();
		} catch (FileNotFoundException fne) {
			try {
				// create a settings.cfg file with default data
				getConfig().saveToDefaultFile();
			} catch (IOException ioe) {
				showMessage("Error", "Error creating default configuration file.");
			}
		} catch (Exception e) {
			// problem with formatting of internal file data
			showMessage("Error", e.getLocalizedMessage());
		}
	}

	/**
	 * get the whiteboard object for the whiteboardcore object. This method also
	 * removes whiteboard windows that have already been closed.
	 * 
	 * @param core - the core object to use for the whiteboard
	 * @return the whiteboardwindow for the core object, or null if it doesn't exist
	 */
	private WhiteboardWindow getWhiteboard(WhiteboardCore core) {
		for (int i = whiteboards.size() - 1; i >= 0; i--) {
			if (!whiteboards.get(i).isVisible()) {
				whiteboards.remove(whiteboards.get(i));
			} else if (((WhiteboardConfiguration) whiteboards.get(i).getConfig()).getName().equals(core.getName())) {
				return whiteboards.get(i);
			}
		}
		return null;
	}

	/** launch the selected whiteboard from the connectlist */
	public void launchWhiteboard() {
		// ensure there is a whiteboard selected
		if (connectList.getSelectedIndex() < 0)
			return;

		// launch the selected whiteboard
		launchWhiteboard(false, ((WhiteboardCore) connectList.getSelectedValue()));
	}

	/**
	 * create the whiteboard
	 * 
	 * @param isCreating - is the user creating this whiteboard
	 * @param permLevel - the permission level of the whiteboard
	 */
	public boolean launchWhiteboard(boolean isCreating, WhiteboardCore core) {
		try {
			if (isCreating) {
				if (!MarsClient.createWhiteboard(core)) {
					return false;
				}
				// if user is creating, set permission level to owner
				getConfig().getUserPeer().setPermissionLevel(WhiteboardPeer.PERM_LEVEL.OWNER);
			} else {
				// else set to viewer, and check to make sure not passworded
				getConfig().getUserPeer().setPermissionLevel(WhiteboardPeer.PERM_LEVEL.VIEWER);
			}
			// check if this whiteboard is already launched
			WhiteboardWindow win = getWhiteboard(core);
			if (win != null) {
				win.toFront();
				return false;
			}
			//check to make sure you can use the port
			new ServerSocket(core.getHead().getPort()).close();
			return whiteboards.add(new WhiteboardWindow(getConfig(), core));
		} catch (IOException e) {
			showMessage("Error", "Whiteboard port already in use.");
			return false;
		}
	}

	public static void main(String[] args) {
		// give the windows a slightly different look
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		new ConnectWindow();
	}

	/**
	 * Class to listen for mouse events to show the popup menu on the user list.
	 * This was taken from the Java Menu tutorial
	 * (http://java.sun.com/docs/books/tutorial/uiswing/components/menu.html#popup)
	 */
	protected class PopupListener extends MouseAdapter {
		@SuppressWarnings("synthetic-access")
		public void mouseReleased(MouseEvent e) {
			if (SwingUtilities.isRightMouseButton(e)) {
				connectList.setSelectedIndex(connectList.locationToIndex(e .getPoint()));
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
}