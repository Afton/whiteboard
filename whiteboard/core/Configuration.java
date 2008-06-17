/**
 * File: Configuration.java
 * Author: Kyle Porter
 * Date: Sept 30th, 2006
 */

package whiteboard.core;

import java.awt.Color;
import java.awt.Font;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import whiteboard.gui.GuiUtilities;
import whiteboard.networking.WhiteboardPeer;
import whiteboard.networking.mars.MarsProtocol;

/**
 * This class holds all the settings for the windows
 */
public class Configuration {
	/** displayed font */
	private Font font;
	
	/** local user peer */
	private WhiteboardPeer userPeer;
	/** default listening port to use */
	private int defaultListenPort;
	/** default host to connect to */
	private String defaultHost;
	/** default host port to connect to */
	private int defaultHostPort;
	/** randomly assigned user colour */
	private Color userColour;

	/** constructor */
	public Configuration() {
		font = new Font("Arial", Font.PLAIN, 12);
		Random rand = new Random();
		try {
			char chars[] = new char[4];
			chars[0] = (char) ('a' + rand.nextInt(26));
			chars[1] = (char) ('a' + rand.nextInt(26));
			chars[2] = (char) ('a' + rand.nextInt(26));
			chars[3] = (char) ('a' + rand.nextInt(26));
			userPeer = new WhiteboardPeer(new String(chars), WhiteboardPeer.PERM_LEVEL.VIEWER);
		} catch (Exception e) {
			setUserPeer(null);
		}
		userColour = new Color(rand.nextInt(256)/256f, rand.nextInt(256)/256f, rand.nextInt(256)/256f);
		defaultListenPort = MarsProtocol.PORT;
		
		//set the default host options
		defaultHost = "";
		defaultHostPort = MarsProtocol.PORT;
	}

	/** load settings from settings.cfg file */
	public void loadFromDefaultFile() throws FileNotFoundException {
		loadFromFile("settings.cfg");
	}

	/** 
	 * load settings from fileName
	 * @param fileName - the name of the file to load (ie settings.cfg)
	 */
	public void loadFromFile(String fileName) throws FileNotFoundException {
		Configuration config = (Configuration) GuiUtilities.xmlLoadFromFile(fileName);
		this.font = config.getFont();
		this.userPeer = config.getUserPeer();
		this.defaultListenPort = config.getDefaultListenPort();
		this.defaultHost = config.getDefaultHost();
		this.defaultHostPort = config.getDefaultHostPort();
		this.userColour = config.getUserColour();
	}

	/** save settings to settings.cfg file */
	public void saveToDefaultFile() throws IOException {
		saveToFile("settings.cfg");
	}

	/** 
	 * save settings to fileName
	 * @param fileName - the name of the file to save to (ie settings.cfg)
	 */
	public void saveToFile(String fileName) throws IOException {
		GuiUtilities.xmlSaveToFile(this, fileName);
	}

	public Font getFont() {
		return font;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public WhiteboardPeer getUserPeer() {
		return userPeer;
	}

	public void setUserPeer(WhiteboardPeer client) {
		this.userPeer = client;
	}

	public String getDefaultHost() {
		return defaultHost;
	}

	public void setDefaultHost(String defaultHost) {
		this.defaultHost = defaultHost;
	}

	public int getDefaultHostPort() {
		return defaultHostPort;
	}

	public void setDefaultHostPort(int defaultHostPort) {
		this.defaultHostPort = defaultHostPort;
	}

	public int getDefaultListenPort() {
		return defaultListenPort;
	}

	public void setDefaultListenPort(int defaultListenPort) {
		this.defaultListenPort = defaultListenPort;
	}

	public Color getUserColour() {
		return userColour;
	}

	public void setUserColour(Color userColour) {
		this.userColour = userColour;
	}
}