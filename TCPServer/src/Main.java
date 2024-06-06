import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Main {
    private static final int BUFFER_SIZE = 256;
    private static final int MAX_PACKETS = 12;

    // Combines the header and body into a single byte array
    public static byte[] combineHeaderAndData(byte[] header, byte[] body) {
        byte[] paketBytes = new byte[header.length + body.length];
        System.arraycopy(header, 0, paketBytes, 0, header.length);
        System.arraycopy(body, 0, paketBytes, header.length, body.length);
        return paketBytes;
    }

    // Combines the header information into a byte array
    public static byte[] combineHeader(int reserved, int statusCode, int fileSize, int currentFileSize, int packetNumber, int checksum) {
        ByteBuffer packetBuffer = ByteBuffer.allocate(16); // Allocate 16 bytes for the header
        packetBuffer.putShort((short) (packetNumber & 0xFFF));
        packetBuffer.putInt(currentFileSize);
        packetBuffer.putInt(fileSize);
        packetBuffer.put((byte) (reserved));
        packetBuffer.put((byte) (statusCode & 0x7));
        packetBuffer.putInt(checksum);
        return packetBuffer.array();
    }

    // Parses the header from a byte array to a string
    public static String parseHeader(byte[] header) {
        ByteBuffer packetBuffer = ByteBuffer.wrap(header);
        int packetNumber = packetBuffer.getShort() & 0xFFFF;
        long currentFileSize = packetBuffer.getInt() & 0xFFFFFFFFL;
        long fileSize = packetBuffer.getInt() & 0xFFFFFFFFL;
        int reserved = packetBuffer.get();
        int statusCode = packetBuffer.get() & 0x7;
        int checksum = packetBuffer.getInt();
        return reserved + " " + statusCode + " " + fileSize + " " + currentFileSize + " " + packetNumber + " " + checksum;
    }

    // Calculates the checksum of a byte array
    public static long calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum ^= b;
        }
        return checksum;
    }

    // Compares the calculated checksum with the received checksum
    public static boolean compareChecksum(byte[] data, long receivedChecksum) {
        long calculatedChecksum = calculateChecksum(data);
        return calculatedChecksum == receivedChecksum;
    }

    public static void main(String[] args) {
        int port = 12345; // Server port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket client1Socket = serverSocket.accept(); // Accept connection from client 1
                System.out.println("Client 1 connected");

                Socket client2Socket = serverSocket.accept(); // Accept connection from client 2
                System.out.println("Client 2 connected");

                Thread clientThread = new Thread(() -> {
                    try {
                        handleClient(client1Socket, client2Socket); // Handle the clients in a separate thread
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                clientThread.start();
            }
        } catch (SocketException se) {
            System.out.println("Client connection closed.");
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Handles the communication with the clients
    public static void handleClient(Socket client1Socket, Socket client2Socket) throws IOException {
        try (DataInputStream client1InputStream = new DataInputStream(client1Socket.getInputStream());
             DataOutputStream client1OutputStream = new DataOutputStream(client1Socket.getOutputStream());
             DataOutputStream client2OutputStream = new DataOutputStream(client2Socket.getOutputStream());
             DataInputStream client2InputStream = new DataInputStream(client2Socket.getInputStream())) {

            byte[][] buffer = new byte[MAX_PACKETS][BUFFER_SIZE];
            byte[] body;
            byte[] receivedPacket = new byte[280];
            byte[] receivedHeader = new byte[16];
            String receivedHeaderStr;
            int statusCode;
            int increaseFileSize;
            int increaseRate;
            int currentPacket;
            int counter = 0;
            int receivedChecksum;
            int[] bodyLength = new int[12];
            int[] checksum = new int[12];

            String filepath = client1InputStream.readUTF(); // Read the file path from client 1
            client1InputStream.read(receivedPacket);
            byte[] header = Arrays.copyOfRange(receivedPacket, 0, 16);
            receivedHeaderStr = parseHeader(header); // Parse the received header
            String[] datas = receivedHeaderStr.split(" ");
            int fileSize = Integer.parseInt(datas[2]);
            int lastPacket = fileSize % 256; // get last packet size
            int totalPacket = Integer.parseInt(datas[4]); // get total packet number
            int checksumFile = Integer.parseInt(datas[5]); // get checksum for all file

            byte[] packet = combineHeader(0, 0, 0, 0, 0, 0);
            client1OutputStream.write(packet); // Send initial response to client 1

            packet = combineHeader(0, 0, fileSize, 0, totalPacket, checksumFile);
            client2OutputStream.write(packet); // Send initial header to client 2
            client2OutputStream.writeUTF(filepath); // Send file path to client 2
            client2InputStream.read(receivedHeader);

            for (int j = 0; j < totalPacket; j += 12) { // Reading incoming files according to the size of the buffer
                currentPacket = totalPacket - j;
                statusCode = 1;
                if (currentPacket >= 12) {
                    increaseRate = 11;
                } else {
                    increaseRate = currentPacket - 1;
                }
                increaseFileSize = 0;

                for (int i = 0; i <= increaseRate; i++) {
                    counter++;
                    if (counter == 1) {
                        receivedPacket = new byte[16 + lastPacket];
                    } else {
                        receivedPacket = new byte[272];
                    }
                    client1InputStream.read(receivedPacket); // Read the packet from client 1
                    header = Arrays.copyOfRange(receivedPacket, 0, 16); // get header
                    body = Arrays.copyOfRange(receivedPacket, 16, receivedPacket.length); // get data
                    bodyLength[i] = body.length;
                    increaseFileSize += body.length;
                    receivedHeaderStr = parseHeader(header);
                    datas = receivedHeaderStr.split(" ");
                    receivedChecksum = Integer.parseInt(datas[5]); // get received checksum
                    checksum[i] = receivedChecksum;
                    boolean isValidChecksum = compareChecksum(body, receivedChecksum); // check received checksum and received body's checksum are equal or not
                    if (isValidChecksum) {
                        buffer[i] = body;
                    } else {
                        buffer[i] = body;
                        statusCode = 2; // Set status code to 2 if checksum does not match
                    }
                }

                packet = combineHeader(0, statusCode, 0, 0, 0, 0); // Send status code to client 1
                client1OutputStream.write(packet);

                if (statusCode == 1) { // if there is no error packets from client1
                    for (int i = 0; i <= increaseRate; i++) {
                        packet = combineHeader(0, 1, fileSize, bodyLength[i], 0, checksum[i]);
                        byte[] dataPacket = combineHeaderAndData(packet, buffer[i]);
                        client2OutputStream.write(dataPacket); // Send valid data packets to client 2
                    }
                    client2InputStream.read(receivedHeader);
                    receivedHeaderStr = parseHeader(receivedHeader);
                    datas = receivedHeaderStr.split(" ");
                    if (Integer.parseInt(datas[1]) == 4) { // if there is no error sent packets to client2
                        statusCode = 7; // Set status code to 7 if client2 acknowledges
                        packet = combineHeader(0, statusCode, 0, 0, 0, 0);
                        client1OutputStream.write(packet); // Notifies client1 that the buffer is empty
                    } else if (Integer.parseInt(datas[1]) == 2) { // if there is error sent packets to client2
                        int StatusCode = Integer.parseInt(datas[1]);
                        while (StatusCode == 2) { // keep sending packets to client 2 until they go right
                            for (int i = 0; i <= increaseRate; i++) {
                                packet = combineHeader(0, 1, fileSize, bodyLength[i], 0, checksum[i]);
                                byte[] dataPacket = combineHeaderAndData(packet, buffer[i]);
                                client2OutputStream.write(dataPacket); // Resend data packets if client 2 requests
                            }
                            client2InputStream.read(receivedHeader);
                            receivedHeaderStr = parseHeader(receivedHeader);
                            datas = receivedHeaderStr.split(" ");
                            statusCode = Integer.parseInt(datas[1]);
                            if (statusCode == 4) {
                                break; // Break loop if client2 acknowledges
                            }
                        }
                    }
                } else if (statusCode == 2) { // If packets from client1 are corrupted, re-read the re-sent packets
                    j -= 12;
                    totalPacket -= increaseFileSize;
                    totalPacket += (increaseRate + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (!client1Socket.isClosed()) {
                    client1Socket.close(); // Close client 1 socket
                }
                if (!client2Socket.isClosed()) {
                    client2Socket.close(); // Close client 2 socket
                }
            } catch (IOException e){
            e.printStackTrace();
            }
        }
    }
}

