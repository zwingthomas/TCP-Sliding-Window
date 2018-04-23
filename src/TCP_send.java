import java.io.IOException;
import java.net.*;

public class TCP_send {

    private int src_port;
    private InetAddress remote_IP;
    private int remote_port;
    private String file_name;
    private int mtu;
    private int sws;
    private char flag;

    public TCP_send(int port, String remote_IP, int remote_port, int sws, char flag) throws UnknownHostException {
        this.src_port = port;
        InetAddress addr = InetAddress.getByName(remote_IP);
        this.remote_IP = addr;
        this.remote_port = remote_port;
        this.flag = flag;
    }

    public void send(TCP_segm segment) {

        boolean running = true;

        DatagramSocket socket = null;

        try {
            System.out.println("src prt: " + src_port);
            socket = new DatagramSocket(this.src_port);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        System.out.println(segment.toString());

        while(running) {
            if (flag == 'D') {
                //send DATA
                DatagramPacket packet = new DatagramPacket(segment.serialize(), segment.totalLength, this.remote_IP, this.remote_port);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //receive ACK
            }
        }
    }


    //use UDP sockets to send to another computer --> google examples of UDP java sockets

}
