package whiteboard.core.entities;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;

public class WB_Line extends WB_Shape {
	
	
	private Point start = null;
	private Point end = null;
	
	public WB_Line() {
		setHashCode(super.hashCode());
	}
	
	public WB_Line(Point p0, Point p1)
	{
		this();
		start = p0;
		end = p1;
	}
	
	public WB_Line(int x0, int y0, int x1, int y1)
	{
		this(new Point(x0,y0), new Point(x1, y1));
	}

	public ArrayList<Byte> pack()
	{
		// pack up base class and set to this.type
		ArrayList<Byte> contents = new ArrayList<Byte>();
		contents.addAll(BytePacker.convertIntToBytes(hashCode()));
		contents.add(new Integer(ShapeConstants.SHAPE_TYPE.LINE_TYPE.ordinal()).byteValue());
		BytePacker.packAttributes(contents, getAttributes());
		
		// add additions to base class
		// 2 == number of points. begin, end
		contents.addAll(BytePacker.convertIntToBytes(2));
		contents.addAll(BytePacker.convertPointToBytes(start));
		contents.addAll(BytePacker.convertPointToBytes(end));
				
		return contents;
	}
	

	public void localDraw(Graphics2D g) {
		g.drawLine(start.x, start.y, end.x, end.y);
	}


	public boolean isOnPerimeter(Point p) {
		return isOnLine(p, start, end);
	}


	public boolean intersects(Rectangle r) { 
		return r.intersectsLine(new Line2D.Double(start, end)) || r.contains(this.getBounds());
	}
	
	public Rectangle getBounds() {
		if((start == null) || (end == null)) {
			return null;
		}
		int width = Math.abs(start.x - end.x);
		int height = Math.abs(start.y - end.y);
		return new Rectangle(Math.min(start.x, end.x), Math.min(start.y, end.y), width, height);
	}

	@Override
	public void translate(int dx, int dy) {
		start.translate(dx, dy);
		end.translate(dx, dy);
	}

	@Override
	public String toString()
	{
		return "[[LINE: start: " + start +", end: " + end +", Attributes: " + this.getAttributes().toString() + "]]";
	}
	
	@Override
	public int hashCode()
	{
		return hcode;
	}
	
}