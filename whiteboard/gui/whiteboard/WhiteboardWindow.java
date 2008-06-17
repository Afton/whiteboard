/**
 * File: WhiteboardWindow.java
 * Author: Kyle Porter
 * Date: Sept 25th, 2006
 */

package whiteboard.gui.whiteboard;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import whiteboard.core.CloseableThread;
import whiteboard.core.Configuration;
import whiteboard.core.WhiteboardConfiguration;
import whiteboard.core.WhiteboardCore;
import whiteboard.core.entities.BytePacker;
import whiteboard.core.exceptions.TransactionManagerAlreadySetException;
import whiteboard.core.transaction.TransactionManager;
import whiteboard.gui.dialogs.BasicFrame;
import whiteboard.gui.dialogs.PasswordDialog;
import whiteboard.networking.WhiteboardPeer;
import whiteboard.networking.mars.MarsClient;
import whiteboard.networking.venus.VenusClient;
import whiteboard.networking.venus.VenusProtocol;
import whiteboard.networking.venus.VenusSendThread;
import whiteboard.networking.venus.VenusServer;
import whiteboard.networking.venus.VenusClient.CONNECT_TYPE;

/**
 * The class that is the main window of the whiteboarding application. This sets
 * the layout of the application, and initializes any needed variables.
 */
@SuppressWarnings("serial")
public class WhiteboardWindow extends BasicFrame {
	private VenusProtocol venusProtocol;
	private List<CloseableThread> threads = new ArrayList<CloseableThread>();
	private TransactionManager transMan;
	private NetworkChatPollThread chatPollThread;
	private final WhiteboardCore core;
	
	//GUI COMPONENTS
	private ButtonPanel buttonPanel;
	private ChatPanel chatPanel;
	private UserListPanel userListPanel;
	protected Canvas canvas;
	private MenuBar menuBar;

	private boolean isInSetup = true;
	private boolean isClosing = false;

	
	
	/**
	 * Constructor.
	 */
	public WhiteboardWindow(Configuration config, WhiteboardCore core) {
		//initialize any variables that need it
		this.core = core;
		initializeVariables(config, core);
		//do the layout for the application
		initializeLayout();

		try {
			initializeTransactionManager(config, core);

			super.open(800, 600, false);
			
			if (config.getUserPeer().equals(core.getHead())) {
				setVisible(true);
			}
		}  catch (IOException ioe) {
			showMessage("Error", "Error setting up whiteboard.");
			this.safeExit(CONNECT_TYPE.BAD, false);
		}
	}

	private void initializeTransactionManager(Configuration config, WhiteboardCore wCore) throws IOException {
		try{
			//initialize tansaction manager
			transMan = new TransactionManager(wCore.getHead(), canvas, venusProtocol);
			canvas.setTransactionManager(transMan);

			// Get password if needed
			if ((wCore.getPermissionLevel() == WhiteboardCore.WB_PERM_LEVEL.LOCKED) && !config.getUserPeer().getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.OWNER)) {
				PasswordDialog pDialog = new PasswordDialog(this);
				((WhiteboardConfiguration) getConfig()).setPassword(pDialog.getPassword().toCharArray());

				if (pDialog.isCancelled()) {
					safeExit(CONNECT_TYPE.BAD, false);
					return;
				}

				threads.add(new VenusClient(wCore.getHead(), wCore.getHead().getPort(), true, venusProtocol, transMan, canvas, pDialog.getPassword()));
			} else {
				if (!config.getUserPeer().equals(wCore.getHead())) {
					threads.add(new VenusClient(wCore.getHead(), wCore.getHead().getPort(), true, venusProtocol, transMan, canvas));
				}
			}

			// Add the thread for sending shapes
			threads.add(new VenusSendThread(venusProtocol, transMan));
			// Add thread for listening for incoming connections
			threads.add(new VenusServer(getConfig().getUserPeer(), core.getHead().getPort(), venusProtocol, transMan));

			// Start threads
			for (Thread thread : threads) {
				thread.start();
			}
		} catch (TransactionManagerAlreadySetException e) {
			// this should *never* happen
			e.printStackTrace(); 
		}
	}
	
	/** abstract method from superclass */
	@Override
	protected void initializeProgramVariables() {
		setVersion("0.2");
		setVersionDate("Sept 30, 2006");
		setProgramTitle("Distributed Whiteboard");
	}

	/** the method responsible for laying out the parts of the main window */
	private void initializeLayout() {
		//set panel to have a borderlayout
		this.getContentPane().setLayout(new BorderLayout());

		//set the title of the program
		setTitle(getProgramTitle() + ": " + ((WhiteboardConfiguration) getConfig()).getName());
		
		//create and add the canvas (whiteboard)
		canvas = new Canvas(this);

		//create and add the button panel
		buttonPanel = new ButtonPanel(canvas, getConfig().getFont());
		this.getContentPane().add(buttonPanel, BorderLayout.WEST);

		JScrollPane scrollPane = new JScrollPane(canvas, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.getHorizontalScrollBar().setMaximum(canvas.getPreferredSize().width);
		scrollPane.getHorizontalScrollBar().setValue(canvas.getPreferredSize().width/2 - 250);
		scrollPane.getVerticalScrollBar().setMaximum(canvas.getPreferredSize().height);
		scrollPane.getVerticalScrollBar().setValue(canvas.getPreferredSize().height/2 - 250);
		this.getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		//create and add the user list
		userListPanel = new UserListPanel(this, venusProtocol);
		this.getContentPane().add(userListPanel, BorderLayout.EAST);
		
		//create and add the chat panel
		chatPanel = new ChatPanel(this);
		this.getContentPane().add(chatPanel, BorderLayout.SOUTH);

		//create and set the menu bar
		menuBar = new MenuBar(this);
		setJMenuBar(menuBar);
		

		//if you're the head, allow settings to go
		if(getConfig().getUserPeer().getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.OWNER))
			canvas.setAllowUserInput(true);
		//create a list data listener to ensure there is a head, so you can't draw unless there is
		userListPanel.addListDataListener(new ListDataListener() {
			public void contentsChanged(ListDataEvent arg0) {
				if(isClosing)
					return;
				boolean hasHead = false;
				if(((VenusProtocol) arg0.getSource()).getPeers().size() > 0)
					hasHead = ((VenusProtocol) arg0.getSource()).getPeers().get(0).getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.OWNER);
				canvas.setAllowUserInput(!getConfig().getUserPeer().getPermissionLevel().equals(WhiteboardPeer.PERM_LEVEL.VIEWER));
				if(isInSetup && hasHead) {
					isInSetup = false;
				} else if(!isInSetup && !hasHead) {
					transMan.setElection(true);
					//call an election
					venusProtocol.callElection(transMan);
				}
			}

			public void intervalAdded(@SuppressWarnings("unused") ListDataEvent arg0) {}
			public void intervalRemoved(@SuppressWarnings("unused") ListDataEvent arg0) {}
		});
	}

	/** 
	 * the method that initializes any variables that need initialization
	 * on program start, and add any listeners to this frame.
	 */
	private void initializeVariables(Configuration config, WhiteboardCore wCore) {
		this.setConfig(new WhiteboardConfiguration(config, wCore));
		
		//add an exit window listener
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(@SuppressWarnings("unused") WindowEvent e) {
				safeExit(CONNECT_TYPE.BAD, true);
			}
		});

		//add a resize listener
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(@SuppressWarnings("unused") ComponentEvent e) {
				//resize the canvas whenever the main window is resized
				canvas.autoscale();
			}
		});

		// Venus stuff
		venusProtocol = new VenusProtocol((WhiteboardConfiguration) getConfig(), this);

		//chat polling thread
		chatPollThread = new NetworkChatPollThread(this);
		chatPollThread.start();
	}

	/**
	 * set the button panel to be showing or not
	 * @param isShowing - true if button panel to be shown or not
	 */
	protected void setButtonPanelShowing(boolean isShowing) {
		buttonPanel.setVisible(isShowing);
	}
	
	/**
	 * set the chat panel to be showing or not
	 * @param isShowing - true if chat panel to be shown or not
	 */
	protected void setChatPanelShowing(boolean isShowing) {
		chatPanel.setVisible(isShowing);
	}
	
	/**
	 * set the user list to be showing or not
	 * @param isShowing - true if user list to be shown or not
	 */
	protected void setUserListShowing(boolean isShowing) {
		userListPanel.setVisible(isShowing);
	}

	/**
	 * Return the whiteboard canvas.
	 * @return canvas representing the whiteboard
	 */
	protected Canvas getCanvas() {
		return canvas;
	}

	/** save the whiteboard to a local file */
	protected void save() {
		//save the canvas
		JFileChooser save = new JFileChooser();
		save.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		save.setLocation(0, 0);

		if (save.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				//create stream to output file
				FileOutputStream out = new FileOutputStream(save.getSelectedFile());
				List<List<Byte>> byteList = canvas.getByteRepresentation();
				for(List<Byte> list : byteList) {
					//write out number of bytes in this object
					for(Byte b : BytePacker.convertIntToBytes(list.size())) {
						out.write(b);
					}
					//write out actual data
					for(Byte b : list) {
						out.write(b);
					}
				}
				out.close();
			} catch (IOException ioe) {
				System.err.println(ioe.getMessage());
				ioe.printStackTrace();
			}
		}
	}
	
	/** load the whiteboard from a local file */
	protected void load() {
		//load a saved whiteboard
		JFileChooser load = new JFileChooser();
		load.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		load.setLocation(0, 0);

		if (load.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				List<List<Byte>> byteList = new ArrayList<List<Byte>>();
				//get the file stream of the file
				FileInputStream iStream = new FileInputStream(load.getSelectedFile());
				List<Byte> list;
				//array to hold actual data
				byte[] objData;
				//array to hold byte value of integer
				byte[] intBytes = new byte[4];
				int numBytes;
				//keep reading in integer byte values for objects
				while(iStream.read(intBytes) >= 0) {
					//get the number of bytes in the object
					numBytes = BytePacker.convertBytesToInt(intBytes, 0);
					list = new ArrayList<Byte>(numBytes);
					objData = new byte[numBytes];
					//read in the data, add to the list
					if(iStream.read(objData) >= 0) {
						for(byte b : objData) {
							list.add(b);
						}
					}
					//add the completed byte list to the whole arraylist
					byteList.add(list);
				}
				//close stream
				iStream.close();
				canvas.loadFromByteRepresentation(byteList);
			} catch(FileNotFoundException fne) {
				fne.printStackTrace();
			} catch(IOException ie) {
				ie.printStackTrace();
			}
		}
	}

	/** disconnect from the whiteboard network */
	protected void disconnect() {
		// Stop threads
		for (CloseableThread closeableThread : threads) {
			closeableThread.close();
		}
	}

	/** method to safely exit program, close all connections and windows, etc */
	public void safeExit(CONNECT_TYPE connected, boolean sendDelete) {
		isClosing = true;
		//dispose all open windows
		if(userListPanel != null) {
			userListPanel.closeAllPrivateChats();
		}
		if(canvas != null) {
			canvas.stopPollThread();
		}
		if(chatPollThread != null) {
			chatPollThread.quit();
		}
		if(transMan != null) {
			transMan.stopThreads();
		}
		
		if (sendDelete && (1 == venusProtocol.getPeers().size())) {
			MarsClient.deleteWhiteboard(core);
		}
		
		//disconnect from network
		disconnect();

		switch (connected) {
			case BAD_PASSWORD:
				showMessage("Notice", "Bad password");
				break;
			case KICKED:
				showMessage("Notice", "You've been kicked");
				break;
		}
		
		// exit
		close();
	}

	/**
	 * change the font and propagate to sub windows
	 * @param font - the font to change to
	 */
	@Override
	public void updateFont(Font font) {
		buttonPanel.updateFont(font);
		chatPanel.updateFont(font);
		userListPanel.updateFont(font);
	}

	public static void main(String[] args) {
		//give the windows a slightly different look
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		new WhiteboardWindow(new Configuration(), new WhiteboardCore("Whiteboard", WhiteboardCore.WB_PERM_LEVEL.UNLOCKED, null));
	}
	
	public WhiteboardCore getCore() {
		return core;
	}

	public ChatPanel getChatPanel() {
		return chatPanel;
	}

	public UserListPanel getUserListPanel() {
		return userListPanel;
	}

	public TransactionManager getTransactionManager() {
		return transMan;
	}
	
	public void debug()
	{
		String coreInfo = core.getDebugInfo(); 
		String venusInfo = venusProtocol.getWhiteboardConfig().toString();
		
	}
}