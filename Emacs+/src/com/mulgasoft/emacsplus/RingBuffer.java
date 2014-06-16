/**
 * Copyright (c) 2009, 2010 Mark Feber, MulgaSoft
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.mulgasoft.emacsplus;

import java.util.ArrayList;
import java.util.List;

/**
 * Ring Buffer implementation
 * 
 * @author Mark Feber - initial API and implementation
 */
public class RingBuffer<T> {

	public static final int DEFAULT_RING_SIZE = 16;
	public static final int LARGE_RING_SIZE = 30;
	public static final int NO_POS = -1;

	private ArrayList<IRingBufferElement<T>> ringBuffer = null;

	private int size = DEFAULT_RING_SIZE;
	private int pos = NO_POS;
	// marks the last non-empty location
	private int maxPos = 0;
	// marks the current yank location (if no intervening cmd has occurred)
	private int yankpos = 0;
	// holds the current yank location (in case rotate has intervened)
	private int lastyankpos = 0;
	// mark last command as yank
	private boolean yanked = false;

	private boolean infiniteLoop = true;
	public void setInfiniteLoop(boolean infinite) {
		this.infiniteLoop = infinite;
	}
	
	// flag last manual rotation - clear as soon as a yank or yank-pop is executed
	boolean rotated = false;
	
	public YankRotate rotateDirection = YankRotate.FORWARD;
	
	public RingBuffer() {
		this(DEFAULT_RING_SIZE);
	}
	
	public RingBuffer(int size) {
		this.size = size;
		ringBuffer = initArray(size);
	}
	
	public RingBuffer(List<T> elements) {
		this.size = elements.size();
		ringBuffer = initArray(size, elements);
	}
	
	private ArrayList<IRingBufferElement<T>> initArray(int size, List<T> elements) {
		ArrayList<IRingBufferElement<T>> a = new ArrayList<IRingBufferElement<T>>(size);
		IRingBufferElement<T> ele = null;
		int tsize = (elements != null ? elements.size() : 0);
		// this initializes the internal ArrayList size properly
		for (int i=0; i < size; i++) {
			if (tsize > 0 && i < tsize) {
				ele = this.getNewElement();
				ele.set(elements.get(i));
			}
			a.add(ele);
		}
		maxPos = (tsize > 0 ? tsize - 1 : 0); 
		return a;
	}
	
	private ArrayList<IRingBufferElement<T>> initArray(int size){
		return initArray(size, null);
	}
	
	protected static int getDefaultSize() {
		return DEFAULT_RING_SIZE;
	}
	
	/**
	 * @return the maximum size of the ring buffer
	 */
	public int maxSize(){
		return size;
	}
	
	/**
	 * @return the current length of the ring buffer
	 */
	public int length(){
		return maxPos + (isEmpty() ? 0 : 1);
	}

	int getPos() {
		return (pos == NO_POS ? 0 : pos);
	}
	
	int getYankpos() {
		return yankpos;
	}

	/**
	 * Is the ring buffer empty
	 * 
	 * @return true if no elements have been inserted yet, else false 
	 */
	public boolean isEmpty() {
		return pos == NO_POS;
	}
	
	/**
	 * Change the size of the ring buffer
	 * Preserve the contents starting from the current insertion position
	 * 
	 * @param newSize the new ring buffer size
	 */
	public synchronized void setSize(int newSize){

		if (size != newSize){
			// build and copy
			ArrayList<IRingBufferElement<T>> newBuffer = initArray(newSize);
			// check if current contents  fit into new buffer
			if (isEmpty()) {
				// no elements yet
			} else if (newSize >  maxPos){
				// new size is less than current content size, simple copy
				for (int i=0; i <= maxPos; i++){
					newBuffer.set(i,ringBuffer.get(i));
				}
			} else {
				// start from the newSize position back from current position
				int ii = pos - (newSize-1);
				if (ii < 0) {
					ii = size+ii;
				}
				for (int i = 0; i < newSize; i++, ii++){
					if (ii >= size){
						ii = 0;
					}
					newBuffer.set(i,ringBuffer.get(ii));
				}
				maxPos = newSize -1;
				lastyankpos = yankpos = pos = maxPos;	// reset
			}
			ringBuffer = newBuffer;
			size = newSize;
		}
	}
	
	// have we just yanked
	public boolean isYanked() {
		return yanked;
	}

	// note that we have just yanked
	public void setYanked(boolean yanked) {
		this.yanked = yanked;
	}
	
	/**
	 * Return the element's string at the specified position
	 * 
	 * @param pos the element's position
	 * @return the element's string or null if no element at that position
	 */
	public T get(int pos) {
		T result = null;
		IRingBufferElement<T> rbe = getElement(pos);
		if (rbe != null) {
			result = rbe.get();
		}
		return result;
	}
	
	/**
	 * Return the element at the specified position
	 * 
	 * @param pos the element's position
	 * @return the element or null if no element at that position
	 */
	protected IRingBufferElement<T> getElement(int pos) {
		if (0 > pos || pos > maxPos) {
			pos = 0;
		}
		return ringBuffer.get(pos);
	}

	/**
	 * @return the content at the yank position
	 */
	public T yank() {
		recordYank();
		return get(yankpos);
	}

	/**
	 * @return the element at the yank position
	 */
	public IRingBufferElement<T> yankElement() {
		recordYank();
		return getElement(yankpos);
	}
	
	/**
	 * @return String last yanked
	 */
	public T lastYank() {
		return get(lastyankpos);
	}

	/**
	 * Return the 'next' yank position content
	 * 
	 * If the ring has just been manually rotated, return the current yank position
	 * else, rotate once and return the element there.
	 * 
	 * @return the content at the 'next' yank position
	 */
	public T yankPop() {
		// don't change yank position if it's
		// just been manually rotated 
		if (!rotated && --yankpos < 0) {
			yankpos = maxPos;
		}
		recordYank();
		return yank();
	}

	/**
	 * Add the content to the current element
	 * @param content - the content to be added
	 */
	private IRingBufferElement<T> put(T content) {
		IRingBufferElement<T> rbe = getElement();
		rbe.set(content);
		return rbe;
	}
	
	/**
	 * Add or append the content to the element, depending on the current state
	 * @param content - the content to be added
	 */
	public synchronized IRingBufferElement<T> putNext(T content) {
		rotated = false;	// clear rotation
		IRingBufferElement<T> result = null;
		// Never allow null content or empty string as an element
		if (content != null && content.toString().length() > 0) {
			if (++pos >= size) {
				pos = 0;
			}
			if (pos > maxPos) {
				maxPos = pos;
			}
			yankpos = pos;
			result = put(content);
		}
		return result;
	}

	/**
	 * Add the element to the next position in the ring buffer
	 * 
	 * @param element
	 * @return the previous contents of the ring position
	 */
	public synchronized IRingBufferElement<T> putNext(IRingBufferElement<T> element) {
		IRingBufferElement<T> result;
		if (++pos >= size) {
			pos = 0;
		}
		if (pos > maxPos) {
			maxPos = pos;
		}
		yankpos = pos;
		result = getCurrentElement();
		setCurrentElement(element);
		return result;
	}
	
	/**
	 * Check string against possible 'next' & 'previous' positions
	 * for duplicate entries
	 * 
	 * @param dup
	 * @return true if duplicate (or empty content) detected, else false
	 */
	public boolean  isDuplicate(T dup) {
		// null/empty strings are disallowed
		boolean result = dup == null || dup.toString().length() == 0;
		// check against current position
		result = (result ? result : dup.equals(get(getPos())));
		// check against last yank
		result = (result ? result : dup.equals(lastYank()));
		// check against potential yank
		result = (result ? result : dup.equals(get(getYankpos())));
		if (!result && (pos == maxPos)) {
			// if we're at the end, check the beginning
			if (result = dup.equals(get(0))) {
				// move to this position
				yankpos = pos = 0;
			}
		}
		return result;
	}

	/**
	 * I remember yank 
	 */
	private void recordYank() {
		// clear manual rotated flag
		rotated = false;
		// remember position of last yank
		lastyankpos = yankpos;
	}

	/**
	 * Get the direction of a manual rotation
	 * 
	 * @return the direction of rotation
	 */
	public YankRotate getRotateDirection() {
		return rotateDirection;
	}

	/**
	 * Set the direction of a manual rotation
	 * @param rotateDirection
	 */
	public void setRotateDirection(YankRotate rotateDirection) {
		this.rotateDirection = rotateDirection;
	}
	
	/**
	 * Set the direction of a manual rotation by id
	 * @param rotateDirectionId
	 */
	public void setRotateDirection(String rotateDirectionId) {
		if (rotateDirectionId != null) {
			if (YankRotate.FORWARD.id().equals(rotateDirectionId)){
				setRotateDirection(YankRotate.FORWARD);
			} else if (YankRotate.BACKWARD.id().equals(rotateDirectionId)) {
				setRotateDirection(YankRotate.BACKWARD);
			}
		}
	}

	private void invertRotateDirection() {
		if (YankRotate.FORWARD == rotateDirection){
			setRotateDirection(YankRotate.BACKWARD);
		} else if (YankRotate.BACKWARD == rotateDirection) {
			setRotateDirection(YankRotate.FORWARD);
		}
	}

	/**
	 * Rotate the yank position in the specified direction
	 * @param direction
	 * @return the element at the new position
	 */
	public IRingBufferElement<T> rotateYankPos(YankRotate direction){
		yankpos += direction.direction();
		if (direction.direction() != 0) {
			rotated = true;
		}
		if (yankpos > maxPos){
			yankpos = infiniteLoop ? 0 : maxPos;
			if (!infiniteLoop) {
				Beeper.beep();
			}
		} else if (yankpos < 0){
			yankpos = infiniteLoop ? maxPos : 0;
			if (!infiniteLoop) {
				Beeper.beep();
			}
		}
		return getElement(yankpos);
	}

	/**
	 * rotate the yank position forward
	 * i.e. in the direction away from normal yank direction. This is the opposite
	 * of the emacs default, since we currently can't pass C-u arguments
	 * 
	 * @return the element at the rotated position
	 */
	public IRingBufferElement<T> rotateYankPos(){
		return rotateYankPos(getRotateDirection());
	}

	/**
	 * Rotate from the specified ring entry, counting back from the most recent as 1
	 * So, if the current position points at X, the other positions are:
	 * -2 -1 0 X 2 3  etc.
	 *         ^  
	 * @param count
	 * 
	 * @return the element count positions from current or null
	 */
	public IRingBufferElement<T> rotateYankPos(int count) {
		IRingBufferElement<T> result = getElement(lastyankpos);
		if (count != 1) {
			boolean reverse = count <= 0;
			try {
				if (reverse) {
					invertRotateDirection();
					// account for decrement in loop
					count = Math.abs(count)+2;
				}
				System.out.println(result.get());
				for (int i = count - 1; i > 0; i--) {
					result = rotateYankPos();
				}
			} finally {
				if (reverse) {
					invertRotateDirection();
				}
			}
		}
		return result;
	}
	
	/**
	 * get or create the ring buffer element
	 * @return the current element
	 */
	protected IRingBufferElement<T> getElement() {
		IRingBufferElement<T> rbe = getCurrentElement();
		if (rbe == null) {
			rbe = getNewElement(); 
			ringBuffer.set(pos, rbe);
		}
		return rbe;
	}

	protected final IRingBufferElement<T> getCurrentElement() {
		return ringBuffer.get(getPos());
	}
	
	protected final void setCurrentElement(IRingBufferElement<T> rbe) {
		int p = getPos();
		if (ringBuffer.get(p) != null) {
			ringBuffer.get(p).dispose();
		}
		ringBuffer.set(p, rbe);
	}
	
	/**
	 * Create a new, unstored, ring buffer element
	 * 
	 * @return the new IRingBufferElement
	 */
	protected IRingBufferElement<T> getNewElement() {
		return new AbstractRingBufferElement();
	}

	/**
	 * Simple Ring Buffer Element
	 * 
	 * Holds content of type T 
	 * 
	 * @author Mark Feber - initial API and implementation
	 */
	protected class AbstractRingBufferElement implements IRingBufferElement<T> {
		private T content;

		public T get() {
			return content;
		}
		
		public void set(T content) {
			this.content = content;
		}

		public String toString() {
			return (content != null ? content.toString() : EmacsPlusUtils.EMPTY_STR);
		}
		
		/**
		 * Generic dispose does nothing 
		 * 
		 * @see com.mulgasoft.emacsplus.RingBuffer.IRingBufferElement#dispose()
		 */
		public void dispose() {}
	}

	public interface IRingBufferElement<T> {
		
		/**
		 * @return the content of this element
		 */
		T get();
		
		/**
		 * Set the content of this element
		 * 
		 * @param content
		 */
		void set(T content);
		
		/**
		 * Called when an element is dropped from the RingBuffer
		 * Implementors can perform any necessary cleanups  
		 */
		void dispose();
	}

}
