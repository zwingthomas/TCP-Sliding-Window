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
    private int senderSeqNum;
    private String fileName;

    public TCP_recv(DatagramSocket socket, int mtu, int sws) {
        this.socket = socket;
        this.mtu = mtu;
        this.sws = sws;
    }

    public void receive() throws IOException {
        int expectedNextSeq = -1;
        int prevAck = -1;
        byte[] buf = new byte[this.mtu];
        boolean running = true;

        HashMap<Integer, byte[]> dataBuf = new HashMap<>();

        //TODO: implement triple duplicate ACKS
        while (running) {
            TCP_segm recv = receiveData();  //receive the data

            //Checking for out of order packets
            int acknowledgment = recv.sequence;
            if(recv.sequence != expectedNextSeq && expectedNextSeq != -1) {
                acknowledgment = prevAck;
            }

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

            System.out.println(recv.toString());

            if(recv.getFlag().contains("D")){
                dataBuf.put(recv.sequence, recv.getData());
            }
            else if(recv.getFlag().contains("F")){
                this.fileName = new String(recv.getData());
                connectionTeardown(recv);
                running = false;
            }

            //TODO: manage TimeOut and SequenceNumber

            sendAck("A", 0, acknowledgment + 1, recv.timeStamp);
            expectedNextSeq = recv.sequence + recv.getData().length;
            prevAck = acknowledgment;
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
        recv = receiveData();                                   //receive SYN
        sendAck("SA", 500, recv.sequence, recv.timeStamp);      //Send SYNACK
        receiveData();                                          //Receive ACK
    }

    public void connectionTeardown(TCP_segm recv) throws IOException {
        sendAck("A", 0, recv.sequence, recv.timeStamp);       //Send ACK
        sendAck("FA", 501,recv.sequence + 1, System.nanoTime()); //Send FIN + ACK
        receiveData();                                          //Receive ACK
    }

    public TCP_segm receiveData() throws IOException {
        byte[] empty = new byte[0];
        byte[] buf = new byte[mtu];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        TCP_segm recv = new TCP_segm(0, 0, 0, 0, (short) 0, empty, "E");
        recv = recv.deserialize(packet.getData());
        System.out.println("RECV____________________");
        System.out.println(recv.toString());
        System.out.println();
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
        System.out.println("SENT______________________");
        System.out.println(send.toString());
        System.out.println();
    }


}



















