/**
 * 
 */
package whiteboard.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import whiteboard.networking.WhiteboardPeer;

/**
 * @author patrick
 */
public class WhiteboardCore implements Comparable<WhiteboardCore> {
	public static enum WB_PERM_LEVEL {
		UNLOCKED, LOCKED, INVITE
	}

	private String name;

	private WhiteboardPeer head;

	private char[] password;

	private WhiteboardCore.WB_PERM_LEVEL permissionLevel;

	public WhiteboardCore(String name, WhiteboardCore.WB_PERM_LEVEL permLevel,
			WhiteboardPeer head) {
		this.name = name;
		this.head = head;
		permissionLevel = permLevel;
	}

	public WhiteboardCore(String name, WhiteboardCore.WB_PERM_LEVEL permLevel,
			char[] password, String headName, InetAddress addr, int port)
			throws UnknownHostException, IOException {
		this(name, permLevel, new WhiteboardPeer(headName, addr.getAddress(),
				port, WhiteboardPeer.PERM_LEVEL.OWNER));
		if ((null != password) && (0 < password.length)) {
			this.password = password;
		}
	}

	public WhiteboardCore(String name, WhiteboardCore.WB_PERM_LEVEL permLevel,
			String headName, byte[] addr, int port)
			throws UnknownHostException, IOException {
		this(name, permLevel, null, headName, InetAddress.getByAddress(addr),
				port);
	}

	public WhiteboardCore(String name, WhiteboardCore.WB_PERM_LEVEL permLevel,
			char[] password, WhiteboardPeer head) {
		this(name, permLevel, head);
		if ((null != password) && (0 < password.length)) {
			this.password = password;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(WhiteboardCore whiteboard) {
		int compare = whiteboard.permissionLevel.ordinal()
				- permissionLevel.ordinal();

		if (permissionLevel == whiteboard.permissionLevel) {
			compare = name.compareToIgnoreCase(whiteboard.name);
		}

		return compare;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof WhiteboardCore) {
			WhiteboardCore whiteboard = (WhiteboardCore) o;
			return name.equals(whiteboard.name);
		} else if (o instanceof String) {
			return name.equals(o);
		}

		return false;
	}

	public WhiteboardPeer getHead() {
		return head;
	}

	public String getName() {
		return name;
	}

	public void setHead(WhiteboardPeer head) {
		this.head = head;
	}

	public void setName(String name) {
		this.name = name;
	}

	public WhiteboardCore.WB_PERM_LEVEL getPermissionLevel() {
		return permissionLevel;
	}

	public void setPermissionLevel(WhiteboardCore.WB_PERM_LEVEL permissionLevel) {
		this.permissionLevel = permissionLevel;
	}

	@Override
	public String toString() {
		return name;
	}

	public char[] getPassword() {
		return password;
	}

	public void setPassword(char[] password) {
		this.password = password;
		
	}
	
	public String getDebugInfo()
	{			
		return "Core: " + getName() +  
			   "\n	Password: " + getPassword().toString() + 
			   "\n	HEAD: " + head.getDebugInfo();
	}
}
