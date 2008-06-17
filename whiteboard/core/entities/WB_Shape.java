/**
 * 
 */
package whiteboard.core.entities;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * @author Afton
 *
 */
public abstract class WB_Shape {
	
	@SuppressWarnings("unused")
	/*** used for making selections */
	protected static final float LINE_SELECT_SENSITIVITY = 0.35f;
	
	protected boolean isOnLine(Point test, Point start, Point end)
	{
		// special case: slope = infinity
		if(start.x == end.x) {
			//return true if point on line between two y values
			return ((test.x == start.x) && (test.y <= Math.max(start.y, end.y)) && (test.y >= Math.min(start.y, end.y))) ? true : false;
		} else if(getBounds().contains(test)) {
			//point is within bounds of line, so
			//using parametric equations, determine if point is on the line
			//P = start + t(end - start)    0 <= t <= 1
			float tx = ((float) (test.x - start.x)) / ((float) (end.x - start.x));
			float ty = ((float) (test.y - start.y)) / ((float) (end.y - start.y));
			return ((Math.abs(tx) <= 1) && (Math.abs(ty) <= 1) && (Math.abs(tx - ty) < LINE_SELECT_SENSITIVITY)) ? true : false;
		}
		return false;
	}
	
	private HashMap<String, String> attributes = new LinkedHashMap<String,String>();
	/** hashcode for this shape */
	protected int hcode;

	/***
	 * Should be used only during object re-creation from byte[]
	 * @param hcode sets the hashcode for this object to hcode
	 */
	public final void setHashCode(int hcode) {
		this.hcode = hcode;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * move all points by specified dx and dy
	 * @param dx - amount to move in x axis
	 * @param dy - amount to move in y axis
	 */
	public abstract void translate(int dx, int dy);
	
	
	/***
	 * 
	 * @param g the graphics object that the object will draw on. 
	 */
	protected abstract void localDraw(Graphics2D g);

	
	public void draw(Graphics2D g) {
		boolean coloured = containsAttribute("WB_COLOUR");
		
		if (coloured) {
			synchronized (this) {
			Color col = g.getColor();
			g.setColor(new Color( Integer.parseInt(getAttribute("WB_COLOUR")))) ;
			localDraw(g);	
			g.setColor(col);
			}
		} else {
			localDraw(g);
		}
	}
	
	/***
	 * 
	 * @return all the attributes in Map form. 
	 */
	public final HashMap<String, String> getAttributes() {
		return attributes;
	}
	
	/***
	 * 
	 * @param key set the attribute with value 'key'
	 * @param val to the value give by 'val'. This may 
	 *  	  overwrite preexisting values
	 */
	public final void putAttribute(String key, String val) {
		attributes.put(key, val);
	}
	
	/***
	 * 
	 * @param key 
	 * 	
	 * @return the value of the attribute specified by 'key'
	 */
	public final String getAttribute(String key) {
		return attributes.get(key);
	}
	
	/***
	 * 
	 * @param key : an attribute name to test.
	 * @return : true if the attribute exists. 
	 */
	public final boolean containsAttribute(String key) {
		return attributes.containsKey(key);
	}
	
	/***
	 * 
	 * @return The object shrunken to a byte-array following some fairly particular rules
	 * 			This is why single inheritance sucks monkey butt. 
	 */
	public abstract ArrayList<Byte> pack();
		

	public final void setAttributes(HashMap<String, String> attributes) {
		this.attributes = attributes;
	}
	
	/***
	 * 
	 * @param p
	 * @return true if the point p is inside/on the WB_displayObject
	 */
	public abstract boolean isOnPerimeter(Point p);

	/***
	 * 
	 * @param r
	 * @return true iff the rectangle intersects with the geometry. 
	 */
	public abstract boolean intersects(Rectangle r);
	
	public abstract Rectangle getBounds();
	
	/***
	 * This draws the "selected" version of the object
	 * @param g
	 */
	public void drawBounds(Graphics g)
	{
		Rectangle r = this.getBounds();
		
		// draw the points of the bounding box. note: for long lines
		// this may not be ideal UI
		g.drawOval((int)r.getMinX(), (int)r.getMinY(), 3, 3);
		g.drawOval((int)r.getMinX(), (int)r.getMaxY(), 3, 3);
		g.drawOval((int)r.getMaxX(), (int)r.getMinY(), 3, 3);
		g.drawOval((int)r.getMaxX(), (int)r.getMaxY(), 3, 3);
	}
	
	
	public abstract String toString();
	
	public boolean equals(WB_Shape p)
	{
		return p.hashCode() == this.hashCode();
	}
}
