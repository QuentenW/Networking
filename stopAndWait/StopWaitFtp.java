
/**
 * 
 * @author 	Quenten Welch
 * @version	2024
 *
 */

import java.util.logging.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class StopWaitFtp {

	private static final Logger logger = Logger.getLogger("StopWaitFtp"); // global logger
	private int timeout;
	private DatagramSocket udpSocket;
	private Socket tcpSocket;
	private DataOutputStream tcpOut;
	private DataInputStream tcpIn;

	// Variables to be set during the TCP handshake
	private int initialSeqNum;
	private String serverName;
	private int serverUdpPort;
	private Timer timer;

	class TimeoutHandler extends TimerTask {
		private DatagramPacket packet;
		private int seqNum;

		public TimeoutHandler(DatagramPacket packet, int seqNum) {
			this.packet = packet;
			this.seqNum = seqNum;
		}

		@Override
		public void run() {
			try {
				System.out.println("timeout");
				udpSocket.send(packet); // Resend the packet
				System.out.println("retx <" + seqNum + ">");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Constructor to initialize the program
	 * 
	 * @param timeout The time-out interval for the retransmission timer, in
	 *                milli-seconds
	 */
	public StopWaitFtp(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Send the specified file to the specified remote server.
	 * 
	 * @param serverName Name of the remote server
	 * @param serverPort Port number of the remote server
	 * @param fileName   Name of the file to be trasferred to the rmeote server
	 * @return true if the file transfer completed successfully, false otherwise
	 */
	public boolean send(String serverName, int serverPort, String fileName) {
		try {
			// Initialize TCP and UDP sockets
			initializeConnections(serverName, serverPort);

			// Perform the TCP handshake
			if (!tcpHandshake(fileName)) {
				System.out.println("TCP Handshake failed");
				return false;
			}

			// Send file content over UDP using Stop-and-Wait protocol
			return sendFileContent(fileName);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			// Close resources
			closeConnections();
		}
	}

	private void initializeConnections(String serverName, int serverPort) throws IOException {
		// Initialize TCP connection
		tcpSocket = new Socket(serverName, serverPort);
		tcpOut = new DataOutputStream(tcpSocket.getOutputStream());
		tcpIn = new DataInputStream(tcpSocket.getInputStream());

		// Initialize UDP socket
		udpSocket = new DatagramSocket();
	}

	private boolean tcpHandshake(String fileName) throws IOException {
		// Send file name, file length, and local UDP port
		File file = new File(fileName);
		tcpOut.writeUTF(file.getName());
		tcpOut.writeLong(file.length());
		tcpOut.writeInt(udpSocket.getLocalPort());
		tcpOut.flush();

		// Receive server UDP port and initial sequence number
		serverUdpPort = tcpIn.readInt();
		// System.out.println("Received server UDP port: " + serverUdpPort); // Debug
		// print

		initialSeqNum = tcpIn.readInt();

		// need to store serverUdpPort and initialSeqNum for later use?
		return true; // Return true if handshake is successful
	}

	private boolean sendFileContent(String fileName) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(fileName);
		byte[] buffer = new byte[FtpSegment.MAX_PAYLOAD_SIZE];
		int bytesRead;
		int seqNum = initialSeqNum;
		// System.out.println("sendFileContent: server UDP port = " + serverUdpPort);
		while ((bytesRead = fileInputStream.read(buffer)) != -1) {
			FtpSegment segment = new FtpSegment(seqNum, buffer, bytesRead);
			DatagramPacket packet = FtpSegment.makePacket(segment, InetAddress.getByName(serverName), serverUdpPort);
			udpSocket.send(packet);
			System.out.println("send <" + seqNum + ">");

			// Start the timer for retransmission
			if (timer != null) {
				timer.cancel(); // Cancel the previous timer
			}
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimeoutHandler(packet, seqNum), timeout, timeout);

			if (!waitForAck(seqNum)) {
				return false; // Failed to receive ACK
			}

			seqNum++; // Increment sequence number for the next segment
		}

		fileInputStream.close();
		return true; // File transfer completed successfully
	}

	private boolean waitForAck(int expectedSeqNum) throws IOException {
		byte[] ackBuffer = new byte[FtpSegment.MAX_SEGMENT_SIZE];
		DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);

		while (true) {
			udpSocket.receive(ackPacket);
			FtpSegment ackSegment = new FtpSegment(ackPacket);
			int ackNum = ackSegment.getSeqNum();
			System.out.println("ack <" + ackNum + ">");

			if (ackNum == expectedSeqNum + 1) {
				if (timer != null) {
					timer.cancel(); // Cancel the timer on receiving the correct ACK
					timer = null; // Set the timer to null to indicate it's no longer active
				}
				return true;
			}
		}
	}

	private void closeConnections() {
		try {
			if (tcpOut != null)
				tcpOut.close();
			if (tcpIn != null)
				tcpIn.close();
			if (tcpSocket != null)
				tcpSocket.close();
			if (udpSocket != null)
				udpSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

} // end of class