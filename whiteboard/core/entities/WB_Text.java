package whiteboard.core.entities;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

/***
 * 
 * @author Afton
 *
 */
public class WB_Text extends WB_Point {
	private FontMetrics fontMetrics;
	private String text; 
	
	public WB_Text(Point p, String init)
	{
		super(p);
		text = init;
	}
	
	
	public WB_Text(int arg0, int arg1) {
		super(arg0, arg1);
		text = "";
	}

	public ArrayList<Byte> pack() {
		ArrayList<Byte> contents = new ArrayList<Byte>();
		contents.addAll(BytePacker.convertIntToBytes(hashCode()));
		contents.add(new Integer(ShapeConstants.SHAPE_TYPE.TEXT_TYPE.ordinal()).byteValue());
		BytePacker.packAttributes(contents, getAttributes());

		contents.addAll(BytePacker.convertIntToBytes(text.length()));
		contents.addAll(BytePacker.convertStringToBytes(text));
		contents.addAll(BytePacker.convertIntToBytes(1));
		contents.addAll(BytePacker.convertPointToBytes(loc));
		
		return contents;
	}

	public void localDraw(Graphics2D g)
	{
		fontMetrics = g.getFontMetrics();
		String[] lines = text.split("\n");
		for(int i = 0; i < lines.length; ++i) {
			g.drawString(lines[i],loc.x, loc.y + (i+1) * g.getFontMetrics().getHeight());
		}
	}

	public boolean isOnPerimeter(@SuppressWarnings("unused") Point p) {
		return false; // our live will be so much easier if we only 
		// allow text selection with a selection rectangle.
		//return getBounds().contains(p);
	}

	public Rectangle getBounds() {
		String[] textLines = text.split("\n");
		int width = 0;
		//get the maximum width of the lines
		for(String line : textLines) {
			width = Math.max(width, fontMetrics.stringWidth(line));
		}
		//get the total height
		int height = textLines.length * (fontMetrics.getHeight() + 1);
		return new Rectangle(loc.x, loc.y, width, height);
	}
	
	public String toString()
	{
		String asString = super.toString();
		return asString.replaceFirst("POINT", "TEXT");
	}
}