import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class TCP_send extends Thread {

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
        this.sws = sws;
    }

    public void send(TCP_segm[] segmArr) throws InterruptedException {

        HashMap<Integer, TCP_segm> inTransit = new HashMap<Integer, TCP_segm>();
        ReentrantLock lock = new ReentrantLock();

        Thread sendData = new Thread(new Runnable() {
            @Override
            public void run() {
                int segsSent = 0;
                while (segsSent < segmArr.length) {
                    try {
                        lock.lock();
                        if (inTransit.size() < sws) {
                            segmArr[segsSent].setTimeStamp(System.nanoTime());
                            sendData(segmArr[segsSent]);
                            inTransit.put(segmArr[segsSent].sequence, segmArr[segsSent]);
                            segsSent++;
                        }
                        for(HashMap.Entry<Integer, TCP_segm> entry : inTransit.entrySet()) {
                            if((System.nanoTime() - entry.getValue().timeStamp) > timeout){
                                inTransit.remove(entry);
                                entry.getValue().setTimeStamp(System.nanoTime());
                                sendData(entry.getValue());
                                inTransit.put(entry.getValue().sequence, entry.getValue());
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }
            }
        });

        sendData.start();

        int ackNum = 0;
        while (ackNum < segmArr.length) {
            try {
                TCP_segm ack = receiveAck();
                lock.lock();
                inTransit.remove((ack.acknowledgment - 1));
                ackNum++;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
        sendData.join();
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
        //System.out.println("Sending_______________");
        //System.out.println(segment.toString() + "\n");
        socket.send(packet);
    }

    public void sendNoData(String flag, int seqNum) throws IOException {
        int size = 0;
        byte[] buf = new byte[size];
        TCP_segm tcpSegm = new TCP_segm(seqNum, 0, System.nanoTime(), 0, (short) 0, buf, flag);
        tcpSegm.serialize();
        //System.out.println("Sending_______________");
        //System.out.println(tcpSegm.toString() + "\n");
        DatagramPacket packet = new DatagramPacket(tcpSegm.serialize(), 0, tcpSegm.getLength() + 24, this.remote_IP, this.remote_port);
        socket.send(packet);
    }

    public TCP_segm receiveAck() throws IOException {
        byte[] buf = new byte[24];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        TCP_segm ack = new TCP_segm(0, 0, 0, 0, (short) 0, null, "E");
        ack = ack.deserialize(packet.getData());
        //System.out.println("\t\t\t\t\t\tRTT in RECV" + (System.nanoTime() - ack.timeStamp));
        computeTimeout(ack.timeStamp, ack.acknowledgment-1);
        //System.out.println("Recving_______________");
        //System.out.println(ack.toString() + "\n");
        return ack;
    }

    public void computeTimeout(long timestamp, int sequenceNum) {
        //System.out.println("/t/t/t/t/tCOMPUTE TIMEOUT CALLED: " + timestamp + " || " + sequenceNum);
        double a = 0.875;
        double b = 0.75;
        if (sequenceNum == 0) {
            this.ERTT = System.nanoTime() - timestamp;
            this.EDEV = 0L;
            this.timeout = 2L * this.ERTT;
        } else {
            System.out.println("computeTimeout________________");
            long SRTT = System.nanoTime() - timestamp;
            System.out.println("SRTT: " + SRTT);
            System.out.println("ERTT before: " + ERTT);
            long SDEV = Math.abs(SRTT - this.ERTT);
            System.out.println("SDEV: " + SDEV);
            this.ERTT = (long) (a * this.ERTT) + (long) ((1 - a) * SRTT);
            System.out.println("ERTT: " + this.ERTT);
            this.EDEV = (long) (b * this.EDEV) + (long) ((1 - b) * SDEV);
            System.out.println("EDEV: " + EDEV);
            this.timeout = this.ERTT + (4L * this.EDEV);
            System.out.println("timeout: " + this.timeout);
        }
        System.out.println("TIMEOUT TIME: " + this.timeout);
    }
}















































