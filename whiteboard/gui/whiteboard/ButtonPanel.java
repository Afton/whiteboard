/**
 * File: ButtonPanel.java
 * Author: Kyle Porter
 * Date: Sept 25th, 2006
 */

package whiteboard.gui.whiteboard;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import whiteboard.gui.GuiUtilities;

/**
 * This class is the panel that contains the buttons for the whiteboard.
 */
@SuppressWarnings("serial")
public class ButtonPanel extends JPanel implements ActionListener {
	/** the canvas that the buttons act on */
	private Canvas canvas;

	/**
	 * constructor
	 * @param canvas - the canvas the buttons in the panel act on
	 */
	public ButtonPanel(Canvas canvas, Font font) {
		this.canvas = canvas;

		//set the layout and add the buttons
		initializeLayout();
		updateFont(font);
	}

	/** create and add the buttons to the panel */
	private void initializeLayout() {
		//set the layout to be 9 rows, 1 column of buttons
		this.setLayout(new GridLayout(9, 1));
		
		//create the buttons
		ButtonGroup g = new ButtonGroup();
		JToggleButton b = GuiUtilities.createButton(GuiUtilities.createImageIcon("selection_cursor.gif"), null, Canvas.MODE.SELECT.toString(), Canvas.MODE.SELECT.toString(), this);
		b.setSelected(true); g.add(b); this.add(b);
		b = GuiUtilities.createButton(GuiUtilities.createImageIcon("text.gif"), null, Canvas.MODE.TEXT.toString(), Canvas.MODE.TEXT.toString(), this);
		g.add(b); this.add(b);
		b = GuiUtilities.createButton(GuiUtilities.createImageIcon("freehand.gif"), null, Canvas.MODE.FREEHAND.toString(), Canvas.MODE.FREEHAND.toString(), this);
		g.add(b); this.add(b);
		b = GuiUtilities.createButton(GuiUtilities.createImageIcon("points.gif"), null, Canvas.MODE.POINT.toString(), Canvas.MODE.POINT.toString(), this);
		g.add(b); this.add(b);
		b = GuiUtilities.createButton(GuiUtilities.createImageIcon("line.gif"), null, Canvas.MODE.LINE.toString(), Canvas.MODE.LINE.toString(), this);
		g.add(b); this.add(b);
		b = GuiUtilities.createButton(GuiUtilities.createImageIcon("polyline.gif"), null, Canvas.MODE.POLYLINE.toString(), Canvas.MODE.POLYLINE.toString(), this);
		g.add(b); this.add(b);
		b = GuiUtilities.createButton(GuiUtilities.createImageIcon("rectangle.gif"), null, Canvas.MODE.RECTANGLE.toString(), Canvas.MODE.RECTANGLE.toString(), this);
		g.add(b); this.add(b);
		b = GuiUtilities.createButton(GuiUtilities.createImageIcon("polygon.gif"), null, Canvas.MODE.POLYGON.toString(), Canvas.MODE.POLYGON.toString(), this);
		g.add(b); this.add(b);
		b = GuiUtilities.createButton(GuiUtilities.createImageIcon("eraser.gif"), null, Canvas.MODE.ERASER.toString(), Canvas.MODE.ERASER.toString(), this);
		g.add(b); this.add(b);
	}

	/**
	 * enable/disable the buttons in the panel
	 * @param isEnabled - true if enabling the buttons, false otherwise
	 */
	protected void setButtons(boolean isEnabled) {
		//get all the components in the panel
		Component[] components = getComponents();
		//iterate through, setting buttons to enabled/disabled
		for(int i = 0; i < this.getComponentCount(); i++) {
			if(components[i] instanceof JButton) {
				components[i].setEnabled(isEnabled);
			}
		}
	}
	
	/**
	 * change the font and propagate to sub windows
	 * @param font - the font to change to
	 */
	public void updateFont(Font font) {
		//get all the components in the panel
		Component[] components = getComponents();
		//iterate through, updating fonts
		for(int i = 0; i < this.getComponentCount(); i++) {
			if(components[i] instanceof JButton) {
				components[i].setFont(font);
			}
		}
	}
	
	/**
	 * called when an action is triggered
	 * @param action - the action that triggered this method call
	 */
	public void actionPerformed(ActionEvent action) {
		String arg = action.getActionCommand().trim();
		canvas.setUserMode(Canvas.MODE.valueOf(arg)); 
	}
}
