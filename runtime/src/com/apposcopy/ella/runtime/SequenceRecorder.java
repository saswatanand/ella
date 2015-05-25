package com.apposcopy.ella.runtime;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SequenceRecorder extends Recorder
{
	// Important:
	// rwl should be held in *read* mode for *writting* to the arrays
	// rwl should be held in *write* mode when resizing the arrays or when 
	// *reading* from them, to avoid reading incomplete values.
	// write-write conflics are prevented by the use of the atomic pointer writep. 
	private ReadWriteLock rwl = new ReentrantReadWriteLock();
	private AtomicInteger readp = new AtomicInteger(0);
	private AtomicInteger writep = new AtomicInteger(0);

	protected int[] data = new int[10000];
	protected long[] metadata = new long[10000];
	
	public void record(int d, long md)
	{
		rwl.readLock().lock();
		int wi = writep.getAndIncrement();
		int ri = readp.get();
		if((wi-ri) >= data.length){
			// Lock for writting and grow the arrays
			rwl.readLock().unlock();
			rwl.writeLock().lock();
			if((wi-ri) >= data.length){
			
				// Re-set indexes
				// pointers can be updated non-atomically, because we hold the write lock
				wi = wi % data.length;
				ri = ri % data.length;
				if(wi < ri) wi += data.length; // write pointer should always be "ahead"
				writep.set(wi+1);
				readp.set(ri);			
			
				int[] oldData = data;
				int newLen = oldData.length*2;
				int[] newData = new int[newLen];
				System.arraycopy(oldData, 0, newData, 0, oldData.length);
				data = newData;
			
				long[] oldMetadata = metadata;
				long[] newMetadata = new long[newLen];
				System.arraycopy(oldMetadata, 0, newMetadata, 0, oldMetadata.length);
				metadata = newMetadata;
			
			}
			rwl.writeLock().unlock();
			rwl.readLock().lock();
		}
		wi = wi % data.length;
		assert wi != (ri % data.length);
		data[wi] = d;
		metadata[wi] = md;
		rwl.readLock().unlock();
	}
	
	public String data()
	{
		StringBuilder builder = new StringBuilder();
		// Taking the writeLock instead of the readLock guarantees all array stores have been fully commited.
		// Thus we may not read a new value for data[i] paired with an old metadata[i]
		rwl.writeLock().lock();
		int[] seq = data;
		long[] ts = metadata;
		int wi = writep.get();
		// Consume the buffer until we catch up with the position of wi we just got.
		for(int ri = readp.getAndSet(wi); ri < wi; ri++) {
			int i = ri % data.length;
			builder.append(ts[i]+" "+seq[i]+"\n");
		}
		rwl.writeLock().unlock();
		return builder.toString();
	}
}
