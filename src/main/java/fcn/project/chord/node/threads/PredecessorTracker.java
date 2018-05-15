package fcn.project.chord.node.threads;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import fcn.project.chord.node.ChordNode;
import fcn.project.chord.node.RequestResponse;

public class PredecessorTracker implements Runnable {

	ChordNode localNode;

	public PredecessorTracker(ChordNode localNode) {
		super();
		this.localNode = localNode;
	}

	@Override
	public void run() {
		Socket connector;
		InetSocketAddress currPred = null;

		while(true){
			try{
				currPred = localNode.predecessor();
//				System.out.println(" Predecessor Tracker : Curr predecessor : " + currPred.getAddress());
				if (currPred != null && !currPred.equals(localNode.address())) {

					connector = new Socket(currPred.getAddress(), currPred.getPort());
					PrintStream output = new PrintStream(connector.getOutputStream());
					output.println(RequestResponse.PING);
					try {
						Thread.sleep(60);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					/* GET response */
					BufferedReader reader = new BufferedReader(new InputStreamReader(connector.getInputStream()));		
					String response = reader.readLine();
					if (response == null || !response.equals(RequestResponse.PONG)) {
						localNode.clearPredecessor();	
					}
					connector.close();

				}
				Thread.sleep(5000);
			}catch(Exception e){
				System.out.println("error in Predecessor Tracker " + e + currPred);
			}
		}

	}

}
