/**
 * File: WhiteboardConfiguration.java
 * Author: Kyle Porter
 * Date: Sept 30th, 2006
 */

package whiteboard.core;

import whiteboard.networking.Peer;
import whiteboard.networking.WhiteboardPeer;


/**
 * This class extends Configuration to create a settings class for whiteboards that
 * includes any additional whiteboard settings.
 */
public class WhiteboardConfiguration extends Configuration {
	/** number of tries a user gets to enter the right password */
	public static final int NUM_PASSWORD_TRIES = 3;
	
	/** permission level for this whiteboard */
	private WhiteboardCore.WB_PERM_LEVEL permissionLevel;
	/** the name of this whiteboard */
	private String name;
	/** the head peer for this whiteboard */
	private Peer head;
	/** the password for this whiteboard */
	private char[] password;
	/** default joining user perm level */
	private WhiteboardPeer.PERM_LEVEL joinDefaultUserPermLevel = WhiteboardPeer.PERM_LEVEL.VIEWER;
	/** port for this whiteboard */
	private int port;

	
	public String getDebugInfo()
	{
		
		return "ConfigObject: \n	Name: " + name + ", Password: " + password.toString() + 
							  "\n  	Head: " + ( (WhiteboardPeer) head).getDebugInfo();
	}
	
	/** constructor */
	public WhiteboardConfiguration() {
		super();
	}
	
	/** copy constructor */
	public WhiteboardConfiguration(Configuration config, WhiteboardCore core) {
		setFont(config.getFont());
		try {
			setUserPeer(new WhiteboardPeer(config.getUserPeer()));
		} catch (Exception e) {
			setUserPeer(null);
		}
		setDefaultListenPort(config.getDefaultListenPort());
		setDefaultHost(config.getDefaultHost());
		setDefaultHostPort(config.getDefaultHostPort());
		permissionLevel = core.getPermissionLevel();
		name = core.getName();
		head = core.getHead();
		password = core.getPassword();
		port = core.getHead().getPort();
	}

	public WhiteboardCore.WB_PERM_LEVEL getPermissionLevel() {
		return permissionLevel;
	}

	public void setPermissionLevel(WhiteboardCore.WB_PERM_LEVEL permissionLevel) {
		this.permissionLevel = permissionLevel;
	}

	public Peer getHead() {
		return head;
	}

	public void setHead(Peer head) {
		this.head = head;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public char[] getPassword() {
		return password;
	}

	public WhiteboardPeer.PERM_LEVEL getDefaultJoinUserPermLevel() {
		return joinDefaultUserPermLevel;
	}

	public void setDefaultJoinUserPermLevel(WhiteboardPeer.PERM_LEVEL joinUserPermLevel) {
		this.joinDefaultUserPermLevel = joinUserPermLevel;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public int getPort() {
		return port;
	}
}