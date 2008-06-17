/**
 * File: GuiUtilities.java
 * Author: Kyle Porter
 * Date: Sept 27th, 2006
 */

package whiteboard.gui;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

/**
 * This class contains utility methods for use in other classes
 */
public class GuiUtilities {

	/**
	 * Returns an ImageIcon, or null if the path was invalid. Starts in the
	 * image directory, just need the file name.
	 * 
	 * @param imgName - the name of the image file
	 */
	public static ImageIcon createImageIcon(String imgName) {
		java.net.URL imgURL = GuiUtilities.class.getResource("images/" + imgName);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		}
		System.err.println("Couldn't find file: " + imgName);
		return null;
	}

	/**
	 * Returns an Image, or null if the path was invalid. Starts in the image
	 * directory, just need the file name.
	 * 
	 * @param imgName - the name of the image file
	 */
	public static Image createImage(String imgName) {
		java.net.URL imgURL = GuiUtilities.class.getResource("images/" + imgName);
		if (imgURL != null) {
			return Toolkit.getDefaultToolkit().getImage(imgURL);
		}
		System.err.println("Couldn't find file: " + imgName);
		return null;
	}
	
	/**
	 * Create a button. To create a button with no icon, use null, and same for text.
	 * However, both cannot be null or null will be returned instead of a button.
	 * 
	 * @param icon - the icon to be used in the button (can be null)
	 * @param text - the text to be displayed on the button (can be null)
	 * @param actionCommand - the action command for the button
	 * @param toolTip - the tooltip for the button
	 * @param listener - the actionlistener for the button
	 * @return the completed button
	 */
	public static JToggleButton createButton(ImageIcon icon, String text, String actionCommand, String toolTip, ActionListener listener) {
		if(icon == null && text == null)
			return null;

		JToggleButton button;
		if(icon != null && text == null)
			button = new JToggleButton(icon);
		else if(icon == null)
			button = new JToggleButton(text);
		else
			button = new JToggleButton(text, icon);

		button.setActionCommand(actionCommand);
		button.setToolTipText(toolTip);
		button.addActionListener(listener);

		return button;
	}
	
	/** 
	 * load settings from fileName
	 * @param fileName - the name of the file to load (ie settings.cfg)
	 */
	public static String loadFromFile(String fileName) throws FileNotFoundException, IOException {
		//get the file stream of the file
		BufferedReader file = new BufferedReader(new FileReader(fileName));
		//create a buffer to carry the contents
		StringBuffer fileBuffer = new StringBuffer();
		//character array to read into
		char charBuffer[] = new char[1024];
		int numRead;
		//while not at end of file, read in and append to buffer
		while ((numRead = file.read(charBuffer)) != -1) {
			fileBuffer.append(charBuffer, 0, numRead);
		}
		//close stream
		file.close();
		
		return fileBuffer.toString();
	}
	
	/** 
	 * save fileContents to fileName
	 * @param fileName - the name of the file to save to (ie settings.cfg)
	 * @param fileContents - the contents of the file as a string
	 */
	public static void saveToFile(String fileName, String fileContents) throws IOException {
		//create stream to output file
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		//write data
		out.write(fileContents);
		//close stream
		out.close();
	}

	/** 
	 * reads an object from the given file, which is in xml format
	 * @param fileName - the name of the file to load from(ie settings.cfg)
	 */
    public static Object xmlLoadFromFile(String fileName) throws FileNotFoundException {
        XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(fileName)));
        Object obj = decoder.readObject();
        decoder.close();
        return obj;
    }

	/** 
	 * writes the given object out in xml format to a file
	 * @param obj - the object to write to file
	 * @param fileName - the name of the file to save to (ie settings.cfg)
	 */
	public static void xmlSaveToFile(Object obj, String fileName) throws FileNotFoundException {
		XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(fileName)));
		encoder.writeObject(obj);
        encoder.close();
	}
}