package com.apposcopy.ella.runtime;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MethodSequenceRecorder implements CoverageRecorder
{
	// Important:
	// rwl should be held in *read* mode for *writting* to the arrays
	// rwl should be held in *write* mode when resizing the arrays or when 
	// *reading* from them, to avoid reading incomplete values.
	// write-write conflics are prevented by the use of the atomic pointer writep. 
	private ReadWriteLock rwl = new ReentrantReadWriteLock();
	private AtomicInteger readp = new AtomicInteger(0);
	private AtomicInteger writep = new AtomicInteger(0);

	protected int[] sequence = new int[10000];
	protected long[] timeStamps = new long[10000];
	
	public void m(int mId)
	{
		rwl.readLock().lock();
		int wi = writep.getAndIncrement();
		int ri = readp.get();
		if((wi-ri) >= sequence.length){
			// Lock for writting and grow the arrays
			rwl.readLock().unlock();
			rwl.writeLock().lock();
			if((wi-ri) >= sequence.length){
			
				// Re-set indexes
				// pointers can be updated non-atomically, because we hold the write lock
				wi = wi % sequence.length;
				ri = ri % sequence.length;
				if(wi < ri) wi += sequence.length; // write pointer should always be "ahead"
				writep.set(wi+1);
				readp.set(ri);			
			
				int[] oldSeq = sequence;
				int newLen = oldSeq.length*2;
				int[] newSeq = new int[newLen];
				System.arraycopy(oldSeq, 0, newSeq, 0, oldSeq.length);
				sequence = newSeq;
			
				long[] oldTimeStamps = timeStamps;
				long[] newTimeStamps = new long[newLen];
				System.arraycopy(oldTimeStamps, 0, newTimeStamps, 0, oldTimeStamps.length);
				timeStamps = newTimeStamps;
			
			}
			rwl.writeLock().unlock();
			rwl.readLock().lock();
		}
		wi = wi % sequence.length;
		assert wi != (ri % sequence.length);
		sequence[wi] = mId;
		timeStamps[wi] = System.currentTimeMillis();
		rwl.readLock().unlock();
	}
	
	public String data()
	{
		StringBuilder builder = new StringBuilder();
		// Taking the writeLock instead of the readLock guarantees all array stores have been fully commited.
		// Thus we may not read a new value for sequence[i] paired with an old timeStamps[i]
		rwl.writeLock().lock();
		int[] seq = sequence;
		long[] ts = timeStamps;
		int wi = writep.get();
		// Consume the buffer until we catch up with the position of wi we just got.
		for(int ri = readp.getAndSet(wi); ri < wi; ri++) {
			int i = ri % sequence.length;
			builder.append(ts[i]+" "+seq[i]+"\n");
		}
		rwl.writeLock().unlock();
		return builder.toString();
	}
}
