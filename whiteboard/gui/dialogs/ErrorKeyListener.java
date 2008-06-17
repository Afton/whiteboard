/**
 * File: ErrorKeyListener.java
 * Author: Kyle Porter
 * Date: Oct 1st, 2006
 */

package whiteboard.gui.dialogs;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;

/** 
 * This class sets the foreground color to black on keypress.
 * Also contains a method to set the field to errored.
 */
public class ErrorKeyListener implements KeyListener {
	/** color to set a field when an error occurs */
	private static Color ERROR_COLOR = Color.RED;

	public void keyPressed(KeyEvent arg0) {
		//if the source is a text field, set back to black
		if(arg0.getSource() instanceof JTextField)
			((JTextField) arg0.getSource()).setForeground(Color.BLACK);
	}

	public void keyReleased(@SuppressWarnings("unused") KeyEvent arg0) {}
	public void keyTyped(@SuppressWarnings("unused") KeyEvent arg0) {}

	/** used to set a field to error state (default red color, request focus) */
	public static void setError(Object obj) {
		if(obj instanceof JTextField) {
			((JTextField) obj).requestFocus();
			((JTextField) obj).setForeground(ERROR_COLOR);
		}
	}
}