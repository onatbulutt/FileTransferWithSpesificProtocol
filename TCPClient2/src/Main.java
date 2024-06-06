import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Main {
    private static final int BUFFER_SIZE = 256;
    private static final int MAX_PACKETS = 12;

    // Calculates the checksum of a byte array
    public static int calculateChecksum(byte[] data) {
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

    // Verifies the size of the received file against the actual file size
    public static boolean verifyFileSize(byte[] receivedFileBytes, String filePath) {
        File file = new File(filePath);
        long actualFileSize = file.length();
        long receivedFileSize = receivedFileBytes.length;
        return receivedFileSize == actualFileSize;
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

    public static void main(String[] args) {
        String hostname = "127.0.0.1"; // Server hostname
        int port = 12345; // Server port
        String receivedHeaderStr;
        ByteArrayOutputStream totalBufferStream = new ByteArrayOutputStream();
        byte[][] buffer = new byte[MAX_PACKETS][BUFFER_SIZE];
        int currentPacket;
        int increaseRate;
        int counter = 0;
        int receivedChecksum;
        int increaseFileSize = 0;
        int statusCode;
        byte[] packet;
        byte[] receivedPacket;
        byte[] receivedHeader = new byte[16];

        try (Socket socket1 = new Socket(hostname, port)) {
            DataOutputStream outputStream = new DataOutputStream(socket1.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket1.getInputStream());

            // Read initial header from server
            inputStream.read(receivedHeader);
            receivedHeaderStr = parseHeader(receivedHeader);
            String[] datas = receivedHeaderStr.split(" ");
            int fileSize = Integer.parseInt(datas[2]); // get file size
            int lastPacket = fileSize % 256; // get las packet size
            int totalPacket = Integer.parseInt(datas[4]); // get total packet
            int checksumFile = Integer.parseInt(datas[5]); // get checksum of file
            byte[] totalBuffer;

            // Send initial response to server
            packet = combineHeader(0, 0, 0, 0, 0, 0);
            outputStream.write(packet);

            String filePath = inputStream.readUTF(); // Read file path from server

            for (int j = 0; j < totalPacket; j += 12) {
                currentPacket = totalPacket - j;
                if (currentPacket >= 12) {
                    increaseRate = 11;
                } else {
                    increaseRate = currentPacket - 1;
                }
                statusCode = 4;

                for (int i = 0; i <= increaseRate; i++) {
                    counter++;
                    if (counter == 1) {
                        receivedPacket = new byte[16 + lastPacket];
                    } else {
                        receivedPacket = new byte[272];
                    }
                    increaseFileSize = 0;

                    // Read the packet from server
                    inputStream.read(receivedPacket);
                    byte[] header = Arrays.copyOfRange(receivedPacket, 0, 16);
                    byte[] body = Arrays.copyOfRange(receivedPacket, 16, receivedPacket.length);
                    receivedHeaderStr = parseHeader(header);
                    datas = receivedHeaderStr.split(" ");
                    receivedChecksum = Integer.parseInt(datas[5]);
                    increaseFileSize += Integer.parseInt(datas[3]);
                    boolean isValidChecksum = compareChecksum(body, receivedChecksum); // check received checksum and received body's checksum are equal or not
                    if (isValidChecksum) {
                        buffer[i] = body;
                    } else {
                        System.out.println("Error");
                        buffer[i] = body;
                        statusCode = 2; // Set status code to 2 if checksum does not match
                    }
                }

                if (statusCode == 2) { // If a faulty file is received, read the files sent again by the server
                    packet = combineHeader(0, statusCode, 0, 0, 0, 0);
                    outputStream.write(packet);
                    j -= 12;
                    totalPacket -= increaseFileSize;
                    totalPacket += (increaseRate + 1);
                } else if (statusCode == 4) { // If the packets arrived successfully, add them to the buffer
                    for (int k = 0; k <= increaseRate; k++) {
                        totalBufferStream.write(buffer[k]);
                    }
                    packet = combineHeader(0, statusCode, 0, 0, 0, 0); // sen status code to server
                    outputStream.write(packet);
                }
            }

            totalBuffer = totalBufferStream.toByteArray(); // Convert the accumulated byte array output stream to a byte array
            int checksumFile2 = calculateChecksum(totalBuffer);
            System.out.println("Process is done.");
            System.out.println("Results of process: ");

            // Verify file size
            boolean fileSizeCheck = verifyFileSize(totalBuffer, filePath);
            if (fileSizeCheck) {
                System.out.println("The size of the sent file and the received file are the same");
            } else {
                System.out.println("The size of the sent file and the received file are not the same");
            }

            // Verify file contents
            if (checksumFile == checksumFile2) {
                System.out.println("The contents of the sent file and the incoming file are the same. SUCCESS");
            } else {
                System.out.println("The contents of the sent file and the incoming file are not the same. FAILED");
            }
        } catch (UnknownHostException e) {
            System.out.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Situation: " + e.getMessage());
        }
    }
}
