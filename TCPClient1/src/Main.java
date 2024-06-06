import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;


public class Main {
    public static boolean isFileSizeLessThan100KB(String filePath) {  // function for checking file size
        File file = new File(filePath);
        long fileSizeInBytes = file.length();
        long fileSizeInKB = fileSizeInBytes / 1024;
        return fileSizeInKB < 100;
    }
    public static boolean checkFileExistence(String filePath) { // function for checking file existance
        File file = new File(filePath);
        return file.exists() && file.isFile();
    }
    public static byte[] combineHeader(int reserved, int statusCode, int fileSize, int currentFileSize, int packetnumber, int checksum) {
        // function for creating header
        ByteBuffer packetBuffer = ByteBuffer.allocate(16);
        packetBuffer.putShort((short) (packetnumber & 0xFFF));
        packetBuffer.putInt(currentFileSize);
        packetBuffer.putInt(fileSize);
        packetBuffer.put((byte) (reserved));
        packetBuffer.put((byte) (statusCode & 0x7));
        packetBuffer.putInt(checksum);
        return packetBuffer.array();
    }
    public static byte[] combineHeaderAndData(byte[] header, byte[] body) { //function for combine header and data
        byte[] paketBytes = new byte[header.length + body.length];
        System.arraycopy(header, 0, paketBytes, 0, header.length);
        System.arraycopy(body, 0, paketBytes, header.length, body.length);
        return paketBytes;
    }
    public static String parseHeader(byte[] header) { // function for parsing header
        ByteBuffer packetBuffer = ByteBuffer.wrap(header);
        int packetnumber = packetBuffer.getShort() & 0xFFFF;
        long currentFileSize = packetBuffer.getInt() & 0xFFFFFFFFL;
        long fileSize = packetBuffer.getInt() & 0xFFFFFFFFL;
        int reserved = packetBuffer.get();
        int statusCode = packetBuffer.get() & 0x7;
        int checksum = packetBuffer.getInt();
        return reserved + " " + statusCode + " " + fileSize + " " + currentFileSize + " " + packetnumber + " " +checksum +" " + checksum;
    }
    public static int calculateChecksum(File file) throws IOException { // function for calculating checksum for file
        byte[] data = readFileToByteArray(file);
        int checksum = 0;
        for (byte b : data) {
            checksum ^= b;
        }
        return checksum;
    }
    private static byte[] readFileToByteArray(File file) throws IOException { // function for convert file to byte array
        FileInputStream fis = null;
        byte[] data = new byte[(int) file.length()];
        try {
            fis = new FileInputStream(file);
            fis.read(data);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return data;
    }
    public static byte[] getSpecificPart(File inputFile, int chunkSize, int partNumber) { // Function that splits the file into packets
        try {
            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] buffer = new byte[chunkSize];
            int bytesRead;
            int currentPart = 1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (currentPart == partNumber) {
                    inputStream.close();
                    if (bytesRead < chunkSize) {
                        byte[] exactBuffer = new byte[bytesRead];
                        System.arraycopy(buffer, 0, exactBuffer, 0, bytesRead);
                        return exactBuffer;
                    }
                    return buffer;
                }
                currentPart++;
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static int calculateChecksum(byte[] data) { // function for calculating checksum for byte array
        int checksum = 0;
        for (byte b : data) {
            checksum ^= b;
        }
        return checksum;
    }

    public static void main(String[] args) {

        String hostname = "127.0.0.1"; // hostname
        int port = 12345; // port number
        int statusCode = 0;
        int reserved = 0;
        int fileSize;
        int increaseCurrentSize = 0;
        int currentFileSize = 0;
        int packetNumber;
        String receivedHeader;
        int checksum;
        byte[] data;
        byte[] packet;
        byte[] receivedHeaderBuffer = new byte[16];

        try (Socket socket1 = new Socket(hostname, port)) { // creating socket
            DataOutputStream outputStream = new DataOutputStream(socket1.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket1.getInputStream());
            Scanner scanner = new Scanner(System.in);
            System.out.println("Please enter the file path of the file you want to send.");
            String filePath = scanner.nextLine();
            File file = new File(filePath);
            int checksumFile = calculateChecksum(file); // calculate checksum value for the file
            fileSize = (int) file.length(); // get the size of the file
            int totalPacket = fileSize / 256;
            int lastPacketSize = fileSize % 256;
            int increaseRate;
            int currentPacket;

            if (lastPacketSize != 0) {
                totalPacket++;
            }

            int staticTotalPacket = totalPacket;
            int lastIndex = filePath.lastIndexOf('.');
            String extension = filePath.substring(lastIndex); // get the file extension
            boolean isLessThan100KB = isFileSizeLessThan100KB(filePath); // check if the file size is less than 100KB
            boolean isFileExist = checkFileExistence(filePath); // check if the file exists

            if (isFileExist) {
                if (extension.equals(".txt") || extension.equals(".bin")) {
                    if (isLessThan100KB) {
                        System.out.println("File size is less than 100 KB. You cannot send files smaller than 100 KB");
                    } else {
                        System.out.println("Started file transfer");
                        packet = combineHeader(0, 0, fileSize, 0, totalPacket, checksumFile); // combine header for the first packet
                        outputStream.writeUTF(filePath); // send file path
                        outputStream.write(packet); // send the header packet (first packet to server)
                        inputStream.read(receivedHeaderBuffer); // read the response header (first packet form server)

                        //Send file fragments one after the other according to the server's buffer size
                        for (int j = 0; j < staticTotalPacket; j += 12) {
                            currentPacket = staticTotalPacket - j;
                            if (currentPacket >= 12) {
                                increaseRate = 11;
                            } else {
                                increaseRate = currentPacket - 1;
                            }
                            increaseCurrentSize = 0;

                            for (int i = 0; i <= increaseRate; i++) {
                                data = getSpecificPart(file, 256, totalPacket); // get a part of the file
                                checksum = calculateChecksum(data); // calculate checksum for the data
                                totalPacket--;
                                packetNumber = totalPacket + 1;
                                currentFileSize += data.length;
                                increaseCurrentSize += data.length;
                                packet = combineHeader(reserved, statusCode, fileSize, currentFileSize, packetNumber, checksum); // combine header
                                packet = combineHeaderAndData(packet, data); // combine header and data
                                outputStream.write(packet); // send the packet
                            }
                            inputStream.read(receivedHeaderBuffer); // read the response header
                            receivedHeader = parseHeader(receivedHeaderBuffer); // parse the header
                            String[] datas = receivedHeader.split(" ");
                            if (Integer.parseInt(datas[1]) == 1) { // if status code is 1, wait
                                inputStream.read(receivedHeaderBuffer); // read another packet from the server
                                receivedHeader = parseHeader(receivedHeaderBuffer);
                                datas = receivedHeader.split(" ");
                                if (Integer.parseInt(datas[1]) == 7) { // if status code is 7, continue
                                    // Continue the process
                                }
                            } else if (Integer.parseInt(datas[1]) == 2) { // if status code is 2, send again
                                j -= 12;
                                currentFileSize -= increaseCurrentSize;
                                totalPacket += (increaseRate + 1);
                            }
                        }
                    }
                } else {
                    System.out.println("Only text and binary files can be used.");
                }
            } else {
                System.out.println("File does not exist.");
            }
        } catch (UnknownHostException e) {
            System.out.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Situation: " + e.getMessage());
        }
    }
}


