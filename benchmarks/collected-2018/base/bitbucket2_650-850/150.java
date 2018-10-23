// https://searchcode.com/api/result/126531386/

/*
 * Aleph Toolkit
 *
 * Copyright 1999, Brown University, Providence, RI.
 * 
 *                         All Rights Reserved
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose other than its incorporation into a
 * commercial product is hereby granted without fee, provided that the
 * above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of Brown University not be used in
 * advertising or publicity pertaining to distribution of the software
 * without specific, written prior permission.
 * 
 * BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR ANY
 * PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY BE LIABLE FOR
 * ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package aleph.comm.udp;

import aleph.Aleph;
import aleph.Message;
import aleph.meter.Counter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.Math;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Vector;

/**
 * A UDPConnection implements a reliable packet protocol using UDP packets.  It
 * uses a thread to handle retransmissions and acknowledgments.
 *
 * @author Michael Rubin
 * @date   May 1998
 **/
public class UDPConnection extends Connection implements Constants {

  /* Invariants:
   * - All packets in send window have been sent but not ack'd
   * - nextSeqnum is the seqnum of the next packet expected
   * - lastAcked < nextSeqnum
   */
  private static final boolean DEBUG = false;

  private static DatagramSocket outSocket; // for outgoing packets

  MovePacketsOut movePacketsOut; // thread

  private SendWindow    sWindow; // Send window
  private Vector        sQueue;	// Send window overflow
  private PacketQueue   oQueue;	// queue of delivered packets
  private ReceiveWindow rWindow; // Receive window

  private long timeRetransmit;	// Next scheduled retransmission
  private long timeAckRequired;	// Next scheduled pure ack
  private int  max_nack_seqnum; /* NACK packets specify seq # of
				 * out-of-order packet rec'd.  This field
				 * maintains highest such # rec'd to date.
				 * Use: to repeated retransmissions
				 * of packets below this seq # */
  private int  nextSeqnum;	// seq # next expected packet 
  private int  lastAcked;	// last seq num acknowledged

  private Packet ackPacket, lossPacket, rsvpPacket;	// preallocated
  private Address destination;	// partner's address

  // debugging
  private Packet lastReceived;
  private Packet lastFreed;
  private Packet lastFreedBecause;

  // how many unacked Packets to accumulate
  private final int ackWindow = (S_WSIZE >> 1); 

  private OutputStream outStream; // convert message to packets
  private InputStream  inStream; // convert message to packets

  private volatile boolean alive; // detect recent activity

  // Instrumentation (ta-da!)
  private static Counter packetsSent   = new Counter("Packets sent");
  private static Counter packetsResent = new Counter("Packets resent");
  private static Counter packetsDup    = new Counter("Packets duplicated");
  private static Counter packetsOver   = new Counter("Packets overflowed");

  private volatile boolean quit = false; // time to cash in?

  /**
   * Constructor.
   * @param destination partner's address
   **/
  public UDPConnection(Address destination) {
    super();

    this.destination = destination;

    sWindow = new SendWindow();
    rWindow = new ReceiveWindow();
    sQueue =  new Vector();
    oQueue  = new PacketQueue();

    max_nack_seqnum      = -1;
    lastAcked            = -1;
    timeRetransmit       = Long.MAX_VALUE; // don't retransmit yet
    timeAckRequired      = Long.MAX_VALUE; // don't ack yet

    // preallocate packets
    ackPacket  = new Packet(ACK);
    lossPacket = new Packet(NACK);
    rsvpPacket = new Packet(RSVP);

    // streams
    try {
      outStream = new PacketOutputStream();
      inStream  = new PacketInputStream();
    } catch (Exception e) {
      Aleph.panic("UDPConnection " + e);
    }

    this.movePacketsOut = new MovePacketsOut();
    movePacketsOut.start();     // shepherd packets out

  }

  /**
   * Deliver new packet to connection.
   * @param packet new arrival.
   **/
  public synchronized void deliver(Packet packet) {
    if (! alive) {              // pending ping
      alive = true;             // just received something
      notifyAll();              // wake up concerned parties
    }
    lastReceived = packet;      // debugging help
    int seqnum = packet.getSeqnum();
    int acknum = packet.getAcknum();

    sWindow.freeAckd(acknum); // free any newly acknowledged packets

    if (DEBUG)
      Aleph.debug("Packet delivered " + packet);

    int type = packet.getType();
    try {
      switch (type) {
      case NACK:                  // retransmit
        if (max_nack_seqnum < seqnum) {
          sWindow.retransmit(max_nack_seqnum + 1, seqnum);
          max_nack_seqnum = seqnum;
        }
        break;
      case ACK:			// skip to end
        break;
      case BYE:			// skip to end
	quit = true;
        break;
      case RSVP:		// force ack
        sendAck();
        break;
      default:			// something interesting
        /* Expected sequence number */
        if (seqnum == nextSeqnum) {
          nextSeqnum++;
          oQueue.enq(packet);
          rWindow.advance();      // handle any adjacent out-of-order packets
        } // end expected seq number

        ///////////////////////////////////
        // Handle old duplicates         //
        ///////////////////////////////////

        else if (seqnum < nextSeqnum) {// below expected sequence number
          packetsDup.inc();       // record duplication
          sendAck();		// ack already
        }	// end below expected seq number

        //////////////////////////////////////////////////////////////////
        // Handle packets that are higher than we expect - we missed some
        //////////////////////////////////////////////////////////////////

        else {			// above expected sequence number
          // check to see if we nakked this already
          if (! rWindow.present(seqnum-1)) { // predecessor missing?
            sendLossInfo(seqnum);	// send NACK packet
          }
          rWindow.put(packet, seqnum);
        }
        // Send pure ack if requested or if there are too many unacked packets.
        if (nextSeqnum > (lastAcked + ackWindow))
          sendAck();
        break;
      }
    } catch (IOException e) {}
  }

  /**
   * Send'em a message.
   * @param message what to send
   * @exception java.io.IOException something's wrong
   **/
  public synchronized void send (Message message) throws IOException {
    if (DEBUG)
      Aleph.debug("udp.Connection: sending " + message);
    messagesSent.inc();       // record message sent
    ObjectOutputStream objectOutput = new ObjectOutputStream(outStream);
    objectOutput.writeObject(message);
    objectOutput.close();
  }

  /**
   * Either send the packet or enqueue it.
   **/
  private void send (Packet packet) throws IOException {
    if (DEBUG)
      Aleph.debug("udp.Connection: sending " + packet);
    if (! sWindow.full()) {	// space in send window, send this packet
      sWindow.put(packet);
      if (DEBUG)
	Aleph.debug("Connection.send: sending right away " + packet);
      packet.send(outSocket, destination);
      // Set retransmission timer, if not already set.
      long now = System.currentTimeMillis();
      long newTimeRetransmit = Math.min(timeRetransmit, now + RETX_TIMEOUT);
      if (newTimeRetransmit > timeRetransmit)
	timeRetransmit = newTimeRetransmit;
      packetsSent.inc();        // record packet sent
      synchronized(this) {
        notifyAll();		// inform movePacketsOut thread
      }
    } else {
      // No space in send window, enqueue it.
      if (DEBUG)
	Aleph.debug("Connection.send: no space, queuing: " + packet);
      sQueue.addElement(packet);
    }
  }

  /**
   * Pull in next message.  Blocking method.
   * @returns next message
   **/
  protected Message receive () {
    try {
      return (Message) (new ObjectInputStream(inStream)).readObject();
    } catch(Exception e) {
      Aleph.panic("receive: " + e);
      return null;		// not reached
    }
  }

  /**
   * Clean up at the end.
   **/
  public synchronized void close () {
    // the static outsocket may be needed later
    try {
      sendBye();
      super.close();
      outStream.close();
      inStream.close();
      quit = true;              // first tell thread to quit
      notifyAll();              // then wake it up
    } catch (IOException e) {};
  }

  public synchronized void flush ()  {
    try {
      alive = false;              // set watchdog variable
      sendRSVP();
      long start = System.currentTimeMillis();
      while (! sWindow.empty()) {
        try {wait(Constants.ACK_TIMEOUT);} catch (InterruptedException e) {};
        if (System.currentTimeMillis() - start > Constants.ACK_TIMEOUT)
          throw new InterruptedIOException();
      }
    } catch (IOException e) {}
  }

  public synchronized boolean ping () {
    alive = false;              // set watchdog variable
    try {
      sendRSVP();
      long start = System.currentTimeMillis();
      while (! alive) {
        try {wait(Constants.ACK_TIMEOUT);} catch (InterruptedException e) {};
        if (System.currentTimeMillis() - start > Constants.ACK_TIMEOUT)
          return false;           // waited long enough
      }
      return true;
    } catch (IOException e) {
      return false;
    }
  }


  /**
   * Shut down connection
   **/
  void sendBye () throws IOException {
    Packet byePacket = new Packet(BYE);
    byePacket.send(outSocket, destination);
    if (DEBUG)
      Aleph.debug("Connection sends " + byePacket + " to " + destination);
  }

  /**
   * Sends a pure ack (usually because of idle time interval)
   **/
  void sendAck () throws IOException {
    fillAcknum(ackPacket);
    ackPacket.send(outSocket, destination);
    if (DEBUG)
      Aleph.debug("Connection sends " + ackPacket + " to " + destination);
  } // sendAck

  /**
   * Sends RSVP (request for acks)
   **/
  void sendRSVP () throws IOException {
    fillAcknum(rsvpPacket);
    rsvpPacket.send(outSocket, destination);
    if (DEBUG)
      Aleph.debug("Connection sends " + rsvpPacket + " to " + destination);
  }

  /**
   * Sends a NACK packet.
   * The acknum field of the packet contains the sequence number
   * up to which we have successfully received.
   * @param seqnum sequence number of an out-of-order packet just received.
   **/
  void sendLossInfo (int seqnum) throws IOException {
    if (DEBUG)
      Aleph.debug("sending NACK for Packet #" + seqnum + " to " + destination);
    lossPacket.setSeqnum(seqnum);
    fillAcknum(lossPacket);
    lossPacket.send(outSocket, destination);
  } 

  /**
   * Set acknum to most recent value.
   **/
  private void fillAcknum (Packet packet) {
    int acknum = nextSeqnum - 1;
    packet.setAcknum(acknum);
    lastAcked = acknum;
    long now = System.currentTimeMillis();
    long newTimeAckRequired = now + ACK_TIMEOUT;
    if (newTimeAckRequired < timeAckRequired) {// did we advance the deadline?
      // move up deadline and goose waiting thread
      timeAckRequired = now + ACK_TIMEOUT;
      notifyAll();              // assume already holding lock
    }
  }

  private int rIndex (int i) {
    return i & (R_WSIZE - 1);
  }

  public String toString () {
    StringBuffer result = new StringBuffer();
    try {
      long now = System.currentTimeMillis();
      long whenAckRequired = timeAckRequired - now;
      long whenRetransmit  = timeRetransmit  - now;
      result.append("\tlast ack " + whenAckRequired);
      result.append("\tlast retrans " + whenRetransmit);
      result.append("\n\tSend Window " + sWindow);
      result.append("\n\tSend Window overflow " + sQueue);
      result.append("\n\tReceive Window " + rWindow);
      result.append("\n\tReady " + oQueue);
      result.append("\n\tlast packet received: " + lastReceived);
      result.append("\n\tlast packet freed: " + lastFreed);
      result.append("\n\tlast packet freed because: " + lastFreedBecause);
      return result.toString();
    } catch (Exception e) {
      return e.toString();
    }
  }

  /**
   * Inner class that defines send window.
   **/
  class SendWindow {

    private Packet[] window;	// Send window 
    private int      head_seqnum; // first packet's seq number
    private int      tail_seqnum; // next packet's seq number
    private int      size;	// number of packets in send window 

    public SendWindow () {
      window = new Packet[S_WSIZE];
      head_seqnum = tail_seqnum = 0;
      size = 0;
    }

    public void put (Packet packet) {
      fillAcknum(packet);	// contemporary ack number
      window[toIndex(tail_seqnum)] = packet;
      packet.setSeqnum(tail_seqnum++);
      size++;
    }
  
    /**
     * Free newly-acknowledged packets.
     **/
    public void freeAckd (int acknum) {
      if (head_seqnum <= acknum) {
        lastFreed = window[toIndex(head_seqnum)];
        lastFreedBecause = lastReceived;
        int total = acknum - head_seqnum + 1; // How many acked?
        head_seqnum += total;	// advance head sequence number
        size -= total;	// reduce size
        // reset retransmission timer
        timeRetransmit = System.currentTimeMillis() + RETX_TIMEOUT;
        if (!sQueue.isEmpty())	// if packets are backed up ...
          UDPConnection.this.notifyAll(); // wake up movePacketsOut thread
      }
    }
        
    /**
     * Retransmit all packets.
     **/
    public void retransmit () throws IOException {
      retransmit(head_seqnum, tail_seqnum);
    }

    /**
     * Retransmit range of packets.
     * @param from_seq retransmit greater or equal sequence numbers
     * @param to_seq retransmit lesser sequence numbers
     **/
    public void retransmit (int from_seq, int to_seq) throws IOException {
      if (DEBUG)
        Aleph.debug("retransmit" + from_seq + ", " + to_seq + ")");
      // clip sequence numbers to top and bottom of send window.
      from_seq = Math.max(from_seq, head_seqnum);
      to_seq   = Math.min(to_seq+1, tail_seqnum);
      for (int i = from_seq; i < to_seq; i++) {
        Packet packet = window[toIndex(i)];
        fillAcknum(packet);
        if (DEBUG)
          Aleph.debug("retransmitting " + packet);
        packet.send(outSocket, destination);
        packetsResent.inc();
      }
      timeRetransmit = System.currentTimeMillis() + RETX_TIMEOUT; // reset timer
    }

    public boolean full () {
      return size == S_WSIZE;
    }  

    public boolean empty () {
      return size == 0;
    }

    public int size () {
      return size;
    }

    private int toIndex (int i) {
      return i & (S_WSIZE-1);
    }

    public String toString () {
      try {
        StringBuffer result = new StringBuffer("[");
        if (empty()) {
          result.append("next seqNum: ");
          result.append(tail_seqnum);
        } else {
          for (int i = head_seqnum; i < tail_seqnum; i++) {
            result.append(window[toIndex(i)].toString());
            result.append(" ");
          }
        }
        result.append("]");
        return result.toString();
      } catch (Exception e) {
        return e.toString();
      }
    }
  }

  /**
   * Inner class that manages receive window.
   **/
  private class ReceiveWindow {
    private Packet[] window;	// Each slot either null or unreceived packet.
    private int      head;	// window starts at next expected + 1 packet

    public ReceiveWindow () {
      window = new Packet[R_WSIZE]; // initially null
      head   = toIndex(1);	// initially expecting packet zero
    }

    /**
     * Advance head of receive window until we find an empty slot.  Each
     * intervening full slot is a packet that arrived earlier and out of order.
     **/
    public void advance () {
      while (window[head] != null) {
        oQueue.enq(window[head]); // handle packet
        window[head] = null;    // remove from window
        head = toIndex(head + 1); // next slot
        nextSeqnum++;		// advance next expected seq number
      }
      head = toIndex(head + 1); // to one beyond next expected packet
    }

    // Is slot occupied?
    public boolean present (int seqnum) {
      if (seqnum <= nextSeqnum + R_WSIZE)	// fits in window
        return window[toIndex(seqnum)] != null;
      else
        return false;
    }

    // Put
    public void put (Packet packet, int seqnum) {
      if (seqnum <= nextSeqnum + R_WSIZE) {	// fits in window
        int i = toIndex(seqnum);
        if (window[i] != null)	// note receipt of duplicate
          packetsDup.inc();
        window[i] = packet;
      }
    }

    private int toIndex (int i) {
      return i & (R_WSIZE-1);
    }

    public String toString () {
      try {
        StringBuffer result = new StringBuffer("[last acked: ");
        result.append(Integer.toString(lastAcked));
        result.append(", next expected: ");
        result.append(Integer.toString(nextSeqnum));
        result.append(", ");
        int i = head;
        for (i = 0; i < R_WSIZE; i++) {
          Packet packet = window[toIndex(head + i)];
          if (packet != null) {
            result.append(window[toIndex(head + i)]);
            result.append(" ");
          }
        }
        result.append("]");
        return result.toString();
      } catch (Exception e) {
        return e.toString();
      }
    }
  }
  
  /**
   * Inner class to transform messages to packets.
   **/
  private class PacketOutputStream extends OutputStream {
    Packet packet;
    PacketOutputStream () {
      packet = new Packet(DATA);
    }
    public void write (int b) throws IOException {
      if (packet.available() == 0) {
        send(packet);
        packet = new Packet(DATA);
        packetsOver.inc();
      }
      packet.append(b);
    }
    // Write len bytes starting at offset off from byte array b.
    public void write(byte[] b, int off, int len) throws IOException {
      int a = packet.available();
      while(len > a) {
        if (DEBUG)
          Aleph.debug("PacketOutputStream: len " + len + " a " + a);
        packet.append(b, off, a);
        off += a;
        len -= a;
        send(packet);
        packet = new Packet(DATA);
        a = packet.available();
        packetsOver.inc();
      }
      packet.append(b, off, len);
    }
    public void flush () {
      try {
        if (!packet.empty()) {
          send(packet);
          packet = new Packet(DATA);
        }
      } catch (IOException e) {}
    }
    public void close() {
      flush();
    }
  }
  
  /**
   * Inner class to transform packets to messages.
   **/
  public class PacketInputStream extends InputStream {
    private Packet packet;
    PacketInputStream () {
      super();
      packet = null;
    }
    public int available () {
      if (packet == null)
        return 0;
      return packet.available();
    }
    public int read () {
      if (packet == null || packet.available() == 0)
        packet = oQueue.deq();  // potentially blocking
      int c = packet.read();
      return c;
    }
    public int read (byte b[], int off, int len) {
      if (packet == null || packet.available() == 0)
        packet = oQueue.deq();  // potentially blocking
      return packet.read(b, off, len);
    }
    public void close () {}
    public String toString() {
      return packet.toString();
    }
  }

  /**
   * Inner class: moves out messages for this PE.
   **/
  private class MovePacketsOut extends Thread {
    public void run () {
      synchronized (UDPConnection.this) {
        try {
          while (! quit) {
            long now = System.currentTimeMillis(); // read time once each loop
            if (! sWindow.empty() && now > timeRetransmit) { // retransmit now
              if (DEBUG)
                Aleph.debug("Retransmit timeout expired");
              sWindow.retransmit();
            }
            // Transmit from send queue while there is space in send window.
            while (!sQueue.isEmpty() && !sWindow.full()) {
              // remove from send queue
              Packet packet = (Packet)sQueue.firstElement(); 
              sQueue.removeElement(packet);
              sWindow.put(packet); // place in send window
              if (DEBUG)
                Aleph.debug("Connection.movePacketsOut: sending " +
                                   packet + " to " + destination);
              packet.send(outSocket, destination);
              packetsSent.inc();
            }
            if (! sWindow.empty()) // reschedule next retransmission
              timeRetransmit = now + RETX_TIMEOUT;
            else
              timeRetransmit = Long.MAX_VALUE;
            if (now > timeAckRequired) { // Time to ack?
              if (lastAcked < (nextSeqnum - 1)) {
                if (DEBUG)
                  Aleph.debug("Ack timeout expired");
                sendAck();	// Send pure ack packet
              }
            }
            long interval = Math.max(timeAckRequired, timeRetransmit) - now;
            try {
              if (DEBUG)
                Aleph.debug("sleeping for " +
                                   Math.min(Math.max(0, interval),
                                            RETX_TIMEOUT));
              UDPConnection.this.wait(Math.min(Math.max(0, interval),
                                            RETX_TIMEOUT));
            } catch (InterruptedException e) {}
          }
        } catch (IOException e) {}
      }
    }
  }

  static {                      // initialize static members
    try {
      outSocket = new DatagramSocket();
    } catch (Exception e) {
      Aleph.panic("Exception in static initializer: " + e);
    }
  }

}

