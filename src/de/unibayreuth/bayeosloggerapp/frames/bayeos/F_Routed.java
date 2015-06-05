package de.unibayreuth.bayeosloggerapp.frames.bayeos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class F_Routed extends Frame {

	short myId;
	short panId;
	
	public F_Routed(byte[] payload) {
		super(payload);

		ByteBuffer bf_payload = ByteBuffer.wrap(payload);
		bf_payload.order(ByteOrder.LITTLE_ENDIAN);

		// second byte of payload
		bf_payload.position(1);

		// second and third byte
		this.myId =  bf_payload.getShort();
		
		//fourth and fifth byte
		this.panId = bf_payload.getShort();

	}
		

}
