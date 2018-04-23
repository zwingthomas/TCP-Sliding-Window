import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class TCP_recv {

    private int port;
    private int mtu;
    private int sws;

    public TCP_recv(int port, int mtu, int sws){
        this.port = port;
        this.mtu = mtu;
        this.sws = sws;
    }

    public void receive() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(this.port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        byte[] buf = new byte[this.mtu];
        boolean running = true;
        TCP_segm received = null;
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("received.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                TCP_segm s = new TCP_segm(0, 0, 0, 0, (short) 0, null, 'D');
                s = s.deserialize(packet.getData());
                System.out.println(s.toString());
                byte[] recvData = s.getData();
                String str = new String(recvData);
                System.out.println("recv: " + str);
                writer.write(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

