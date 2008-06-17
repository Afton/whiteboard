package whiteboard.core.entities;

public class ShapeConstants {

	// Shape Type
	public static enum SHAPE_TYPE { NULL_TYPE, // keep the ordinal() values correct
									POINT_TYPE,
									LINE_TYPE,
									POLYLINE_TYPE,
									POLYGON_TYPE,
									TEXT_TYPE};
	
	// This is the key to the text-type string 
	public static final String TEXT_STRING = "WB_DISPLAY_STRING";
	
	/** The types of request objects */
	public static enum WB_REQUEST_TYPE { NULL_REQUEST, // keep the ordinal() values correct
									OBJECT_CREATION,
									OBJECT_GEOM_MODIFICATION,
									OBJECT_ATTIBUTE_MODIFICATION,
									OBJECT_ATTRIBUTE_DELETION,
									OBJECT_DELETION,
									EPOCH_SEQUENCE_REQUEST, // I think this actually means 'epoch-sequenceNum request'
									CHAT, // the object is chat message
									WB_COPY_REQUEST, // request the whiteboard/history
									WB_ELECTION,
									WB_PERM_CHANGE};
									
	/** the classes of a chat */
	public static enum CHAT_TYPE {CHAT_PUBLIC, CHAT_PRIVATE};

	/** the types of geometric transformation we might allow */
	public static enum GEOM_TRANSFORM_TYPE { TRANSLATION, SCALE, ROTATION };
	
	// Data Protocol elements
	public static final Integer PACKET_TYPE_OFFSET = 0;
	public static final Integer PACKET_EPOCH_OFFSET = 1;
	public static final Integer PACKET_SEQUENCE_NUMBER_OFFSET = 5;
	public static final Integer PACKET_CREATION_EPOCH = 9;
	public static final Integer PACKET_CREATION_SEQUENCE = 13;
	public static final Integer PACKET_OBJECT_ID_OFFSET = 17;	
	public static final Integer PACKET_BASE_HEADER_OFFSET = 9;
	public static final Integer PACKET_EXP_HEADER_OFFSET = 17;

	public static final int INTEGER_BYTE_SIZE = Integer.SIZE/Byte.SIZE;
}
