package whiteboard.core.entities;

import java.util.ArrayList;
import java.util.List;
public class WB_Chat {
	/** string containing the chat message */
	private String chatMsg;
	/** enum value indicating if this chat is public (0) or private (1) */
	private ShapeConstants.CHAT_TYPE chatType;

	/** constructor */
	public WB_Chat(String message) {
		chatMsg = message;
		chatType = ShapeConstants.CHAT_TYPE.CHAT_PUBLIC;
	}

	/**
	 * constructor
	 * PRE: byte is a byte packet for a chat
	 * @param b - the data for a chat packet
	 */
	public WB_Chat(byte[] b) {
		Byte type = b[0];
		if(ShapeConstants.WB_REQUEST_TYPE.values()[type.intValue()] != ShapeConstants.WB_REQUEST_TYPE.CHAT)
			return;
		
		type = b[1];
		chatType = ShapeConstants.CHAT_TYPE.values()[type.intValue()];
		int numChars = BytePacker.convertBytesToInt(b, 2);
		byte[] msg = new byte[numChars];
		for(int i = 6; i < b.length; ++i) {
			msg[i-6] = b[i];
		}
		chatMsg = new String(msg);
	}

	/** 
	 * constructor
	 * @param message - the message this chat encapsulates
	 * @param chatType - CHAT_PUBLIC or CHAT_PRIVATE
	 */
	public WB_Chat(String message, ShapeConstants.CHAT_TYPE chatType) {
		chatMsg = message;
		//limit the size
		if(chatMsg.length() > Integer.MAX_VALUE)
			chatMsg = chatMsg.substring(0, Integer.MAX_VALUE);
		this.chatType = chatType;
	}

	/**
	 * method that packs this chat message into protocol byte form
	 * @return null if no appropriate message, or arraylist of bytes 
	 * that corresponds to the chat protocol
	 */
	public List<Byte> pack() {
		if(chatMsg == null || chatMsg.trim().equals(""))
			return null;

		// pack up chat message
		ArrayList<Byte> contents = new ArrayList<Byte>();
		contents.add(new Integer(ShapeConstants.WB_REQUEST_TYPE.CHAT.ordinal()).byteValue());
		contents.add(new Integer(chatType.ordinal()).byteValue());
		contents.addAll(BytePacker.convertIntToBytes(chatMsg.length()));
		contents.addAll(BytePacker.convertStringToBytes(chatMsg));

		return contents;
	}

	public String getChatMsg() {
		return chatMsg;
	}
	
	public boolean isPublic() {
		return (chatType == ShapeConstants.CHAT_TYPE.CHAT_PUBLIC);
	}

	public ShapeConstants.CHAT_TYPE getChatType() {
		return chatType;
	}
}