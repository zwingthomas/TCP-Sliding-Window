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
    private int sequenceSender;

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
            sendData(segment); //send DATA
            TCP_segm ack = receiveAck(); //receive ACK
            return ack;
        }
        return null;
    }

    public void handshake(int initSeqNum) throws IOException {
        //Send SYN
        sendNoData("S", initSeqNum);

        //Receive SYNACK
        TCP_segm ack = receiveAck();

        //Send ACK
       sendNoData("A", initSeqNum + 1);
    }

    public void connectionTeardown() throws IOException {
        //Send FIN

        sendNoData("F", this.sequenceSender);

        //Receive ACK
        TCP_segm ack = receiveAck();

        //Receive FINACK
        TCP_segm finAck = receiveAck();

        //Send ack
        sendNoData("A", finAck.sequence + 1);
        System.exit(0);

    }

    public void sendData(TCP_segm segment) throws IOException {
        segment.serialize();
        System.out.println("\n\nSegment we are sending: \n" + segment.toString());
        DatagramPacket packet = new DatagramPacket(segment.serialize(), 0, segment.getLength() + 24, this.remote_IP, this.remote_port);
        socket.send(packet);
    }

    public void sendNoData(String flag, int seqNum) throws IOException {
        int size = 0;
        byte[] buf = new byte[size];
        TCP_segm tcpSegm = new TCP_segm(seqNum, 0, System.nanoTime(), 0, (short) 0, buf, flag);
        tcpSegm.serialize();
        DatagramPacket packet = new DatagramPacket(tcpSegm.serialize(), 0, tcpSegm.getLength() + 24, this.remote_IP, this.remote_port);
        socket.send(packet);
        System.out.println("SENT____________________");
        System.out.println(tcpSegm.toString());
        System.out.println();
        computeTimeout(tcpSegm.timeStamp, tcpSegm.sequence);
    }

    public TCP_segm receiveAck() throws IOException{
        byte[] buf = new byte[24];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        TCP_segm ack = new TCP_segm(0, 0, 0, 0, (short) 0, null, "E");
        ack = ack.deserialize(packet.getData());
        System.out.println("RECV____________________");
        System.out.println(ack.toString());
        System.out.println();
        computeTimeout(ack.timeStamp, ack.sequence);
        return ack;
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















































