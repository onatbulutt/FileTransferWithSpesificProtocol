# File Transfer with SIMPLE FILE TRANSFER PROTOCOL
The Simple File Transfer Protocol is an internet protocol that enables file transfer from one user to another. To control the file transfer, a server program is utilized between the
two users. This server handles the communication and ensures the integrity and successful delivery of the files. Additionally, the protocol is designed to be
straightforward and easy to implement, making it a suitable choice for basic file transfer needs.

# Installation
Follow the steps below to run and develop this project:
### 1. Installing IntelliJ IDEA Community Edition
We recommend using IntelliJ IDEA Community Edition 2023.1.3 or a newer version for developing the project. IntelliJ IDEA provides a comprehensive integrated development environment for Java development. [Download IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
### 2. Installing JDK 11
To compile and run the project, you need to have Java Development Kit (JDK) 11 or a newer version installed. JDK can be downloaded and installed from the official Java website or via a package manager.
[Oracle JDK Download Page](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)

# Opening the Project
1. Download project as zip.
2. Extract the zip.
3. Open IntelliJ IDEA.
4. From the menu bar, select "File" > "Open".
5. Navigate to your project directory and select server and client respectively.
6. Click on the "Open" button to load the project into IntelliJ IDEA.

# Usage
This project is based on a client-server architecture and provides a communication protocol for file transfer operation. Follow the steps below to start the client and server modules and perform file queries:
### Starting the TCPServer
1. Open the TCPServer program. Start the program by pressing the Run button located in the upper right corner or by using Shift+f10.
2. Keep the program running after it has started, throughout the operations performed.
### Starting the Client1
1. Open the Client1 program. Start the program by pressing the Run button located in the upper right corner or by using Shift+f10.
2. After program is running, enter the file path that you want to send.
3. Keep the program running after it has started, throughout the operations performed.
### Starting the Client2
1. Open the Client2 program. Start the program by pressing the Run button located in the upper right corner or by using Shift+f10.
2. After program is running, file transfer operation is started. After a while, the process is completed depending on the file size.

You can reach details of protocol and file operations in RFC-Report.pdf file.

# Example
In below, there are the images from Client1 and Client2 console respectively while file transfer operation is running.
![image](https://github.com/onatbulutt/FileTransferWithSpesificProtocol/assets/155490196/1d4dfc1a-e4ec-40b8-9439-63031d2fa745)
![image](https://github.com/onatbulutt/FileTransferWithSpesificProtocol/assets/155490196/5a4c3b65-469e-4e29-9d2e-7614e123beed)
