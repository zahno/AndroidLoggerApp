package de.unibayreuth.bayeosloggerapp.frames.bayeos;

public class F_Message extends Frame {

	String message;

	public F_Message(byte[] f) {
		super(f);
		StringBuilder output = new StringBuilder();
		for (byte s : f) {
			output.append((char) s);
		}
		this.message = output.toString();
	}

	public String toString() {
		String type = "";
		switch (super.getType()) {
		case FRAMETYPE_MESSAGE:
			type = "Message\t";
			break;
		case FRAMETYPE_ERROR_MESSAGE:
			type = "Error Message";
			break;
		}
		return "Type: " + type + "\tMessage:\t" + message;
	}
}
