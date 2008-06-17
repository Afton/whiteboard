package whiteboard.core.entities;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/***
 * 
 * @author Afton
 * 
 *
 */
public class WB_Polyline extends WB_Shape {

	protected ArrayList<Point> path = new ArrayList<Point>();
	

	public WB_Polyline()
	{
		setHashCode(super.hashCode());
	}
	
	public WB_Polyline(List<Point> ps)
	{
		this();
		path.addAll(ps);
	}
	
	public void addPoint(Point p)
	{
		this.path.add(p);
	}
	

	public ArrayList<Byte> pack()
	{
		// pack up base class and set to this.type
		ArrayList<Byte> contents = new ArrayList<Byte>();
		contents.addAll(BytePacker.convertIntToBytes(hashCode()));
		contents.add(new Integer(ShapeConstants.SHAPE_TYPE.POLYLINE_TYPE.ordinal()).byteValue());
		BytePacker.packAttributes(contents, getAttributes());
		
		// add additions to base class
		int path_size = path.size();
		contents.addAll(BytePacker.convertIntToBytes(path_size));
		for (Point xy : path)
		{
			contents.addAll(BytePacker.convertPointToBytes(xy));
		}
		
		return contents;
	}

	/* (non-Javadoc)
	 * @see whiteboard.core.entities.WB_DisplayObject#intersects(java.awt.geom.Rectangle2D)
	 */
	public boolean intersects(Rectangle r)
	{
		boolean result = false;
		for (int i=0; i<path.size()-1 && result == false; ++i)
		{
			Point p1 = path.get(i);
			Point p2 = path.get(i+1);
			if (new Line2D.Double(p1,p2).intersects(r))
			{
				result = true;
			}
		}
		return result;
	}

	
	public Rectangle getBounds() {
		if (path.isEmpty())
			return null;
		else if (path.size() == 1) 
			return new Rectangle2D.Double(path.get(0).getX(),path.get(0).getX(),0,0).getBounds();
		else
		{
			// collect the upper-left, lower-right corners of the bound
			int x0=Integer.MAX_VALUE, y0=Integer.MAX_VALUE, x1=Integer.MIN_VALUE, y1=Integer.MIN_VALUE;
			for (Point p : path)
			{
				int x = (int) p.getX();
				int y = (int) p.getY();
				
				if (x < x0)
					x0 = x;
				else if (x > x1)
					x1 = x;
				
				if (y < y0)
					y0 = y;
				else if (y > y1)
					y1 = y;
			}
			return new Rectangle(x0,y0, (x1-x0), (y1-y0)).getBounds();
		}
	}

	@Override
	public void translate(int dx, int dy) {
		for (Point p : path)
		{
			p.translate(dx, dy);
		}
	}

	public void localDraw(Graphics2D g) {
		for (int i=0; i< path.size()-1; ++i)
		{
			Point2D p0 = path.get(i);
			Point2D p1 = path.get(i+1);
			g.drawLine((int)p0.getX(), (int)p0.getY(), (int)p1.getX(), (int)p1.getY());			
		}
	}

	
	public boolean isOnPerimeter(Point p) {
		boolean result = false;
		for (int i=0; i< path.size()-1 && !result ; ++i)
		{
			result = isOnLine(p, path.get(i), path.get(i+1));
		}
		return result;
	}
	
	@Override
	public String toString()
	{
		return "[[POLYLINE: Path: " + path.toString() + ", Attributes: " + this.getAttributes().toString() + "]]";
	}
	

	@Override
	public int hashCode()
	{
		return hcode;
	}
}