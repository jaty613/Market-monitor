package com.qinyadan.monitor.network.control;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class ControlMessageEncoder {


    private Charset charset;

    public ControlMessageEncoder() {
        this.charset = Charset.forName("UTF-8");
    }

    public byte[] encode(Map<String, Object> value) throws ProtocolException {
        ByteBuf cb = ByteBufAllocator.DEFAULT.directBuffer(100);
        encode(value, cb);

        int writeIndex = cb.writerIndex();
        byte[] result = new byte[writeIndex];

        cb.readBytes(result);

        return result;
    }

    private void encode(Map<String, Object> value, ByteBuf cb) throws ProtocolException {
        encodeMap(value, cb);
    }

    private void encode(Object value, ByteBuf cb) throws ProtocolException {
//        try {
            if (value == null) {
                encodeNull(cb);
            } else if (value instanceof String) {
                encodeString((String) value, cb);
            } else if (value instanceof Boolean) {
                encodeBoolean((Boolean) value, cb);
            } else if (value instanceof Short) {
                encodeInt((Short) value, cb);
            } else if (value instanceof Integer) {
                encodeInt((Integer) value, cb);
            } else if (value instanceof Long) {
                encodeLong((Long) value, cb);
            } else if (value instanceof Float) {
                encodeDouble(((Float) value).doubleValue(), cb);
            } else if (value instanceof Double) {
                encodeDouble((Double) value, cb);
            } else if (value instanceof Number) { // Other numbers (i.e.
                // BigInteger and BigDecimal)
                encodeString(value.toString(), cb);
            } else if (value instanceof Collection) {
                encodeCollection((Collection<?>) value, cb);
            } else if (value instanceof Map) {
                encodeMap((Map<?, ?>) value, cb);
            } else if (value.getClass().isArray()) {
                int arraySize = Array.getLength(value);

                List<Object> arrayToList = new ArrayList<Object>(arraySize);
                for (int i = 0; i < arraySize; i++) {
                    arrayToList.add(Array.get(value, i));
                }
                encodeCollection(arrayToList, cb);
            } else {
                throw new ProtocolException("Unsupported type : " + value.getClass().getName());
            }
//        } catch (Exception e) {
//            throw new ProtocolException(e);
//        }
    }

    private void encodeNull(ByteBuf out) {
        out.writeByte((byte) ControlMessageProtocolConstant.TYPE_CHARACTER_NULL);
    }

    private void encodeString(String value, ByteBuf out) {
        out.writeByte((byte) ControlMessageProtocolConstant.TYPE_CHARACTER_STRING);
        putPrefixedBytes(value.getBytes(charset), out);
    }

    private void encodeBoolean(boolean value, ByteBuf out) {
        if (value) {
            out.writeByte((byte) ControlMessageProtocolConstant.TYPE_CHARACTER_BOOL_TRUE);
        } else {
            out.writeByte((byte) ControlMessageProtocolConstant.TYPE_CHARACTER_BOOL_FALSE);
        }
    }

    private void encodeInt(int value, ByteBuf out) {
        out.writeByte((byte) ControlMessageProtocolConstant.TYPE_CHARACTER_INT);

        out.writeByte((byte) (value >> 24));
        out.writeByte((byte) (value >> 16));
        out.writeByte((byte) (value >> 8));
        out.writeByte((byte) (value));
    }

    private void encodeLong(long value, ByteBuf out) {
        out.writeByte((byte) ControlMessageProtocolConstant.TYPE_CHARACTER_LONG);

        out.writeByte((byte) (value >> 56));
        out.writeByte((byte) (value >> 48));
        out.writeByte((byte) (value >> 40));
        out.writeByte((byte) (value >> 32));
        out.writeByte((byte) (value >> 24));
        out.writeByte((byte) (value >> 16));
        out.writeByte((byte) (value >> 8));
        out.writeByte((byte) (value));
    }

    private void encodeDouble(double value, ByteBuf out) {
        out.writeByte((byte) ControlMessageProtocolConstant.TYPE_CHARACTER_DOUBLE);

        long longValue = Double.doubleToLongBits(value);

        out.writeByte((byte) (longValue >> 56));
        out.writeByte((byte) (longValue >> 48));
        out.writeByte((byte) (longValue >> 40));
        out.writeByte((byte) (longValue >> 32));
        out.writeByte((byte) (longValue >> 24));
        out.writeByte((byte) (longValue >> 16));
        out.writeByte((byte) (longValue >> 8));
        out.writeByte((byte) (longValue));
    }

    private void encodeCollection(Collection<?> collection, ByteBuf out) throws ProtocolException {
        out.writeByte((byte) ControlMessageProtocolConstant.CONTROL_CHARACTER_LIST_START);
        for (Object element : collection) {
            encode(element, out);
        }
        out.writeByte((byte) ControlMessageProtocolConstant.CONTROL_CHARACTER_LIST_END);
    }

    private void encodeMap(Map<?, ?> map, ByteBuf out) throws ProtocolException {
        out.writeByte((byte) ControlMessageProtocolConstant.CONTROL_CHARACTER_MAP_START);
        for (Object element : map.entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) element;
            encode(entry.getKey(), out);
            encode(entry.getValue(), out);
        }
        out.writeByte((byte) ControlMessageProtocolConstant.CONTROL_CHARACTER_MAP_END);
    }

    private void putPrefixedBytes(byte[] value, ByteBuf out) {
        int length = value.length;

        byte[] lengthBuf = new byte[5];

        int idx = 0;
        while (true) {
            if ((length & 0xFFFFFF80) == 0) {
                lengthBuf[(idx++)] = (byte) length;
                break;
            }

            lengthBuf[(idx++)] = (byte) (length & 0x7F | 0x80);

            length >>>= 7;
        }

        for (int i = 0; i < idx; i++) {
            out.writeByte(lengthBuf[i]);
        }

        out.writeBytes(value);
    }

}
