import java.net.*;
import java.net.InetSocketAddress;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.lang.*;


public class MultiChat {
  public static void main (String argv[]) {
    int port = 4022;
    try {
      ServerSocketChannel ssc = ServerSocketChannel.open();
      //ssc.setOption(SO_REUSEADDR, true);
      ssc.bind(new InetSocketAddress(port));
      ssc.configureBlocking(false);

      ArrayList<SocketChannel> clients = new ArrayList<SocketChannel>();
      SocketChannel currClient;
      int loopCnt = 0;
      while (true) {
        System.out.println("select: " + loopCnt++);
        Selector selector = Selector.open ();
        ssc.register( selector, SelectionKey.OP_ACCEPT);
        Iterator<SocketChannel> iter = clients.iterator();
        while (iter.hasNext()) {
          currClient = iter.next();
          currClient.register(selector, SelectionKey.OP_READ);
        }

        int unblockedCnt = selector.select();  // block on i/o activity
        
        // why would this ever be 0
        if (unblockedCnt == 0) {
          continue;
        }
        
        Set keys = selector.selectedKeys();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
          SelectionKey key = (SelectionKey)it.next();
          if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
            Socket socket = ssc.socket().accept();
            SocketChannel sc = socket.getChannel();
            sc.configureBlocking(false);
            clients.add(sc);
          }
          else if (
            (key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ)
          {
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer buf = ByteBuffer.allocate( 80 );
            buf.clear();
            int nread = sc.read(buf);
            System.out.println ("nread: " + nread);
            String str = new String(buf.array());
            str = str.trim();
            System.out.println ("str: " + str);
            int nwrite = sc.write( ByteBuffer.wrap( (str + "\n").getBytes()) );
            System.out.println ("nwrite: " + nwrite);
          }
        }
        keys.clear();
      }
    }
    catch (IOException e) {
      System.err.println ("IOException: " + e.getMessage());
    }
  }
}