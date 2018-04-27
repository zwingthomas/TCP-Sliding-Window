import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class TCP_send extends Thread{

    private DatagramSocket socket;
    private InetAddress remote_IP;
    private int remote_port;
    private String file_name;
    private int mtu;
    private int sws;
    private char flag;
    private int sequenceSender;
    private boolean isSending = true;


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
        this.sws = sws;
    }

    public void send(TCP_segm[] segmArr) throws InterruptedException {

        //Complete connectino setup
        //TODO: manage concurrency with locks
        HashMap<Integer, Boolean> isAcked= new HashMap<Integer, Boolean>();
        ArrayList<Integer> inTransit = new ArrayList<Integer>();

        Thread sendData = new Thread(new Runnable() {
            @Override
            public void run() {
                int segsSent = 0;
                int sendable = sws;

                while(segsSent < segmArr.length || inTransit.size() != 0) {
                    while (segsSent < sendable && segsSent < segmArr.length) {
                        try {
                            System.out.println("\t\t\t\t\t\t"+segsSent);
                            sendData(segmArr[segsSent]);
                            isAcked.put(segmArr[segsSent].sequence, false);
                            inTransit.add(segmArr[segsSent].sequence);
                            segsSent++;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                        //System.out.println("The desired Ack: " + inTransit.get(0));
                        //System.out.println("isAcked.get(): "+isAcked.get(inTransit.get(0)));
                        while (inTransit.size() != 0 && isAcked.get(inTransit.get(0)).equals(true)) {
                            System.out.println("\t\t\tACK REMOVED!!!: " + inTransit.get(0));
                            inTransit.remove(0);
                            sendable++;
                        }
                }
                System.out.println("\t\t\t\t\t\tDONE SENDING!");
                isSending = false;
            }
        });

        sendData.start();

        int ackNum = 0;
        while (ackNum < segmArr.length) {
            try {
                TCP_segm ack = receiveAck();
                isAcked.put((ack.acknowledgment - 1), true);
                System.out.println("\nACK RECIEVED: " + isAcked.toString() +"\n");
                ackNum++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sendData.join();
        System.out.println("\t\t\t\t\t\t\t FREEEEDOMMMMM!!");
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
        DatagramPacket packet = new DatagramPacket(segment.serialize(), 0, segment.getLength() + 24, this.remote_IP, this.remote_port);
        System.out.println("Sending_______________");
        System.out.println(segment.toString()+"\n");
        socket.send(packet);
    }

    public void sendNoData(String flag, int seqNum) throws IOException {
        int size = 0;
        byte[] buf = new byte[size];
        TCP_segm tcpSegm = new TCP_segm(seqNum, 0, System.nanoTime(), 0, (short) 0, buf, flag);
        tcpSegm.serialize();
        System.out.println("Sending_______________");
        System.out.println(tcpSegm.toString()+"\n");
        DatagramPacket packet = new DatagramPacket(tcpSegm.serialize(), 0, tcpSegm.getLength() + 24, this.remote_IP, this.remote_port);
        socket.send(packet);
    }

    public TCP_segm receiveAck() throws IOException{
        byte[] buf = new byte[24];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        TCP_segm ack = new TCP_segm(0, 0, 0, 0, (short) 0, null, "E");
        ack = ack.deserialize(packet.getData());
        System.out.println("Recving_______________");
        System.out.println(ack.toString()+"\n");
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















































