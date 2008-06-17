package whiteboard.core.entities;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Afton
 * Basic polygon. Remember that the order of points is important. 
 * In order to avoid certain edge cases, we define a polygon such that the
 * last point is the same as the first, and don't store the last point
 * This means the the polygon with points (0,1) (0,0) (1,0) is a triangle
 * with the last point at (0,1). 
 */
@SuppressWarnings("serial")
public class WB_Polygon extends WB_Shape {

	// points are held by the polygon
	//Polygon poly = new Polygon();
	
	private WB_Polyline perimeter = new WB_Polyline();
	
	public WB_Polygon()
	{
		setHashCode(super.hashCode());
	}
	
	public WB_Polygon(List<Point> ps)
	{
		this();
		if(ps != null) {
			for (Point p : ps) {
				perimeter.addPoint(p);
			}
		}
	}
	
	public void addPoint(Point p)
	{
		perimeter.addPoint(p);
		//poly.addPoint(p.x, p.y);
	}
	
	public ArrayList<Byte> pack()
	{
		// pack up base class and set to this.type
		ArrayList<Byte> contents = new ArrayList<Byte>();
		contents.addAll(BytePacker.convertIntToBytes(hashCode()));
		contents.add(new Integer(ShapeConstants.SHAPE_TYPE.POLYGON_TYPE.ordinal()).byteValue());
		BytePacker.packAttributes(contents, getAttributes());
	
		// add additions to base class		
		contents.addAll(BytePacker.convertIntToBytes(perimeter.path.size()));
		
		for (Point p : perimeter.path)
		{
			contents.addAll(BytePacker.convertPointToBytes(p));
		}		
		return contents;
	}


	public void localDraw(Graphics2D g) {
		perimeter.draw(g);
		Point start = perimeter.path.get(0);
		Point end = perimeter.path.get(perimeter.path.size()-1);
		g.drawLine(end.x, end.y, start.x, start.y);
		
	}

	public boolean isOnPerimeter(Point p) {
		// tests is p is the perimeter by testing the polyline
		// and then testing the closing line.
		return perimeter.isOnPerimeter(p) || isOnLine(p,
					perimeter.path.get(perimeter.path.size()-1), 
					perimeter.path.get(0));		
	}

	/***
	 * As with isOnPerimeter, most of the work is delegated to the polyline
	 */
	public boolean intersects(Rectangle r) {
		return perimeter.intersects(r) ||
			(new WB_Line(perimeter.path.get(perimeter.path.size()-1), 
					perimeter.path.get(0)).intersects(r));
		
	}

	public Rectangle getBounds() {
		return perimeter.getBounds();
	}

	@Override
	public void translate(int dx, int dy) {
		perimeter.translate(dx, dy);
	}
	
	@Override
	public String toString()
	{
		String asString = perimeter.toString();
		return asString.replaceFirst("POLYLINE", "POLYGON");

	}
	

	@Override
	public int hashCode()
	{
		return hcode;
	}
}