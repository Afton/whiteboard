/**
 * File: FontDialog.java
 * Author: Kyle Porter
 * Date: Sept 30th, 2006
 */

package whiteboard.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import whiteboard.gui.startlist.ConnectWindow;
import whiteboard.gui.whiteboard.WhiteboardWindow;

/**
 * This class is used for changing the font of the given window. If the ConnectionWindow is used,
 * any WhiteboardWindows that are launched after will have the same config as the ConnectionWindow. 
 */
@SuppressWarnings("serial")
public class FontDialog extends BasicDialog {
	/** the combobox for the font size */
	private JComboBox fontSizeCombo;
	/** the combobox for the font style */
	private JComboBox fontStyleCombo;
	/** the combobox for the font family */
	private JComboBox fontFamilyCombo;

	/** the parent window for this dialog */
	private BasicFrame window;

	/**
	 * constructor
	 * @param window - the parent window of this dialog
	 */
	public FontDialog(ConnectWindow window) {
		super(window, "Font Properties", true);
		initialize(window);
	}
	
	/**
	 * constructor
	 * @param window - the parent window of this dialog
	 */
	public FontDialog(WhiteboardWindow window) {
		super(window, "Font Properties", true);
		initialize(window);
	}

	/**
	 * common initialization for constructors
	 * @param wbWindow - parent window of this dialog
	 */
	private void initialize(BasicFrame wbWindow) {
		this.window = wbWindow;
		this.setFont(wbWindow.getConfig().getFont());

		initializeLayout();
		//select the correct combobox items for the window font
		initializeVariables();
		
		super.packCenterOpen();
	}

	/** initialize any variables needed for dialog start */
	private void initializeVariables() {
		Font font = this.getFont();
		fontSizeCombo.setSelectedItem(new Integer(font.getSize()));
		fontFamilyCombo.setSelectedItem(font.getFamily());

		switch(font.getStyle()) {
			case Font.BOLD:
				fontStyleCombo.setSelectedItem("Bold"); break;
			case Font.ITALIC:
				fontStyleCombo.setSelectedItem("Italic"); break;
			default:
				fontStyleCombo.setSelectedItem("Plain");
		}
	}

	/** initialize and set the layout for the dialog */
	@SuppressWarnings("boxing")
	private void initializeLayout() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		//add the font size spinner
		JPanel panel = new JPanel(new BorderLayout());
		Integer[] fontSizes = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
		fontSizeCombo = new JComboBox(fontSizes);
		fontSizeCombo.setPreferredSize(new Dimension(150, 23));
		fontSizeCombo.setRenderer(new ComboBoxRenderer());
		fontSizeCombo.setFont(this.getFont());
		JLabel label = new JLabel("Font Size: ");
		label.setFont(this.getFont());
		panel.add(label, BorderLayout.WEST);
		panel.add(fontSizeCombo, BorderLayout.EAST);
		mainPanel.add(panel);
		
		//add the font style spinner
		panel = new JPanel(new BorderLayout());
		String[] fontStyles = {"Bold", "Italic", "Plain"};
		fontStyleCombo = new JComboBox(fontStyles);
		fontStyleCombo.setPreferredSize(new Dimension(150, 23));
		fontStyleCombo.setRenderer(new ComboBoxRenderer());
		fontStyleCombo.setFont(this.getFont());
		label = new JLabel("Font Style: ");
		label.setFont(this.getFont());
		panel.add(label, BorderLayout.WEST);
		panel.add(fontStyleCombo, BorderLayout.EAST);
		mainPanel.add(panel);
		
		//add the font style spinner
		panel = new JPanel(new BorderLayout());
		String[] fontFamilies = {"Arial", "Times New Roman", "Verdana"};
		fontFamilyCombo = new JComboBox(fontFamilies);
		fontFamilyCombo.setPreferredSize(new Dimension(150, 23));
		fontFamilyCombo.setRenderer(new ComboBoxRenderer());
		fontFamilyCombo.setFont(this.getFont());
		label = new JLabel("Font Family: ");
		label.setFont(this.getFont());
		panel.add(label, BorderLayout.WEST);
		panel.add(fontFamilyCombo, BorderLayout.EAST);
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

	/** inherited method, handles OK button press */
	protected boolean actionButton() {
		String fontStyleStr = fontStyleCombo.getSelectedItem().toString();
		int fontStyle = Font.PLAIN;
		if(fontStyleStr.equals("Bold"))
			fontStyle = Font.BOLD;
		else if(fontStyleStr.equals("Italic"))
			fontStyle = Font.ITALIC;
		window.getConfig().setFont(new Font(fontFamilyCombo.getSelectedItem().toString(), fontStyle, Integer.parseInt(fontSizeCombo.getSelectedItem().toString())));
		window.updateFont(window.getConfig().getFont());
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
	
	/** renderer to set combobox alignment to right */
	private class ComboBoxRenderer extends JLabel implements ListCellRenderer {
		public ComboBoxRenderer() {
			setOpaque(true);
			setHorizontalAlignment(JLabel.RIGHT);
			setVerticalAlignment(CENTER);
		}

		/**
		* This method finds the image and text corresponding
		* to the selected value and returns the label, set up
		* to display the text and image.
		*/
		public Component getListCellRendererComponent(JList list, Object value, @SuppressWarnings("unused") int index, boolean isSelected, @SuppressWarnings("unused") boolean cellHasFocus) {
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setText(value.toString() + " ");
			return this;
		}
	}
}