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

        //TODO: Handle connection starting with SYN

        //TODO: Handle connection end with FIN
        while(running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                TCP_segm recv = new TCP_segm(0, 0, 0, 0, (short) 0, null, 'D');
                recv = recv.deserialize(packet.getData());
                System.out.println(recv.toString());
                byte[] recvData = recv.getData();
                String str = new String(recvData);
                System.out.println("recv: " + str);
                writer.write(str);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendACK(TCP_segm recv,DatagramPacket recvPack) {
        TCP_segm s = new TCP_segm(-1, recv.getAcknowlegment(), System.currentTimeMillis(), 0, (short) 0, null, 'A');
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(recvPack.getPort());
        } catch (SocketException e) {
            e.printStackTrace();
        }
        DatagramPacket packet = new DatagramPacket(s.serialize(), s.totalLength, recvPack.getAddress(),recvPack.getPort());
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}