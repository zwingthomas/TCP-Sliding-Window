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

    //variables for timeOut calculation
    long ERTT;
    long EDEV;
    long timeout;

    public TCP_send(DatagramSocket socket, String remote_IP, int remote_port, int sws, char flag) throws UnknownHostException {
        this.socket = socket;
        InetAddress addr = InetAddress.getByName(remote_IP);
        this.remote_IP = addr;
        this.remote_port = remote_port;
        this.flag = flag;
        this.timeout = 5000000000L;
    }

    public TCP_segm send(TCP_segm segment) throws IOException {

        //TODO: put handshake into this method
        //TODO: end connection with FIN
        //TODO: use only the TCP_segm passed in so that it can be accessed to know if it has been acked
        if (flag == 'D') {
            //send DATA
            segment.serialize();
            System.out.println("\n\nSegment we are sending: \n" + segment.toString());
            DatagramPacket packet = new DatagramPacket(segment.serialize(), 0, segment.totalLength, this.remote_IP, this.remote_port);
            socket.send(packet);

            byte[] buf = new byte[24];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            TCP_segm recv = new TCP_segm(0, 0, 0, 0, (short) 0, null, "D");
            recv = recv.deserialize(packet.getData());
            computeTimeout(recv.timeStamp, recv.sequence);
            return recv;
        }
        TCP_segm bad_segm = new TCP_segm(-1, 0, 0, 0, (short) 0, null, "D");

        return bad_segm;

    }

    public boolean handshake(int initSeqNum) throws IOException {

        //Send SYN
        byte[] empty = new byte[0];
        TCP_segm syn = new TCP_segm(initSeqNum, 0, System.nanoTime(), 0, (short) 0, empty, "S");
        syn.serialize();
        DatagramPacket packet = new DatagramPacket(syn.serialize(), 0, syn.totalLength, this.remote_IP, this.remote_port);
        socket.send(packet);
        System.out.println("SYN SENT____________________");
        System.out.println(syn.toString());

        //Receive SYNACK
        byte[] buf = new byte[24];
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        TCP_segm synack = new TCP_segm(0, 0, 0, 0, (short) 0, null, "E");
        synack = synack.deserialize(packet.getData());
        System.out.println("SYNACK RECV____________________");
        System.out.println(synack.toString());
        computeTimeout(synack.timeStamp, synack.sequence);

        //Send ACK
        byte[] emptyBuf = new byte[0];
        TCP_segm ack = new TCP_segm(initSeqNum + 1,synack.sequence+1, synack.timeStamp, 0, (short) 0, empty, "A");
        ack.serialize();
        packet = new DatagramPacket(ack.serialize(), ack.totalLength, this.remote_IP, this.remote_port);
        socket.send(packet);
        System.out.println("ACK SENT______________________");
        System.out.println(ack.toString());


        return true;
    }

    public void computeTimeout(long timestamp, int sequenceNum){
        double a = 0.875;
        double b = 0.75;
        if(sequenceNum == 0){
            this.ERTT = System.nanoTime() - timestamp;
            this.EDEV = 0;
            this.timeout = 2 * this.ERTT;
        }
        else{
            long SRTT = System.nanoTime() - timestamp;
            long SDEV = Math.abs(SRTT - ERTT);
            this.ERTT = (long)(a * this.ERTT) + (long)((1 - a) * SRTT);
            this.EDEV = (long)(b * this.EDEV) + (long)((1 - b) * SDEV);
            this.timeout = this.ERTT + (4 * this.EDEV);
        }
    }

}















































