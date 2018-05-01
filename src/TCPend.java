import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TCPend {
    public static void main(String args[]) throws IOException, InterruptedException {

        int port = 0;
        String remote_IP = null;
        int remote_port = 0;
        String file_name = null;
        int mtu = 0;
        int sws = 0;

        boolean remote = true;

        for(int i = 0; i < args.length; i++){
            switch(args[i]){
                case "-p":
                    port = Integer.parseInt(args[i+1]);
                    i++;
                    break;
                case "-s":
                    remote_IP = args[i+1];
                    i++;
                    remote = false;
                    break;
                case "-a":
                    remote_port = Integer.parseInt(args[i+1]);
                    i++;
                    remote = false;
                    break;
                case "-f":
                    file_name = args[i+1];
                    i++;
                    remote = false;
                    break;
                case "-m":
                    mtu = Integer.parseInt(args[i+1]);
                    i++;
                    break;
                case "-c":
                    sws = Integer.parseInt(args[i+1]);
                    i++;
                    break;
            }
        }

        //for Sender
        if(remote == false) {

            //read the file from fileName
            byte[] data = null;
            try {
                Path path = Paths.get(file_name);
                data = Files.readAllBytes(path);
            } catch (java.io.IOException e) {
                System.out.println("Could not read in file.");
            }
            //send first SYN packet
            //TCP_send();

            //put data to be sent into a smaller container for transmission
            int seqNum = 1; //sequence number starts at 1 due to handshake
            int totalBytesLoaded = 0;
            int neededNumOfSegments = (int)Math.ceil((double)data.length/(double)(mtu - 24));
            DatagramSocket socket = new DatagramSocket(port);
            ArrayList<TCP_segm> toSend = new ArrayList<TCP_segm>();
            for(int segCnt = 0; segCnt < neededNumOfSegments; segCnt++){
                byte[] dataToBeSent;
                if(segCnt == neededNumOfSegments - 1)
                    dataToBeSent = new byte[data.length - 1 - totalBytesLoaded];
                else
                    dataToBeSent = new byte[mtu-24];
                int dataLoadedForSeg;
                for(dataLoadedForSeg = 0; dataLoadedForSeg < (mtu-24) && totalBytesLoaded < data.length-1; dataLoadedForSeg++) {
                    dataToBeSent[dataLoadedForSeg] = data[totalBytesLoaded];
                    totalBytesLoaded++;
                }
                toSend.add(new TCP_segm(seqNum, 1, System.nanoTime(), dataLoadedForSeg, (short) 0, dataToBeSent, "D"));
                toSend.get(segCnt).serialize(); //computes the checksum
                seqNum += dataLoadedForSeg;
            }

            TCP_send sender = new TCP_send(socket, remote_IP, remote_port, sws, 'D', file_name);
            sender.handshake(0);
            sender.send(toSend);
            sender.connectionTeardown();
        }

        //for Receiver
        if(remote == true){
            DatagramSocket socket = new DatagramSocket(port);
            TCP_recv receiver = new TCP_recv(socket, mtu, sws);
            receiver.handshake(500);
            receiver.receive();
        }
    }
}

