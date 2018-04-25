import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class TCP_recv {

    private DatagramSocket socket;
    private int mtu;
    private int sws;
    private int toalDataRecv;

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
            //receive the data
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            TCP_segm recv = new TCP_segm(0, 0, 0, 0, (short) 0, null, "D");
            recv = recv.deserialize(packet.getData());


            //write the data
            String str = new String(recv.getData(), 0, recv.getLength()/4);
            wr.write(str);
            wr.flush();

            //TODO: manage TimeOut and SequenceNumber

            //send ACK
            byte[] emptyBuf = new byte[0];
            TCP_segm ack = new TCP_segm(0, recv.sequence + 1, recv.timeStamp, 0, (short) 0, emptyBuf, "A");
            packet = new DatagramPacket(ack.serialize(), ack.totalLength, packet.getAddress(), packet.getPort());
            socket.send(packet);
        }
    }

    public boolean handshake(int initSeqNum) throws IOException {

        byte[] empty = new byte[0];

        //Receive SYN
        byte[] buf = new byte[24];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        TCP_segm syn = new TCP_segm(0, 0, 0, 0, (short) 0, empty, "D");
        syn = syn.deserialize(packet.getData());
        System.out.println("SYN RECV____________________");
        System.out.println(syn.toString());

        //Send SYNACK
        byte[] emptyBuf = new byte[0];
        TCP_segm synack = new TCP_segm(initSeqNum, syn.sequence+1, syn.timeStamp, 0, (short) 0, empty, "SA");
        synack.serialize();
        packet = new DatagramPacket(synack.serialize(), synack.totalLength, packet.getAddress(), packet.getPort());
        socket.send(packet);
        System.out.println("SYNACK SENT______________________");
        System.out.println(synack.toString());

        //Receive ACK
        buf = new byte[24];
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        TCP_segm ack = new TCP_segm(0, 0, 0, 0, (short) 0, empty, "D");
        syn = ack.deserialize(packet.getData());
        System.out.println("ACK RECV____________________");
        System.out.println(syn.toString());

        return true;
    }

}



















