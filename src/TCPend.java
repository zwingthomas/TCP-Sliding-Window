import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TCPend {
    public static void main(String args[]) throws IOException {

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

        System.out.println("-p: " + port);
        System.out.println("-s: " + remote_IP);
        System.out.println("-a: " + remote_port);
        System.out.println("-f: " + file_name);
        System.out.println("-m: " + mtu);
        System.out.println("-c: " + sws);

        int slidingWindow = 0;

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


            int seqNum = 0;
            int totalBytesLoaded = 0;
            int neededNumOfSegments = (int)Math.ceil((double)data.length/(double)(mtu - 24));
            DatagramSocket socket = new DatagramSocket(port);

            for(int segCnt = 0; segCnt < neededNumOfSegments; segCnt++){
                //put data to be sent into a smaller container for transmission
                byte[] dataToBeSent = new byte[mtu];
                for(int dataLoadedForSeg = 0; dataLoadedForSeg < (mtu-24) && totalBytesLoaded < data.length-1; dataLoadedForSeg++) {
                    dataToBeSent[dataLoadedForSeg] = data[totalBytesLoaded];
                    totalBytesLoaded++;
                }
                //send segment
                TCP_segm toSend = new TCP_segm(seqNum, (seqNum+1), System.nanoTime(), dataToBeSent.length + 24, (short) 0, dataToBeSent, 'D');
                toSend.serialize(); //computes the checksum
                TCP_send sender = new TCP_send(socket, remote_IP, remote_port, slidingWindow, 'D');
                TCP_segm ack = sender.send(toSend);

                //receive ACK
                System.out.println("Recieved acknowledgement: "+ ack.flag);
            }
        }

        //for Receiver
        if(remote == true){
            DatagramSocket socket = new DatagramSocket(port);
            TCP_recv receiver = new TCP_recv(socket, mtu, slidingWindow);
            receiver.receive();
        }

        //send data in units of mtu -> http://www.baeldung.com/udp-in-java


        //keep track of packets sent so far which have not been acknowledged


    }
}

