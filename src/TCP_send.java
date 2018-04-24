import java.io.IOException;
import java.net.*;

public class TCP_send {

    private DatagramSocket socket;
    private InetAddress remote_IP;
    private int remote_port;
    private String file_name;
    private int mtu;
    private int sws;
    private char flag;

    public TCP_send(DatagramSocket socket, String remote_IP, int remote_port, int sws, char flag) throws UnknownHostException {
        this.socket = socket;
        InetAddress addr = InetAddress.getByName(remote_IP);
        this.remote_IP = addr;
        this.remote_port = remote_port;
        this.flag = flag;
    }

    public TCP_segm send(TCP_segm segment) throws IOException {

        //TODO: send first packet with SYN flag


        //TODO: end connection with FIN
        if (flag == 'D') {
            //send DATA
            System.out.println("\n\n Segment we are sending: \n" + segment.toString() + "\n\n");
            DatagramPacket packet = new DatagramPacket(segment.serialize(), segment.totalLength, this.remote_IP, this.remote_port);
            socket.send(packet);

            byte[] buf = new byte[24];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            TCP_segm recv = new TCP_segm(0, 0, 0, 0, (short) 0, null, 'D');
            recv = recv.deserialize(packet.getData());
            return recv;
        }
        TCP_segm bad_segm = new TCP_segm(-1, 0, 0, 0, (short) 0, null, 'D');

        return bad_segm;

        //use UDP sockets to send to another computer --> google examples of UDP java sockets
    }
}
