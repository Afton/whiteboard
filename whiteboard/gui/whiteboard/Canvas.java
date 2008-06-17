/**
 * File: Canvas.java
 * Author: Kyle Porter
 * Date: Sept 25th, 2006
 */

package whiteboard.gui.whiteboard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import whiteboard.core.entities.BytePacker;
import whiteboard.core.entities.ShapeConstants;
import whiteboard.core.entities.WB_Line;
import whiteboard.core.entities.WB_Point;
import whiteboard.core.entities.WB_Polygon;
import whiteboard.core.entities.WB_Polyline;
import whiteboard.core.entities.WB_Shape;
import whiteboard.core.entities.WB_Text;
import whiteboard.core.exceptions.TransactionManagerAlreadySetException;
import whiteboard.core.transaction.LocalTransactionManager;
import whiteboard.core.transaction.ShapePacket;

/**
 * This class contains the Canvas (whiteboard) that allows the user to do 
 * all their drawing.
 */
@SuppressWarnings("serial")
public class Canvas extends JPanel implements ActionListener, Printable, MouseListener, MouseMotionListener, KeyListener {
	private static final Color NO_DRAW_COLOR = new Color(.92f, .92f, .92f);
	private static final Color DRAW_COLOR = Color.WHITE;

	/** true if anti-aliasing is enabled */
	private boolean isAntiAliasingEnabled;
	
	/** main program window holding this canvas */
	private WhiteboardWindow window;
	private NetworkShapePollThread netPollThread;

	/** the current list of selections. */
	private List<WB_Shape> selections = new ArrayList<WB_Shape>();
	/** the list of shapes in the canvas */
	private List<WB_Shape> WBObjs = new LinkedList<WB_Shape>();
	/** the points the user is currently adding (for not committed shapes) */
	private ArrayList<Point> points = new ArrayList<Point>();
	
	/** used to calculate the drawing to the current mouse position */
	private Point mouseMovePoint = new Point(0,0);
	/** boolean to indicate if user has dragged shapes */
	private boolean hasMoved = false;

	/** only process mouse/keyboard events when this is true */
	private boolean allowUserInput = false;

	private WB_Line selectionRectangle = null;
	private Point selectPoint = null;
	
	public enum MODE { SELECT, LINE, POLYGON, POLYLINE, TEXT, POINT, RECTANGLE, ERASER, FREEHAND, TRANSLATE };
	private MODE userMode = MODE.SELECT;

	// TEXT TYPING STUFF
	/** 
	 * Text that is being typed before being committed to an object. This is null when not
	 * typing, empty when no text has been typed, and contains typed text otherwise.
	 */
	private String typedDrawingText = null;

	/** All pulling and pushing is done through the manager. */
	private LocalTransactionManager man = null;

	private JPopupMenu popupMenu;
	
	/**
	 * constructor
	 * @param window - the window the canvas is in
	 */
	public Canvas(WhiteboardWindow window) {
		this.window = window;		
		init();
	}

	/*
	 * this isn't the only way to load it. We can hide more if we decide it's worth it
	 */
	public Canvas(WhiteboardWindow window, List<WB_Shape> objs)
	{
		this.window = window;
		init();
		WBObjs.addAll(objs);
	}

	private void init() {
		initializeLayout();
		initializeVariables();
	}
	
	/** create and set the layout of the canvas */
	private void initializeLayout() {
		this.setFocusable(true);
		if(this.allowUserInput)
			this.setBackground(Canvas.DRAW_COLOR);
		else
			this.setBackground(Canvas.NO_DRAW_COLOR);
		this.setPreferredSize(new Dimension(5000, 5000));
	}

	/** initialize and set any variables that are needed */
	private void initializeVariables() {
		//init the popupMenu menu
		popupMenu = new JPopupMenu();
		//add the canvas mouse listener
		this.addMouseListener(this);
		//add the canvas mouse motion listener
		this.addMouseMotionListener(this);
		this.addKeyListener(this);
		//ensure that typed uncommitted text is committed
		this.addFocusListener(new FocusListener() {
			public void focusGained(@SuppressWarnings("unused") FocusEvent arg0) {}
			public void focusLost(@SuppressWarnings("unused") FocusEvent arg0) {
				commitTypedText();
			}
		});

		//anti-aliasing starts as false
		isAntiAliasingEnabled = false;

		netPollThread = new NetworkShapePollThread(this);
		netPollThread.start();
	}

	/** stop the network polling thread */
	protected void stopPollThread() {
		netPollThread.quit();
	}

	/** scales the window to show the drawing correctly */
	protected void autoscale() {
		//TODO: scale the window (ie zoom in or out)
	}

	/** 
	 * get the start location for the text cursor
	 * @param textStartPoint - the start position for the given text
	 * @param text - the text typed so far
	 * @param fm - the fontmetrics used for the text
	 * @return the upper point for the location of the text cursor
	 */
	private Point getCursorStartPoint(Point textStartPoint, String text, FontMetrics fm) {
		if(text == null || text.equals(""))
			return new Point(textStartPoint.x, textStartPoint.y + 2);
		String[] textLines = text.split("\n");
		int extraLine = 0;
		int extraXWidth = fm.stringWidth(textLines[textLines.length-1]);
		//if the text ends in a newline, it should count that line but split omits it, so count it here
		//also, that means we should start the line at the beginning again, so reset the xWidth
		if(text.endsWith("\n")) {
			++extraLine;
			extraXWidth = 0;
		}
		return new Point(textStartPoint.x + extraXWidth + 2, textStartPoint.y + (textLines.length-1+extraLine) * (fm.getHeight() + 1) + 2);
	}

	/** commits typed text, if there is any waiting */
	public void commitTypedText() {
		//if there is a text to be committed, do it here
		if(this.typedDrawingText != null) {
			if(typedDrawingText.trim().equals("")) {
				typedDrawingText = null;
			} else {
				addText(points, typedDrawingText);
				typedDrawingText = null;
				repaint();
			}
		}
	}

	public boolean isAntiAliasingEnabled() {
		return isAntiAliasingEnabled;
	}

	public void setAntiAliasingEnabled(boolean isAntiAliasingEnabled) {
		this.isAntiAliasingEnabled = isAntiAliasingEnabled;
		this.repaint();
	}
	
	/**
	 * displays a message with given text
	 * @param title - the title of the dialog box
	 * @param msg - the message to be shown in the box
	 */
	public void showMessage(String title, String msg) {
		window.showMessage(title, msg);
	}
	
	/**
	 * Paints the components. Overrides Component method.
	 * @param g - the graphics object to paint the canvas
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if(isAntiAliasingEnabled()) {
			//enable anti-aliasing
		    ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}

		//draw any other things here
		for(int i = 0; i < WBObjs.size(); ++i) {
			if(WBObjs.get(i) != null)
				WBObjs.get(i).draw((Graphics2D) g);
		}

		g.setColor(window.getConfig().getUserColour());
		if(!points.isEmpty()) {
			switch(this.getUserMode()) {
				case RECTANGLE:
					//draw the rectangle (only two points in points list)
					g.drawRect(Math.min(points.get(0).x, points.get(1).x), Math.min(points.get(0).y, points.get(1).y), Math.abs(points.get(0).x - points.get(1).x), Math.abs(points.get(0).y - points.get(1).y));
					break;
				case TEXT:
					if(typedDrawingText == null)
						return;
					//draw the cursor
					Point cursorStart = getCursorStartPoint(points.get(0), typedDrawingText, g.getFontMetrics());
					g.drawLine(cursorStart.x, cursorStart.y, cursorStart.x, cursorStart.y + g.getFontMetrics().getHeight());
					//draw the text
					String[] lines = typedDrawingText.split("\n");
					for(int i = 0; i < lines.length; ++i) {
						g.drawString(lines[i], points.get(0).x, points.get(0).y + (i+1) * g.getFontMetrics().getHeight());
					}
					break;
				default:
					//draw the current point-list
					for (int i=0; i<points.size()-1; ++i) {
						Point p0 = points.get(i);
						Point p1 = points.get(i+1);
						
						g.drawLine(p0.x, p0.y, p1.x, p1.y);
					}
					// and draw to the current mouse-position, unless you're free-handing;
					if (userMode != MODE.FREEHAND )
						g.drawLine(points.get(points.size()-1).x, points.get(points.size()-1).y, mouseMovePoint.x, mouseMovePoint.y);
			}
		}

		
		// draw the current selection if there is one
		for (WB_Shape x : selections)
		{
			x.drawBounds(g);
		}
	
		// and draw the selection rectangle, but only if still in selectPoint mode
		if (selectionRectangle != null && MODE.SELECT == this.getUserMode())
		{
			Rectangle r = selectionRectangle.getBounds();
			g.drawRect(r.x, r.y, r.width, r.height);
		}
	}

	private WB_Shape findShapeByReference(int referenceNum) {
		for (int i =0; i< WBObjs.size(); ++i)
		{
			if (WBObjs.get(i).hashCode() == referenceNum)
			{
				return WBObjs.get(i);
			}
		}
		return null;
	}

	protected synchronized void addPacket(ShapePacket b) {
		// nothing to do if b is null
		if (b == null)
			return;
		
		// parse and act on packetValue.
		switch (b.type)
		{
			case OBJECT_CREATION:
			{
				WB_Shape n = BytePacker.createWB_ShapeFromShapePacket(b);
				if (null != n && !WBObjs.contains(n)) {
					WBObjs.add(n);
					System.err.println( n.toString() );
				}
				break;
			}
			case OBJECT_DELETION:
			{
				WBObjs.remove(findShapeByReference(b.objectReference));
				break;
			}
			case OBJECT_GEOM_MODIFICATION:
			{
				int offset = ShapeConstants.PACKET_EXP_HEADER_OFFSET + Integer.SIZE/8;
				byte transType = b.packet[offset++];
				WB_Shape s = findShapeByReference(b.objectReference);
				if(s == null)
					return;
				int arg1 = BytePacker.convertBytesToInt(b.packet, offset); offset += Integer.SIZE/8;
				int arg2 = BytePacker.convertBytesToInt(b.packet, offset);
				if (ShapeConstants.GEOM_TRANSFORM_TYPE.TRANSLATION.ordinal() == transType) {
					s.translate(arg1, arg2);
				} else if (ShapeConstants.GEOM_TRANSFORM_TYPE.ROTATION.ordinal() == transType) {
					
				} else if (ShapeConstants.GEOM_TRANSFORM_TYPE.SCALE.ordinal() == transType) {
					
				}
			}
			default:
				break;
		}
		repaint();
	}

	/** prints the canvas */
	public void print() {
		PrinterJob printJob = PrinterJob.getPrinterJob();
		printJob.setPrintable(this);
		if(printJob.printDialog()){
			try{
				printJob.print();
			} catch (Exception ex){
				showMessage("Error", ex.getMessage());
			}
		}
	}

	/** method needed for Printable interface */
	public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
		//TODO does this even work?
		if(pi >= 1){
			return Printable.NO_SUCH_PAGE;	
		}
		return Printable.PAGE_EXISTS;
	}

	public WhiteboardWindow getWindow() {
		return window;
	}

	/***
	 * POST: this.selections is non-null
	 * @param selections
	 */
	public synchronized void setSelectionShapes(List<WB_Shape> selections) {
		this.selections = selections;
		if (selections == null) {
			System.err.println("ERROR: Selection made null by canvas.setSelection()");
		}
	}

	/***
	 * 
	 * @param p the Point that will be tested for containment in all objects
	 */
	synchronized private List<WB_Shape> getSelected(Point p)
	{
		LinkedList<WB_Shape> selected = new LinkedList<WB_Shape>();
		for (WB_Shape obj : WBObjs)			
		{
			if (obj.isOnPerimeter(p))
				selected.add(obj);
		}
		return selected;
	}
	
	/***
	 * 
	 * @param p the rectangle that will be tested for intersection in all objects
	 */
	synchronized private List<WB_Shape> getSelected(Rectangle r)
	{
		LinkedList<WB_Shape> selected = new LinkedList<WB_Shape>();
		for (WB_Shape obj : WBObjs)			
		{
			if (obj.intersects(r)) // could be obj.contains() just as sensibly
				selected.add(obj);
		}
		return selected;
	}

	/** delete all the selected shapes */
	private void deleteSelected() {
		if((selections != null) && !selections.isEmpty()) {
			for(WB_Shape shape : selections) {
				deleteShape(shape);
			}
			selections.clear();
			repaint();
		}
	}

	/**
	 * delete the given shape from the whiteboard network
	 * @param shape - the shape to be deleted
	 */
	synchronized private void deleteShape(WB_Shape shape) {
		WBObjs.remove(shape);
		man.pushToNetwork(BytePacker.convertTo_byte_Array(BytePacker.convertIntToBytes(shape.hashCode())), ShapeConstants.WB_REQUEST_TYPE.OBJECT_DELETION);
	}

	/**
	 * re-initialize the whiteboard with the current mode (i.e. drop current
	 * drawing object and start fresh) 
	 * @param userMode the user_mode to set
	 */
	synchronized public void setUserMode(MODE newMode) 
	{
		commitTypedText();
		userMode = newMode;
		points.clear();	
		selections.clear();
		repaint();
		return ;
	}
	
	synchronized public MODE getUserMode()
	{
		return userMode;
	}

	/***
	 * @param manager
	 */
	public void setTransactionManager(LocalTransactionManager manager) throws TransactionManagerAlreadySetException{
		if (this.man == null)
			this.man = manager;
		else
			throw new TransactionManagerAlreadySetException();			
	}
	

	public synchronized LocalTransactionManager getTransactionManager() {
		return man;
	}
	
	private void addLine(List<Point> coords)
	{		
		addShape(new WB_Line(coords.get(0), coords.get(1)));
		coords.clear();
	}
	
	private void addPolyLine(List<Point> coords)
	{
		addShape(new WB_Polyline(coords));
		coords.clear();
	}
	
	private void addPolygon(List<Point> coords)
	{
		addShape(new WB_Polygon(coords));
		coords.clear();
	}
	
	private void addText(List<Point> coord, String value)
	{
		addShape(new WB_Text(coord.get(0), value));
		coord.clear();
	}
	
	private void addPoint(List<Point> coord)
	{
		addShape(new WB_Point(coord.get(0)));
		coord.clear();
	}

	private synchronized void addShape(WB_Shape shape)
	{
		shape.putAttribute("WB_COLOUR", Integer.toString(window.getConfig().getUserColour().getRGB()));
		WBObjs.add(shape);
		man.pushToNetwork(BytePacker.convertTo_byte_Array(shape.pack()), ShapeConstants.WB_REQUEST_TYPE.OBJECT_CREATION);
	}

	/**
	 * send packet to translate the shapes for other users
	 * @param shapes - list of the shapes
	 * @param dx - diff in x axis
	 * @param dy - diff in y axis
	 */
	private void sendTranslateShapes(List<WB_Shape> shapes, int dx, int dy) {
		sendTransformPacket(ShapeConstants.GEOM_TRANSFORM_TYPE.TRANSLATION, shapes, dx, dy);
	}

	/**
	 * send packet to scale the shapes for other users
	 * @param shapes - list of the shapes
	 * @param dx - diff in x axis
	 * @param dy - diff in y axis
	 */
	@SuppressWarnings("unused")
	private void sendScaleShapes(List<WB_Shape> shapes, int scaleX, int scaleY) {
		sendTransformPacket(ShapeConstants.GEOM_TRANSFORM_TYPE.SCALE, shapes, scaleX, scaleY);	
	}

	/**
	 * send packet to rotate the shape for other users
	 * @param shape - shape to rotate
	 * @param dx - diff in x axis
	 * @param dy - diff in y axis
	 */
	@SuppressWarnings("unused")
	private void sendRotateShape(WB_Shape shape, int theta) {
		List<WB_Shape> shapes = new ArrayList<WB_Shape>();
		shapes.add(shape);
		sendTransformPacket(ShapeConstants.GEOM_TRANSFORM_TYPE.ROTATION, shapes, theta, 0);	
	}

	private synchronized void sendTransformPacket(ShapeConstants.GEOM_TRANSFORM_TYPE transType, List<WB_Shape> shapes, int arg1, int arg2) {
		for(WB_Shape s : shapes) {
			ArrayList<Byte> contents = new ArrayList<Byte>();
			contents.addAll(BytePacker.convertIntToBytes(s.hashCode()));
			//add transform type
			contents.add(new Integer(transType.ordinal()).byteValue());
			contents.addAll(BytePacker.convertIntToBytes(arg1));
			contents.addAll(BytePacker.convertIntToBytes(arg2));
			man.pushToNetwork(BytePacker.convertTo_byte_Array(contents), ShapeConstants.WB_REQUEST_TYPE.OBJECT_GEOM_MODIFICATION);
		}
	}

	/**
	 * get the byte representation of all the objects currently in the canvas
	 * @return a list of lists, where each list contains the bytes for an individual object, from pack()
	 */
	public List<List<Byte>> getByteRepresentation() {
		List<List<Byte>> byteList = new ArrayList<List<Byte>>(WBObjs.size());
		for(WB_Shape shape : WBObjs) {
			ArrayList<Byte> packet = shape.pack();
			byteList.add(packet.subList(4, packet.size()));
		}
		return byteList;
	}

	/**
	 * load objects into the whiteboard
	 * @param byteList - list of lists, where each list is the bytes from an object, from pack()
	 */
	public void loadFromByteRepresentation(List<List<Byte>> byteList) {
		for(List<Byte> list : byteList) {
			WB_Shape n = BytePacker.createWB_ShapeFromPacket(BytePacker.convertTo_byte_Array(list), 0);
			if (null != n) {
				addShape(n);
			}
		}
	}

	/*
	 * POPUP MENUS
	 */

	/**
	 * show the correct popupMenu for the right click event
	 * @param arg0 - the mouse event from the mouse click
	 */
	protected void showPopup(MouseEvent arg0) {
		//get the selected item
		List<WB_Shape> list = getSelected(arg0.getPoint());
		if(list.isEmpty()) {
			popupCanvas(arg0.getPoint());
		} else {
			//FIXME: need more/better popup detection or things here
			WB_Shape obj = getSelected(arg0.getPoint()).get(0);
			popupShape(arg0.getPoint());
			if(obj instanceof WB_Text) {
				//etc
			} else if(obj instanceof WB_Polygon) {
				//etc etc etc
			}
		}
	}

	/**
	 * Make the popupMenu menu for white space (canvas).
	 */
	protected void popupCanvas(Point p) {
		popupMenu.removeAll();
		popupMenu.setBorder(BorderFactory.createTitledBorder("Canvas Options"));

		JMenuItem mItem = new JMenuItem("Autoscale");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		popupMenu.add(mItem);
		
		popupMenu.show(this, p.x, p.y);
	}

	/**
	 * Make the popupMenu menu for shape objects.
	 */
	protected void popupShape(Point p) {
		selectPoint = p;
		popupMenu.removeAll();
		popupMenu.setBorder(BorderFactory.createTitledBorder("Shape Options"));

		JMenuItem mItem = new JMenuItem("Delete Shape");
		mItem.setActionCommand(mItem.getText());
		mItem.addActionListener(this);
		popupMenu.add(mItem);

		popupMenu.show(this, p.x, p.y);
	}

	/*
	 * ACTION LISTENER
	 */

	public void actionPerformed(ActionEvent action) {
		String arg = action.getActionCommand().trim();
		if(arg.equals("Autoscale")) {
			autoscale();
		} else if(arg.equals("Delete Shape")) {
			setSelectionShapes(getSelected(selectPoint));
			selectPoint = null;
			deleteSelected();
		} else if(arg.equals("Inspect Shape (DEBUG)")) {
			WB_Shape shape = getSelected(selectPoint).get(0);
			showMessage("DEBUG", "Object Hashcode: " + shape.hashCode() + ", type: " + shape.toString());
		}
	}

	/*
	 * MOUSE LISTENER
	 */

	public void mouseClicked(@SuppressWarnings("unused") MouseEvent arg0) {}
	public void mouseEntered(@SuppressWarnings("unused") MouseEvent arg0) {}
	public void mouseExited(@SuppressWarnings("unused") MouseEvent arg0) {}

	public void mousePressed(MouseEvent arg0) {
		if(!allowUserInput) {
			return;
		}
		commitTypedText();
		//all events corresponding to mouse presses pass through.
		//get the focus on the canvas (for key presses)
		this.requestFocus();
		Point loc = arg0.getPoint();
		switch (userMode) {
			case SELECT :
				List<WB_Shape> selectedShapes = getSelected(loc);
				if(selections.isEmpty() || selectedShapes.isEmpty() || !selections.containsAll(selectedShapes)) {
					setSelectionShapes(getSelected(loc));
				}
				selectPoint = loc;
				break;			
			case LINE :
				if (points.isEmpty()) {
					points.add(loc);
				} else {
					points.add(loc);
					addLine(points);
				}
				break;
			case POLYGON :			
				// if it's a right-click, we've ended the polygon
				// don't include this click in the polygon!!
				if (arg0.getButton() == MouseEvent.BUTTON3) {
					addPolygon(points);
				} else {
					points.add(loc);
				}
				break;
			case POLYLINE :			
				// if it's a right-click, we've ended the polyline
				if (arg0.getButton() == MouseEvent.BUTTON3) {
					addPolyLine(points);
				} else {
					points.add(loc);
				}
				break;
			case TEXT :
				if (arg0.getButton() != MouseEvent.BUTTON3) {
					points.clear();
					points.add(loc);
					this.typedDrawingText = "";
					// defer creation until we get the text-string
				}
				break;
			case POINT :
				if (points.isEmpty()) {
					points.add(loc);
					addPoint(points);
				} else {
					System.err.println("Point clicked, with non-empty point array");
				}
				break;
			case RECTANGLE :
				points.add(loc);
				points.add(new Point(loc));
				break;
			case ERASER :
				setSelectionShapes(getSelected(loc));
				deleteSelected();
				break;
			case FREEHAND : 
				points.clear();
				points.add(loc);
				break;
			default:
				break;
		}		
		repaint();
	}

	public void mouseReleased(MouseEvent arg0) {
		if(!allowUserInput) {
			return;
		}
		Point loc = arg0.getPoint();
		switch (userMode) {
			case SELECT :
				if(SwingUtilities.isRightMouseButton(arg0)) {
					showPopup(arg0);
				} else {
					if(selections.isEmpty() || !hasMoved) {
						if ( loc.equals(selectPoint)) {
							setSelectionShapes(getSelected(loc));
						} else {
							setSelectionShapes(getSelected(new WB_Line(loc, selectPoint).getBounds()));					
						}
					} else {
						int dx = mouseMovePoint.x - selectPoint.x;
						int dy = mouseMovePoint.y - selectPoint.y;
						sendTranslateShapes(selections, dx, dy);
						hasMoved = false;
					}
				}
				break;
			case RECTANGLE:
				//create two missing points for polygon
				Point upperLeft = new Point(Math.min(points.get(0).x, points.get(1).x), Math.min(points.get(0).y, points.get(1).y));
				Point lowerRight = new Point(Math.max(points.get(0).x, points.get(1).x), Math.max(points.get(0).y, points.get(1).y));
				points.clear();
				points.add(upperLeft);
				points.add(new Point(lowerRight.x, upperLeft.y));
				points.add(lowerRight);
				points.add(new Point(upperLeft.x, lowerRight.y));
				addPolygon(points);
				break;
			case FREEHAND:
				if (points.size() == 1)
					addPoint(points);
				else
					addPolyLine(points);
				break;
			// anything else, we just shouldn't care. 
			case LINE :
			case POLYGON :					
			case POLYLINE :			
			case TEXT :
			case POINT :
				break;
		}
		selectionRectangle = null;
		this.repaint();
		//all events corresponding to mouse releases
	}

	/*
	 * MOUSE MOTION LISTENER
	 */
	public void mouseDragged(MouseEvent arg0) {
		if(!allowUserInput) {
			return;
		}
		switch(getUserMode()) {
			case SELECT:
				if(selections.isEmpty()) {
					selectionRectangle = new WB_Line(mouseMovePoint, arg0.getPoint());
				} else {
					//translate all selected items to be dragged
					int dx = arg0.getPoint().x - mouseMovePoint.x;
					int dy = arg0.getPoint().y - mouseMovePoint.y;
					for(WB_Shape s : selections) {
						s.translate(dx, dy);
					}
					hasMoved = true;
					mouseMovePoint.move(arg0.getPoint().x, arg0.getPoint().y);
				}
				this.repaint();
				break;
			case FREEHAND:
				points.add(arg0.getPoint());
				this.repaint();
				break;
			case RECTANGLE:
				if(points.size() > 1) {
					points.set(1, arg0.getPoint());
					if(arg0.isShiftDown()) {
						//maintain the ratio for the sides if shift is down
						//get the maximum distance in x or y
						int maxLength = Math.max(points.get(1).x - points.get(0).x, points.get(1).y - points.get(0).y);
						//move point 1 to that place
						points.get(1).move(points.get(0).x + maxLength, points.get(0).y + maxLength);
					}
					this.repaint();
				}
				break;
		}
	}
	
	public void mouseMoved(MouseEvent arg0) {
		if(!allowUserInput) {
			return;
		}
		mouseMovePoint.move(arg0.getPoint().x, arg0.getPoint().y);
		this.repaint();
	}

	/*
	 * KEY LISTENER
	 */

	public void keyPressed(KeyEvent arg0) {
		if(!allowUserInput) {
			return;
		}
		switch(getUserMode()) {
		case RECTANGLE:
			if(arg0.isShiftDown() && points.size() > 2) {
				//maintain the ratio for the sides if shift is down
				//get the maximum distance in x or y
				int maxLength = Math.max(points.get(1).x - points.get(0).x, points.get(1).y - points.get(0).y);
				//move point 1 to that place
				points.get(1).move(points.get(0).x + maxLength, points.get(0).y + maxLength);
			}
			break;
		case TEXT:
			switch(arg0.getKeyCode()) {
				case KeyEvent.VK_BACK_SPACE:
					if(typedDrawingText.length() > 0) {
						typedDrawingText = typedDrawingText.substring(0, typedDrawingText.length()-1);
					}
					break;
				default:
					if(Character.isDefined(arg0.getKeyChar())) {
						//take care of normal key presses
						this.typedDrawingText += arg0.getKeyChar();
					}
			}
			repaint();
			break;
		case SELECT:
			if(arg0.getKeyCode() == KeyEvent.VK_DELETE)
				deleteSelected();
			break;
		}
	}
	
	public void keyReleased(@SuppressWarnings("unused") KeyEvent arg0) {}
	public void keyTyped(@SuppressWarnings("unused") KeyEvent arg0) {}

	public void setAllowUserInput(boolean allowUserInput) {
		this.allowUserInput = allowUserInput;
		if(allowUserInput)
			this.setBackground(Canvas.DRAW_COLOR);
		else {
			this.setBackground(Canvas.NO_DRAW_COLOR);
			points.clear();
			repaint();
		}
	}
}