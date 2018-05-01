import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class TCP_send extends Thread {

    private DatagramSocket socket;
    private InetAddress remote_IP;
    private int remote_port;
    private String file_name;
    private int mtu;
    protected int sws;
    private char flag;
    private int sequenceSender;

    HashMap<Integer, Integer> retransmitNum;
    private Map<Integer, Long> sequence_timeout_map;
    private HashMap<Integer, TCP_segm> inTransit;
    ReentrantLock lock;

    //variables for timeOut calculation
    long ERTT;
    long EDEV;
    private long timeout;

    //handshake and teardown variables
    boolean handshake_complete = false;
    boolean teardown_complete = false;
    int last_seqNum = 0;

    //variables for output:
    int data = 0;
    int packets_sent = 0;
    int packets_discarded = 0;
    int packets_discarded_checksum = 0;
    int retransmissions = 0;
    int dup_acknowledgements = 0;


    public TCP_send(DatagramSocket socket, String remote_IP, int remote_port, int sws, char flag, String file_name) throws UnknownHostException {
        this.socket = socket;
        InetAddress addr = InetAddress.getByName(remote_IP);
        this.remote_IP = addr;
        this.remote_port = remote_port;
        this.flag = flag;
        this.timeout = 5000000000L;
        this.sws = sws;
        this.file_name = file_name;
        this.inTransit = new HashMap<Integer, TCP_segm>();
        this.retransmitNum = new HashMap<Integer, Integer>();
    }

    public void send(ArrayList<TCP_segm> segmArr) throws InterruptedException {


        inTransit = new HashMap<>();
        lock = new ReentrantLock();
        Thread sendData = new Thread(new SendDataRunnable(segmArr, this, lock, inTransit));

        sequence_timeout_map = Collections.synchronizedMap(new HashMap<Integer, Long>());

        Timer t = new Timer(true);

        t.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        if(inTransit.size() > 0)
                            check_old_timestamps(inTransit, lock);
                    }
                }
                , 0, 1);

        sendData.start();

        int actual_ackNum = 0;
        HashMap<Integer, Integer> ackNum = new HashMap<>();
        while (ackNum.size() < segmArr.size()) {
            try {
                TCP_segm ack = receiveAck();
                actual_ackNum += 1;
                computeTimeout(ack.timeStamp, ack.acknowledgment - 1);

                synchronized(sequence_timeout_map) {
                    sequence_timeout_map.remove(ack.acknowledgment - 1);
                }
                lock.lock();
                inTransit.remove((ack.acknowledgment - 1));
                ackNum.put(ack.acknowledgment, ack.acknowledgment);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
        sendData.join();
        dup_acknowledgements = actual_ackNum - ackNum.size();
    }

    int initialSeqNum = 0;

    public void handshake(int initSeqNum) throws IOException {

        initialSeqNum = initSeqNum;

        Thread receiveHandshake = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    //Receive SYNACK
                    TCP_segm ack = receiveAck();

                    //Send ACK
                    sendNoData("A",  1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                handshake_complete = true;
            }
        });

        receiveHandshake.start();

        while(!handshake_complete) {
            long time_sent = System.nanoTime();
            sendNoData("S", 0);
            retransmissions++;
            if(retransmissions > 16){
                System.out.println("Retransmission Error: Exceeded MAX Retransmissions");
                System.exit(0);
            }
            while(time_sent + timeout > System.nanoTime() && !handshake_complete){
                //donothing
            }
        }
    }

    public void connectionTeardown() throws IOException {
        Thread receiveTeardown = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    TCP_segm ack = new TCP_segm(0, 0, 0, 0, (short) 0, new byte[0], "E");
                    TCP_segm finAck = new TCP_segm(0, 0, 0, 0, (short) 0, new byte[0], "E");

                    //Receive ACK
                    while(!ack.getFlag().contains("A"))
                        ack = receiveAck();

                    //Receive FINACK
                    while(!ack.getFlag().contains("A") || !ack.getFlag().contains("F"))
                        finAck = receiveAck();

                    //Send ack
                    sendNoData("A", finAck.sequence + 1);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                teardown_complete = true;
            }
        });

        int retransmissions = 0;
        receiveTeardown.start();

        while(!teardown_complete) {
            long time_sent = System.nanoTime();
            //Send FIN
            byte[] buf = file_name.getBytes();
            TCP_segm finalSeg = new TCP_segm(last_seqNum + buf.length, 0, System.nanoTime(), file_name.length(), (short) 0, buf, "F");
            sendData(finalSeg);
            retransmissions++;
            if(retransmissions > 16){
                System.out.println("Retransmission Error: Exceeded MAX Retransmissions");
                System.exit(0);
            }
            while(time_sent + timeout > System.nanoTime() && !teardown_complete){
                //donothing
            }
        }

        end_prints();
        System.exit(0);
    }

    public void sendData(TCP_segm segment) throws IOException {
        segment.setTimeStamp(System.nanoTime());
        segment.serialize();
        DatagramPacket packet = new DatagramPacket(segment.serialize(), 0, segment.getLength() + 24, this.remote_IP, this.remote_port);
        synchronized(sequence_timeout_map) {
            sequence_timeout_map.put(segment.sequence, this.timeout + System.nanoTime());
        }
        //System.out.println("Sending_______________");
        //System.out.println(segment.toString() + "\n");
        socket.send(packet);
        System.out.println("snd " + System.nanoTime() / 1000000000 + " " + segment.getFlag() +
                " " + segment.getSequence() + " " + segment.getData().length + " " + segment.getAcknowlegment());
    }

    public void sendNoData(String flag, int ackNum) throws IOException {
        int size = 0;
        byte[] buf = new byte[size];
        TCP_segm segment = new TCP_segm(0, ackNum, System.nanoTime(), 0, (short) 0, buf, flag);
        segment.serialize();
        DatagramPacket packet = new DatagramPacket(segment.serialize(), 0, segment.getLength() + 24, this.remote_IP, this.remote_port);
        socket.send(packet);
        System.out.println("snd " + System.nanoTime() / 1000000000 + " " + segment.getFlag() +
                " " + segment.getSequence() + " " + segment.getData().length + " " + segment.getAcknowlegment());
    }

    public TCP_segm receiveAck() throws IOException {
        byte[] buf = new byte[24];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        TCP_segm ack = new TCP_segm(0, 0, 0, 0, (short) 0, null, "E");
        ack = ack.deserialize(packet.getData());
        System.out.println("rcv " + System.nanoTime() / 1000000000 + " " + ack.getFlag() +
                " " + ack.getSequence() + " " + ack.getData().length + " " + ack.getAcknowlegment());
        return ack;
    }

    public void computeTimeout(long timestamp, int sequenceNum) {
        double a = 0.875;
        double b = 0.75;
        if (sequenceNum == 0) {
            this.ERTT = System.nanoTime() - timestamp;
            this.EDEV = 0L;
            this.timeout = 2L * this.ERTT;
        } else {
            long SRTT = System.nanoTime() - timestamp;
            long SDEV = Math.abs(SRTT - this.ERTT);
            this.ERTT = (long) (a * this.ERTT) + (long) ((1 - a) * SRTT);
            this.EDEV = (long) (b * this.EDEV) + (long) ((1 - b) * SDEV);
            this.timeout = this.ERTT + (4L * this.EDEV);
        }
    }

    public void check_old_timestamps(HashMap<Integer, TCP_segm> inTransit, ReentrantLock lock) {
        ArrayList<Integer> to_retransmit = new ArrayList<>();
        synchronized(sequence_timeout_map) {
                for (Integer seq_num : sequence_timeout_map.keySet()) {
                        if (inTransit.containsKey(seq_num) && sequence_timeout_map.get(seq_num) < System.nanoTime()) {
                                inTransit.get(seq_num).setTimeStamp(System.nanoTime());
                            try {
                                sendData(inTransit.get(seq_num));
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (NullPointerException e) {
                                System.out.println("Line 247");
                            }
                            packets_discarded += 1;
                            retransmissions += 1;
                        }
                }
            }
//            for (Integer seq_num : to_retransmit) {
//                lock.lock();
//                try {
//                    if (inTransit.size() > 0 && inTransit.containsKey(seq_num)) {
//                        inTransit.get(seq_num).setTimeStamp(System.nanoTime());
//                        sendData(inTransit.get(seq_num));
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                inTransit.put(seq_num, inTransit.get(seq_num));
//                lock.unlock();
//            }

    }


    public void end_prints(){
        System.out.println("\nAmount of Data Transferred/Received: " + data);
        System.out.println("No of Packets Sent/Received: " + packets_sent);
        System.out.println("No of Packets Discarded (Out Of Seq): " + packets_discarded);
        System.out.println("No of Packets Discarded (Checksum): " + packets_discarded_checksum);
        System.out.println("No of Retransmissions: " + retransmissions);
        System.out.println("No of Duplicate Acknowledgements: " + dup_acknowledgements);
    }

}

class SendDataRunnable implements Runnable {
    protected ArrayList<TCP_segm> segmArr;
    public final TCP_send sender;
    ReentrantLock lock;
    HashMap<Integer, TCP_segm> inTransit;

    public SendDataRunnable(ArrayList<TCP_segm> segmArr, TCP_send sender, ReentrantLock lock, HashMap<Integer, TCP_segm> inTransit) {
        this.segmArr = segmArr;
        this.sender = sender;
        this.inTransit = inTransit;
        this.lock = lock;
    }

    public void run() {
        int segsSent = 0;
        while (segsSent < segmArr.size()) {
            try {
                lock.lock();
                if (inTransit.size() < sender.sws) {
                    //keeps track of stats for final printout
                    sender.data += segmArr.get(segsSent).getData().length;
                    sender.packets_sent += 1;

                    //retransmission counter
                    int seqNum = segmArr.get(segsSent).sequence;
                    if(sender.retransmitNum.containsKey(seqNum))
                        sender.retransmitNum.put(seqNum, sender.retransmitNum.get(seqNum)+1);
                    else
                        sender.retransmitNum.put(seqNum, 1);
                    if(sender.retransmitNum.get(seqNum) > 16){
                        System.out.println("Retransmission Error: Exceeded MAX Retransmissions");
                        System.exit(0);
                    }

                    //sends data
                    segmArr.get(segsSent).setTimeStamp(System.nanoTime());
                    sender.sendData(segmArr.get(segsSent));
                    inTransit.put(segmArr.get(segsSent).sequence, segmArr.get(segsSent));
                    segsSent++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
        sender.last_seqNum = segmArr.get(segsSent - 1).sequence;
    }
}













































