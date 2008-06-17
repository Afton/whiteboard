package whiteboard.core.entities;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

/***
 * 
 * @author Afton 
 */
public class WB_Point extends WB_Shape {

	protected Point loc = null;

	public WB_Point(int x, int y)
	{
		loc = new Point(x,y);
		setHashCode(super.hashCode());
	}
	
	public WB_Point(Point p)
	{
		loc = new Point(p);
		setHashCode(super.hashCode());
	}	
	
	public void setPoint(Point p)
	{
		loc = new Point(p);
	}
	
	public ArrayList<Byte> pack()
	{
		// pack up base class and set to this.type
		ArrayList<Byte> contents = new ArrayList<Byte>();
		contents.addAll(BytePacker.convertIntToBytes(hashCode()));
		contents.add(new Integer(ShapeConstants.SHAPE_TYPE.POINT_TYPE.ordinal()).byteValue());
		BytePacker.packAttributes(contents, getAttributes());
		
		// redundant, but makes it consistent with other shapes
		contents.addAll(BytePacker.convertIntToBytes(1));
		contents.addAll(BytePacker.convertPointToBytes(loc));
		
		return contents;
	}	

	public void localDraw(Graphics2D g) 
	{
			g.fillOval(loc.x, loc.y, 3,3);
	}

	public boolean isOnPerimeter(Point p) {
		// recall that the bounds for a point are larger than the point for UIs sake.
		return getBounds().contains(p);
	}

	public boolean intersects(Rectangle r) {		
		return r.contains(loc);
	}

	public Rectangle getBounds() {
		return new Rectangle(loc.x - 2, loc.y - 2, 5, 5);
	}

	@Override
	public void translate(int dx, int dy) {
		loc.translate(dx, dy);
	}
	
	@Override
	public String toString()
	{
		return "[[POINT Loc: " + loc + ", Attributes: " + this.getAttributes().toString() + "]]";
	}
	

	@Override
	public int hashCode()
	{
		return hcode;
	}
}