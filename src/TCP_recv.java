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
        while(running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                BufferedWriter writer = new BufferedWriter(new FileWriter("received.txt"));
                writer.write(received);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

