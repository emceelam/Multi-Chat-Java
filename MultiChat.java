import java.net.*;
import java.net.InetSocketAddress;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.lang.*;
import java.time.*;

public class MultiChat {
  public static void main (String argv[]) {
    int port = 4022;
    long time_out = 10;  // seconds
    System.out.println (
      "Multi client chat server, using telnet, programmed in Python.\n" +
      "To connect:\n" +
      "    telnet 127.0.0.1 " + port + "\n" +
      "\n" +
      "Everything typed by one chat user will be copied to other chat users.\n" +
      "Typing 'quit' on telnet sessions will disconnect."
    );
    try {
      ServerSocketChannel ssc = ServerSocketChannel.open();
      //ssc.setOption(SO_REUSEADDR, true);
      ssc.bind(new InetSocketAddress(port));
      ssc.configureBlocking(false);

      Dictionary<SocketChannel,Long> channelToExpiration
        = new Hashtable<SocketChannel,Long>();
      long selectTimeoutSec = 0;
      while (true) {
        Selector selector = Selector.open ();
        ssc.register( selector, SelectionKey.OP_ACCEPT);
        Enumeration<SocketChannel> enumeration = channelToExpiration.keys();

        while (enumeration.hasMoreElements()) {
          SocketChannel channel = enumeration.nextElement();
          channel.register(selector, SelectionKey.OP_READ);
        }

        try {

          int unblockedCnt = selector.select(selectTimeoutSec*1000); 
            // block on i/o activity or timeout on idle clients

          Set keys = selector.selectedKeys();
          Iterator it = keys.iterator();
          SocketChannel readChannel;
          long now = Instant.now().getEpochSecond();
          while (it.hasNext()) {
            SelectionKey key = (SelectionKey)it.next();

            if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
              Socket socket = ssc.socket().accept();
              SocketChannel channel = socket.getChannel();
              channel.configureBlocking(false);
              channelToExpiration.put (channel, now + time_out);
            }
            else if (
              (key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ)
            {
              readChannel = (SocketChannel) key.channel();
              ByteBuffer buf = ByteBuffer.allocate( 80 );
              buf.clear();
              int nread = readChannel.read(buf);
              //System.out.println ("nread: " + nread);
              if (nread == -1) {
                channelToExpiration.remove(readChannel);
                readChannel.socket().close();
                System.out.println ("client has disconnected");
                continue;
              }

              // update the time out
              channelToExpiration.put(readChannel, now + time_out);

              // evaluate the incoming text
              String str = new String(buf.array());
              str = str.trim();
              if (str.equals("")) {
                continue;
              }
              if (str.equalsIgnoreCase("quit")) {
                channelToExpiration.remove(readChannel);
                readChannel.socket().close();
                System.out.println("client quits");
                continue;
              }
              System.out.println ("str: " + str);

              enumeration = channelToExpiration.keys();
              while (enumeration.hasMoreElements()) {
                SocketChannel writeChannel = enumeration.nextElement();
                if (writeChannel == readChannel) {
                  continue; // skip the originator
                }
                int nwrite = writeChannel.write(
                  ByteBuffer.wrap( (str + "\n").getBytes())
                );
                //System.out.println ("nwrite: " + nwrite);
              }
            }
          }
          //keys.clear();
          enumeration = channelToExpiration.keys();
          selectTimeoutSec = 0;
            // select blocks on timeout of 0
          while(enumeration.hasMoreElements()) {
            SocketChannel channel = enumeration.nextElement();
            long expiration = channelToExpiration.get(channel);
            long duration = expiration - now;
            if (duration <= 0) {
              System.out.println ("Client times out. Closing connection.");
              channelToExpiration.remove(channel);
              channel.socket().close();
              continue;
            }

            // minimum timeout calc
            if ( selectTimeoutSec == 0 // unitialized value
              || duration < selectTimeoutSec) // undercut a previous low
            {
              selectTimeoutSec = duration;
            }
          }
          System.out.println ("selectTimeoutSec: " + selectTimeoutSec);
        }
        catch (IOException e) {
          if (e.getMessage() == "Broken pipe") {
            System.out.println ("SIGPIPE");
            continue;
          }
          System.err.println ("IOException: " + e.getMessage());
        }
      }
    }
    catch (IOException e) {
      System.err.println ("IOException: " + e.getMessage());
    }
  }
}