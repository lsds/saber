package uk.ac.imperial.lsds.saber.buffers;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Field;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnboundedQueryBuffer implements IQueryBuffer {

    public static Unsafe getTheUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private Unsafe unsafe = getTheUnsafe();

    private int id;
	
	ByteBuffer buffer;
	
	private boolean isDirect;
	
	public UnboundedQueryBuffer (int id, int size, boolean isDirect) {
		
		if (size <= 0)
			throw new IllegalArgumentException("error: buffer size must be greater than 0");
		
		this.id = id;
		this.isDirect = isDirect;
		
		if (! isDirect) {
			buffer = ByteBuffer.allocate(size);
		} else {
			buffer = ByteBuffer.allocateDirect(size);
		}

		buffer.order( ByteOrder.LITTLE_ENDIAN);
	}
	
	public int getInt (int offset) {
		
		return buffer.getInt(offset); 
	}
	
	public float getFloat (int offset) {
		
		return buffer.getFloat(offset); 
	}
	
	public long getLong (int offset) {
		
		return buffer.getLong(offset); 
	}
	
	public long getMSBLongLong (int offset) {

		return buffer.getLong(normalise(offset));
	}
	
	public long getLSBLongLong (int offset) {

		return buffer.getLong(normalise(offset) + 8);
	}
	
	public byte [] array () {
		
		if (isDirect) {
			//throw new UnsupportedOperationException("error: cannot get byte array from a direct buffer");
            byte [] data = new byte [buffer.position()];
            //int tempPos = buffer.position();
            buffer.position(0);
            buffer.get(data);
            //buffer.position(tempPos);
            return data;
		}

		
		return buffer.array();
	}
	
	public ByteBuffer getByteBuffer () {
		
		return buffer; 
	}
	
	public int capacity () {
		
		return buffer.capacity(); 
	}
	
	public int remaining () {
		
		return buffer.remaining(); 
	}
	
	public boolean hasRemaining () {
		
		return buffer.hasRemaining(); 
	}
	
	public int position() {
		
		return buffer.position();
	}
	
	public int limit () { 
		
		return buffer.limit();
	}
	
	public void close () {
		
		buffer.flip(); 
	}
	
	public void clear () {
		
		buffer.clear();
	}
	
	@SuppressWarnings("finally")
	public int putInt (int value) { 
		try {
			buffer.putInt(value);
		} catch (BufferOverflowException e) {
			e.printStackTrace();
		} finally {
			return 0;
		}
	}
	
	public int putInt (int index, int value) {
		
		buffer.putInt(index, value);
		return 0;
	}
	
	@SuppressWarnings("finally")
	public int putFloat (float value) {
 		try {
			buffer.putFloat(value);
		} catch (BufferOverflowException e) {
			e.printStackTrace();
		} finally {
			return 0;
		}
	}
	
	public int putFloat (int index, float value) {
		
		buffer.putFloat(index, value);
		return 0;
	}
	
	@SuppressWarnings("finally")
	public int putLong (long value) {
		try {
			buffer.putLong(value);
		} catch (BufferOverflowException e) {
			e.printStackTrace();
		} finally {
			return 0;
		}
	}
	
	public int putLong (int index, long value) {
		
		buffer.putLong(index, value);
		return 0;
	}
	
	@SuppressWarnings("finally")
	public int putLongLong (long msbValue, long lsbValue) {
		try {
			buffer.putLong(msbValue);
			buffer.putLong(lsbValue);
		} catch (BufferOverflowException e) {
			e.printStackTrace();
		} finally {
			return 0;
		}
	}
	
	public int putLongLong (int index, long msbValue, long lsbValue) {
		
		buffer.putLong(index, msbValue);
		buffer.putLong(index + 8, lsbValue);
		return 0;
	}
	
	@SuppressWarnings("finally")
	public int put (byte [] values) {
		try {
			buffer.put(values);
		} catch (BufferOverflowException e) {
			e.printStackTrace();
		} finally {
			return 0;
		}
	}
	
	public int put (byte [] src, int offset, int length) {
		
		buffer.put(src, offset, length);
		return 0;
	}
	
	public int put (byte [] src, int length) {
		
		buffer.put(src, 0, length);
		return 0;
	}
	
	public int put (IQueryBuffer src) {
		return put (src.array(), src.array().length);
	}
	
	public int put (IQueryBuffer src, int offset, int length) {
		
		return put (src.array(), offset, length);
	}

	public int put (ByteBuffer src, int offset, int length) {

	    //int tempPosition = src.position();
	    //src.position(offset);
        //buffer.put(src);
        //src.position(tempPosition);

		if (length == 0)
			return 0;

        long addr1 = ((DirectBuffer) src).address();
        long addr2 = ((DirectBuffer) buffer).address();
        int _position = buffer.position();
        //for (int i = offset;  i < (offset+length); i+=4)
        //    unsafe.putInt(addr2 + _position, unsafe.getInt(addr1 + i));
        unsafe.copyMemory(addr1 + offset, addr2 + _position, length);
        int position = _position + length;
        position -= (offset == 0) ? 1 : 0;
        buffer.position(position);
		return 0;
	}


	public void free (int index) {
		
		throw new UnsupportedOperationException("error: cannot free bytes from an unbounded buffer");
	}

	public void release() {
		
		UnboundedQueryBufferFactory.free(this);
	}
	
	public void appendBytesTo (int offset, int length, IQueryBuffer dst) {
		
		/* Check bounds and normalise indices of this byte array */
		
		if (isDirect || dst.isDirect()) {
			//throw new UnsupportedOperationException("error: cannot append bytes from/to a direct buffer");

			long addr1 = ((DirectBuffer) this.buffer).address();
			long addr2 = ((DirectBuffer) dst.getByteBuffer()).address();
			int _position = dst.position();
			//for (int i = offset;  i < (offset+length); i+=4)
			//    unsafe.putInt(addr2 + _position, unsafe.getInt(addr1 + i));
			unsafe.copyMemory(addr1 + offset, addr2 + _position, length);
			int position = _position + length;
			position -= (offset == 0) ? 1 : 0; // check this
			buffer.position(position);
		} else {
            dst.put(this.buffer.array(), offset, length);
        }
	}
	
	public void appendBytesTo (int start, int end, byte [] dst) {
		
		if (isDirect)
			throw new UnsupportedOperationException("error: cannot append bytes to a byte array from a direct buffer");
		
		System.arraycopy(buffer.array(), start, dst, 0, end - start);
	}
	
	public void position (int index) {
		
		buffer.position(index);
	}

	public int normalise (long index) {
		return (int) index;
	}
	
	public long getBytesProcessed () {
		
		throw new UnsupportedOperationException("error: unsupported query buffer method call");
	}

	public boolean isDirect () {
		
		return isDirect;
	}

	public int getBufferId () {
		
		return id;
	}
}
