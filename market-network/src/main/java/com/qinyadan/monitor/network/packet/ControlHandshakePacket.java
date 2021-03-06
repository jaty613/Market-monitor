package com.qinyadan.monitor.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ControlHandshakePacket extends ControlPacket {

	public ControlHandshakePacket(byte[] payload) {
		super(payload);
	}

	public ControlHandshakePacket(int requestId, byte[] payload) {
		super(payload);
		setRequestId(requestId);
	}

	@Override
	public short getPacketType() {
		return PacketType.CONTROL_HANDSHAKE;
	}

	@Override
	public ByteBuf toBuffer() {

		ByteBuf header = Unpooled.buffer(2 + 4 + 4);
		header.writeShort(PacketType.CONTROL_HANDSHAKE);
		header.writeInt(getRequestId());

		return PayloadPacket.appendPayload(header, payload);
	}

	public static ControlHandshakePacket readBuffer(short packetType, ByteBuf buffer) {
		assert packetType == PacketType.CONTROL_HANDSHAKE;

		if (buffer.readableBytes() < 8) {
			buffer.resetReaderIndex();
			return null;
		}

		final int messageId = buffer.readInt();
		final ByteBuf payload = PayloadPacket.readPayload(buffer);
		if (payload == null) {
			return null;
		}
		final ControlHandshakePacket helloPacket = new ControlHandshakePacket(payload.array());
		helloPacket.setRequestId(messageId);
		return helloPacket;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName());
		sb.append("{requestId=").append(getRequestId());
		sb.append(", ");
		if (payload == null) {
			sb.append("payload=null");
		} else {
			sb.append("payloadLength=").append(payload.length);
		}
		sb.append('}');
		return sb.toString();
	}

}
