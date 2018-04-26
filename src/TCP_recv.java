import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TCP_recv {

    private DatagramSocket socket;
    private int mtu;
    private int sws;
    private int senderPort;
    private InetAddress senderIP;
    private int senderSeqNum;


    public TCP_recv(DatagramSocket socket, int mtu, int sws) {
        this.socket = socket;
        this.mtu = mtu;
        this.sws = sws;
    }

    public void receive() throws IOException {
        byte[] buf = new byte[this.mtu];
        boolean running = true;
        Writer wr = new FileWriter("r.txt");

        //TODO: Handle connection starting with SYN
        //TODO: implement triple duplicate ACKS
        //TODO: Handle connection end with FIN
        while (running) {
            TCP_segm recv = receiveData();  //receive the data


            if(recv.getFlag().contains("D")){
                String str = new String(recv.getData(), 0, recv.getLength());
                wr.write(str);
                wr.flush();
            }
            else if(recv.getFlag().contains("F")){
                connectionTeardown(recv);
            }


            //TODO: manage TimeOut and SequenceNumber

            sendAck("A", recv.sequence, recv.timeStamp);//send ACK
        }
    }

    public void handshake(int initSeqNum) throws IOException {
        TCP_segm recv;
        recv = receiveData();                                   //receive SYN
        sendAck("SA", recv.sequence, recv.timeStamp);      //Send SYNACK
        receiveData();                                          //Receive ACK
    }

    public void connectionTeardown(TCP_segm recv) throws IOException {
        sendAck("A", recv.sequence, recv.timeStamp);       //Send ACK
        sendAck("FA", recv.sequence + 1, System.nanoTime()); //Send FIN + ACK
        receiveData();                                          //Receive ACK
        System.exit(0);
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
        String str = new String(recv.getData(), 0, recv.getLength()/4);

        if(recv.sequence == 0){
            this.senderPort = packet.getPort();
            this.senderIP = packet.getAddress();
        }

        return recv;
    }

    public void sendAck(String flag, int prevSeqNum, long prevTimeStamp) throws IOException {
        byte[] empty = new byte[0];
        TCP_segm send = new TCP_segm(0, prevSeqNum+1, prevTimeStamp, 0, (short) 0, empty, flag);
        send.serialize();
        DatagramPacket packet = new DatagramPacket(send.serialize(), send.getLength() + 24, senderIP, senderPort);
        socket.send(packet);
        System.out.println("SENT______________________");
        System.out.println(send.toString());
        System.out.println();
    }


}



















