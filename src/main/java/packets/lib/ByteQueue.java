package packets.lib;

// File: ByteQueue.java from the package edu.colorado.collections
// Complete documentation is available from the ByteQueue link in:
//   http://www.cs.colorado.edu/~main/docs/

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

/******************************************************************************
 * A <CODE>ByteQueue</CODE> is a queue of byte values.
 *
 * <b>Limitations:</b>
 *
 *   (1) The capacity of one of these queues can change after it's created, but
 *   the maximum capacity is limited by the amount of free memory on the
 *   machine. The constructor, <CODE>add</CODE>, <CODE>clone</CODE>,
 *   and <CODE>union</CODE> will result in an
 *   <CODE>OutOfMemoryError</CODE> when free memory is exhausted.
 *
 *   (2) A queue's capacity cannot exceed the maximum integer 2,147,483,647
 *   (<CODE>Integer.MAX_VALUE</CODE>). Any attempt to create a larger capacity
 *   results in a failure due to an arithmetic overflow.
 *
 * <b>Java Source Code for this class:</b>
 *   <A HREF="../../../../edu/colorado/collections/ByteQueue.java">
 *   http://www.cs.colorado.edu/~main/edu/colorado/collections/ByteQueue.java
 *   </A>
 *
 * @author Michael Main
 *   <A HREF="mailto:main@colorado.edu"> (main@colorado.edu) </A>
 *
 * @version Feb 10, 2016
 ******************************************************************************/
public class ByteQueue {
    private byte[] data;
    private int size;
    private int front;
    private int rear;

    @Override
    public String toString() {
        return size + ":" + Arrays.toString(data);
    }

    /**
     * Initialize an empty queue with an initial capacity of 10.  Note that the
     * <CODE>insert</CODE> method works efficiently (without needing more
     * memory) until this capacity is reached.
     **/
    public ByteQueue() {
        final int INITIAL_CAPACITY = 32;
        size = 0;
        data = new byte[INITIAL_CAPACITY];
        // We don't care about front and rear for an empty queue.
    }


    /**
     * Initialize an empty queue with a specified initial capacity. Note that the
     * <CODE>insert</CODE> method works efficiently (without needing more
     * memory) until this capacity is reached.
     * @param initialCapacity the initial capacity of this queue
     *                        <b>Precondition:</b>
     *                        <CODE>initialCapacity</CODE> is non-negative.
     *                        <b>Postcondition:</b>
     *                        This queue is empty and has the given initial capacity.
     * @throws IllegalArgumentException Indicates that initialCapacity is negative.
     * @throws OutOfMemoryError         Indicates insufficient memory for:
     *                                  <CODE>new byte[initialCapacity]</CODE>.
     **/
    public ByteQueue(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity is negative: " + initialCapacity);
        }

        size = 0;
        data = new byte[initialCapacity];
    }

    /**
     * Construct a ByteQueue from a collection of bytes.
     * @param bytes the collection to create the queue from.
     */
    public ByteQueue(Collection<Byte> bytes) {
        data = new byte[bytes.size()];
        for (Byte b : bytes) {
            this.insert(b);
        }
    }

    /**
     * Change the current capacity of this queue.
     * @param minimumCapacity the new capacity for this queue
     *                        <b>Postcondition:</b>
     *                        This queue's capacity has been changed to at least <CODE>minimumCapacity</CODE>.
     *                        If the capacity was already at or greater than <CODE>minimumCapacity</CODE>,
     *                        then the capacity is left unchanged.
     * @throws OutOfMemoryError Indicates insufficient memory for: <CODE>new byte[minimumCapacity]</CODE>.
     **/
    public void ensureCapacity(int minimumCapacity) {
        if (size == 0) {
            data = new byte[minimumCapacity];
        } else if (data.length < minimumCapacity) {
            byte[] biggerArray = new byte[minimumCapacity];
            copyTo(biggerArray);
            data = biggerArray;

            front = 0;
            rear = size - 1;
        }
    }


    /**
     * Accessor method to get the current capacity of this queue.
     * The <CODE>insert</CODE> method works efficiently (without needing
     * more memory) until this capacity is reached.
     * @return the current capacity of this queue
     **/
    public int getCapacity() {
        return data.length;
    }


    /**
     * Get the front item, removing it from this queue.
     * <b>Precondition:</b>
     * This queue is not empty.
     * @return The return value is the front item of this queue, and the item has
     * been removed.
     * @throws NoSuchElementException Indicates that this queue is empty.
     **/
    public byte remove() {
        byte answer;

        if (size == 0) {
            throw new NoSuchElementException("Queue underflow");
        }

        answer = data[front];
        front = nextIndex(front);
        size--;
        return answer;
    }


    /**
     * Insert a new item in this queue.
     * @param item the item to be pushed onto this queue
     **/
    public void insert(byte item) {
        if (size == data.length) {
            // Double the capacity and add 1; this works even if size is 0.
            ensureCapacity(size * 2 + 1);
        }

        if (size == 0) {
            front = 0;
            rear = 0;
        } else {
            rear = nextIndex(rear);
        }

        data[rear] = item;
        size++;
    }


    /**
     * Adds data to the start of the queue. We do not amortize this as it's usually the last data to be added.
     */
    public void prepend(ByteQueue other) {
        if (front != 0) {
            throw new UnsupportedOperationException("Cannot prepend unless queue starts at 0.");
        }

        int requiredLength = size + other.size();
        if (requiredLength >= this.data.length) {
            // if the data array is too small, we create a new array and copy both there
            byte[] res = new byte[requiredLength];
            System.arraycopy(other.data, 0, res, 0, other.size);
            System.arraycopy(this.data, 0, res, other.size, size);
            this.data = res;
        } else {
            // if the data array is large enough, we move the original data and then write the new data
            // at the start
            System.arraycopy(this.data, 0, this.data, other.size, size);
            System.arraycopy(other.data, 0, this.data, 0, other.size);
        }
        this.rear += other.size();
        this.size += other.size();
    }


    /**
     * Determine whether this queue is empty.
     * @return <CODE>true</CODE> if this queue is empty;
     * <CODE>false</CODE> otherwise.
     **/
    public boolean isEmpty() {
        return (size == 0);
    }


    private int nextIndex(int i) {
        if (++i == data.length) {
            return 0;
        } else {
            return i;
        }
    }

    /**
     * Accessor method to determine the number of items in this queue.
     * @return the number of items in this queue
     **/
    public int size() {
        return size;
    }

    /**
     * Empty the array by simply setting the number of items to 0. No need to clear the actual data.
     */
    public void clear() {
        this.size = 0;
    }

    public byte peek() {
        if (size == 0) {
            throw new NoSuchElementException("Queue underflow");
        }

        return data[front];
    }

    public void copyTo(byte[] copy) {
        if (front <= rear) {
            System.arraycopy(data, front, copy, front, size);
        } else {
            int n1 = data.length - front;
            int n2 = rear + 1;
            System.arraycopy(data, front, copy, 0, n1);
            System.arraycopy(data, 0, copy, n1, n2);
        }
    }
}