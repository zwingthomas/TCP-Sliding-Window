import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TCP_recv {

    private DatagramSocket socket;
    private int mtu;
    private int sws;
    private int senderPort;
    private InetAddress senderIP;
    private String fileName;

    public TCP_recv(DatagramSocket socket, int mtu, int sws) {
        this.socket = socket;
        this.mtu = mtu;
        this.sws = sws;
    }

    public void receive() throws IOException {
        int acknowledgment;
        boolean running = true;

        HashMap<Integer, byte[]> dataBuf = new HashMap<>();

        while (running) {
            TCP_segm recv = receiveData();  //receive the data
            acknowledgment = recv.getSequence() + 1;

            //Fast Retransmit in the case that the checksums do not match
            short recvd_checksum = recv.getChecksum();
            recv.serialize(); //recompute checksum
            if(recv.getChecksum() != recvd_checksum){
                sendAck("A", 0, recv.sequence, recv.timeStamp); //TRIPLE
                sendAck("A", 0, recv.sequence, recv.timeStamp); //DUPLICATE
                sendAck("A", 0, recv.sequence, recv.timeStamp); //ACK
                sendAck("A", 0, recv.sequence, recv.timeStamp);
                continue;
            }

            if(recv.getFlag().contains("D")){
                dataBuf.put(recv.sequence, recv.getData());
            }
            else if(recv.getFlag().contains("F")){
                this.fileName = new String(recv.getData());
                connectionTeardown(recv);
                running = false;
                break;
            }

            sendAck("A", 0, acknowledgment, recv.timeStamp);

        }
        Writer wr = new FileWriter(fileName + "1");
        //sort HashMap and print it out
        Object[] keys = dataBuf.keySet().toArray();
        Arrays.sort(keys);
        for(Object key : keys){
            String str = new String(dataBuf.get(key));
            wr.write(str);
            wr.flush();
        }
        System.exit(0);
    }

    public void handshake(int initSeqNum) throws IOException {
        TCP_segm recv;
        recv = receiveData();                                                                   //receive SYN
        sendAck("SA", 0, recv.sequence+1, recv.timeStamp);      //Send SYNACK
        receiveData();                                                                          //Receive ACK
    }

    boolean teardown_recv = false;

    public void connectionTeardown(TCP_segm recv) throws IOException {

        Thread receiveTeardown = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    //Receive Ack
                    long time_last_recv = System.nanoTime();
                    TCP_segm ack = new TCP_segm(0, 0, 0, 0, (short) 0, new byte[0], "E");
                    while(!ack.getFlag().contains("A") && System.nanoTime() - time_last_recv < 5000000000L) {
                        ack = receiveData();
                        time_last_recv = System.nanoTime();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                teardown_recv = true;
            }
        });

        int retransmissions = 0;
        receiveTeardown.start();

        while(!teardown_recv) {
            long time_sent = System.nanoTime();
            //Send ACK and FINACK
            sendAck("A", 0, recv.sequence + 1, recv.timeStamp);       //Send ACK
            sendAck("FA", 1,recv.sequence + 1, System.nanoTime()); //Send FIN + ACK
            retransmissions++;
            if(retransmissions > 16){
                System.out.println("Retransmission Error: Exceeded MAX Retransmissions");
                System.exit(0);
            }
            while(time_sent + 5000000000L > System.nanoTime() && !teardown_recv){
                //donothing
            }
        }
    }

    public TCP_segm receiveData() throws IOException {
        byte[] empty = new byte[0];
        byte[] buf = new byte[mtu];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        TCP_segm recv = new TCP_segm(0, 0, 0, 0, (short) 0, empty, "E");
        recv = recv.deserialize(packet.getData());
        System.out.println("rcv " + System.nanoTime() / 1000000000 + " " + recv.getFlag() +
                " " + recv.getSequence() + " " + recv.getData().length + " " + recv.getAcknowlegment());
        if(recv.sequence == 0){
            this.senderPort = packet.getPort();
            this.senderIP = packet.getAddress();
        }

        return recv;
    }

    //TODO: send duplicate ACKS if packet is received out of sequence
    public void sendAck(String flag, int sequenceNum, int nextExpectedSeq, long prevTimeStamp) throws IOException {
        byte[] empty = new byte[0];
        TCP_segm send = new TCP_segm(sequenceNum, nextExpectedSeq, prevTimeStamp, 0, (short) 0, empty, flag);
        send.serialize();
        DatagramPacket packet = new DatagramPacket(send.serialize(), send.getLength() + 24, senderIP, senderPort);
        socket.send(packet);
        System.out.println("snd " + System.nanoTime() / 1000000000 + " " + send.getFlag() +
                " " + send.getSequence() + " " + send.getData().length + " " + send.getAcknowlegment());
    }
}



















