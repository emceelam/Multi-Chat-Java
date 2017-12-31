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
    try {
      ServerSocketChannel ssc = ServerSocketChannel.open();
      //ssc.setOption(SO_REUSEADDR, true);
      ssc.bind(new InetSocketAddress(port));
      ssc.configureBlocking(false);

      Dictionary<SocketChannel,Long> channelToExpiration
        = new Hashtable<SocketChannel,Long>();
      //ArrayList<Client> clients = new ArrayList<Client>();
      while (true) {
        Selector selector = Selector.open ();
        ssc.register( selector, SelectionKey.OP_ACCEPT);
        Enumeration<SocketChannel> enumeration = channelToExpiration.keys();

        while (enumeration.hasMoreElements()) {
          SocketChannel channel = enumeration.nextElement();
          channel.register(selector, SelectionKey.OP_READ);
        }

        try {

          int unblockedCnt = selector.select();  // block on i/o activity

          // why would this ever be 0
          if (unblockedCnt == 0) {
            continue;
          }

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
                System.out.println ("removing closed client");
                channelToExpiration.remove(readChannel);
                continue;
              }

              String str = new String(buf.array());
              str = str.trim();
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
          keys.clear();
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