package fcn.project.chord.node.comm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fcn.project.chord.node.ChordNode;

public class ConnectionListener implements Runnable{

	private ChordNode node = null; 
	private ServerSocket socket = null;
	private ExecutorService execServ = null;
	private FileWriter logfilewriter = null;
	public ConnectionListener(ChordNode node) throws IOException{
		this.node = node;
		int portNumber = node.getPort();
		InetAddress addr = node.getIP();		
		this.socket = new ServerSocket(portNumber, 50, addr);
		this.execServ = Executors.newCachedThreadPool();
		
	}
	
	public void run() {
		Socket childSocket = null;
		System.out.println("Connection Listener running ...");
		while(true){
			try {
				childSocket = socket.accept();
//				System.out.println("Connection request accepted");
				logfilewriter = new FileWriter("Communicator.log", true);
				Communicator childComm = new Communicator(childSocket, this.node, logfilewriter);
				execServ.execute(childComm);
			} catch (IOException e) {
				System.out.println("\n error in connection listener ");
				e.printStackTrace();
			}
			
		}
	}

}
