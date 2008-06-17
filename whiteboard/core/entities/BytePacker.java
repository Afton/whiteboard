package whiteboard.core.entities;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import whiteboard.core.transaction.ShapePacket;

public class BytePacker {

	private static String seperator = "\\} \\{";
	
	public static List<Byte> packAttributes(List<Byte> contents, HashMap<String, String> attributes)
	{
		int numAtts = attributes.entrySet().size();
		contents.addAll(convertIntToBytes(numAtts));
		
		if (numAtts > 0)
		{
			String attPairs = attributes.toString();
			attPairs = attPairs.replaceAll(seperator, "\0"); // all seperators
			attPairs = attPairs.replaceAll("[\\{\\}]","");// get rid of leading/trailing braces
			byte[] attsAsBytes = attPairs.getBytes();
			
			for ( int i = 0; i < attsAsBytes.length; ++i)
			{
				contents.add(attsAsBytes[i]);
			}
		}

		return contents;
	}



	public static List<Byte> convertStringToBytes(String str) {
		ArrayList<Byte> vals = new ArrayList<Byte>(str.length());
		
		// How 'bout this?
		for (byte b : str.getBytes())
		{
			vals.add(b);
		}		
		
		return vals;
	}
	
	public static List<Byte> convertIntToBytes(int val)
	{
		ArrayList<Byte> vals = new ArrayList<Byte>(4);
		vals.add((byte) (val >>> 24) );
		vals.add((byte) (val >>> 16) );
		vals.add((byte) (val >>> 8) );
		vals.add((byte) (val >>> 0) );
		return vals;
	}
	
	public static List<Byte> convertPointToBytes(Point xy)
	{
		int x = (int) xy.getX();
		int y = (int) xy.getY();
		ArrayList<Byte> vals = new ArrayList<Byte>(8);
		vals.add((byte) (x >>> 24) );
		vals.add((byte) (x >>> 16) );
		vals.add((byte) (x >>> 8) );
		vals.add((byte) (x >>> 0) );
		vals.add((byte) (y >>> 24) );
		vals.add((byte) (y >>> 16) );
		vals.add((byte) (y >>> 8) );
		vals.add((byte) (y >>> 0) );
		return vals;
	}
	
	/***
	 * 
	 * @param toInt PRECONDITION: toInt[start] must have at least 4 bytes to be processed
	 * @return
	 */
    public static int convertBytesToInt(byte[] bytes, int index) 
    {
        int i = 0;
        i = (bytes[index + 3] & 0xFF) |
        ((bytes[index + 2] & 0xFF) << 8) |
        ((bytes[index + 1] & 0xFF) << 16) |
        ((bytes[index] & 0xFF) << 24);
    	return i;
    }
	
    
    /**
     * 
     * @param data the shape in packed format that holds the packed hashmap
     * @param start the index into the shape that the hashmap starts at
     * @param attributes the hashmap to fill
     * @return the new updated offset
     */
    public static int extractKeyVals(byte[] data, int start, HashMap<String,String> attributes)
    {
    	int attNums = convertBytesToInt(data,start);
    	start += ShapeConstants.INTEGER_BYTE_SIZE;

    	String atts = new String(data, start, data.length-start);

    	String[] attPairs = atts.split("\0");
    	for(int i = 0; i < attNums; ++i) {
    		start += attPairs[i].toCharArray().length;
    		String[] pair = attPairs[i].split("=");
    		if(pair.length == 2)
    			attributes.put(pair[0], pair[1]);
    		else
    			System.err.println("BytePacker.extractKeyVals: attribute pair not a pair - " + pair);
    	}

    	return start;
    }
    
    /***
     * 
     * @param data
     * @param start
     * @param numCoords
     * @return returns the updated offset 'start'
     */
    public static int extractPoints(byte[] data, int start, List<Point> coords)
    {
    	coords.clear();
    	int numCoords = convertBytesToInt(data, start);
    	start += ShapeConstants.INTEGER_BYTE_SIZE; // skip past the int
    	
	   	for (int i=0; i < numCoords; ++i)
    	{

    		int x1 = convertBytesToInt(data, start);
    		start += ShapeConstants.INTEGER_BYTE_SIZE;
    		int y1 = convertBytesToInt(data, start);
    		start += ShapeConstants.INTEGER_BYTE_SIZE;
    		
    		coords.add(new Point(x1,y1));  		
    	}
    	return start;
    }    
    
	/***
	 * 
	 * @param packet : This should be the pack'ed version of the object. 
	 * I.e. with the header stripped off (so starting at byte 14). So the relationship
	 * data == CreateWB_DisplayObject(data).pack() should hold
	 * @return the equivalent object. 
	 */
	public static WB_Shape createWB_ShapeFromShapePacket(ShapePacket packet)
	{
		int offset = ShapeConstants.PACKET_EXP_HEADER_OFFSET + ShapeConstants.INTEGER_BYTE_SIZE;
		return createWB_ShapeFromShapePacket(packet, offset);
	}

	public static WB_Shape createWB_ShapeFromShapePacket(ShapePacket packet, int offset) {
		WB_Shape WB_data = createWB_ShapeFromPacket(packet.packet, offset);
		WB_data.setHashCode(packet.objectReference);
		return WB_data;
	}

	public static WB_Shape createWB_ShapeFromPacket(byte[] packet, int offset) {
		WB_Shape WB_data = null;
		Byte type = packet[offset++];
		ShapeConstants.SHAPE_TYPE pType = ShapeConstants.SHAPE_TYPE.values()[type.intValue()];
		
		HashMap<String,String> atts = new HashMap<String,String>();
		offset = extractKeyVals(packet, offset, atts);
		switch(pType) {
			case POINT_TYPE:
				List<Point> coords = new ArrayList<Point>(1);
				extractPoints(packet,offset, coords);
				WB_data = new WB_Point(coords.get(0));
				WB_data.setAttributes(atts);
				break;
			case LINE_TYPE:
				coords = new ArrayList<Point>(2);
				extractPoints(packet, offset, coords);
				WB_data = new WB_Line(coords.get(0), coords.get(1));
				WB_data.setAttributes(atts);
				break;
			case POLYLINE_TYPE:
				coords = new ArrayList<Point>();
				extractPoints(packet,offset, coords);
				WB_data = new WB_Polyline(coords);
				WB_data.setAttributes(atts);
				break;
			case POLYGON_TYPE:
				coords = new ArrayList<Point>();
				extractPoints(packet,offset, coords);
				WB_data = new WB_Polygon(coords);
				WB_data.setAttributes(atts);
				break;
			case TEXT_TYPE:
				coords = new ArrayList<Point>(1);
				int numChars = convertBytesToInt(packet, offset); offset += ShapeConstants.INTEGER_BYTE_SIZE;
				String text = new String(packet, offset, numChars); 
				offset += numChars;

				extractPoints(packet, offset, coords);
				WB_data = new WB_Text(coords.get(0), text);
				WB_data.setAttributes(atts);
				break;
			default:
				return null;
		}
		
		return WB_data;
	}
	
	public static byte[] convertTo_byte_Array(List<Byte> bytes)
	{
		byte[] byteArray = new byte[bytes.size()];
		for (int i=0; i < bytes.size(); ++i)
		{
			byteArray[i] = bytes.get(i); // this can probably be optimized by not using random access
		}

		return byteArray;
	}
	
	public static void insertInto_byte_Array(List<Byte> source, byte[] destination, int startInsertPoint)
	{
		for (int i=0; i< source.size(); ++i)
		{
			destination[i+startInsertPoint] = source.get(i);
		}
	}
	
}
