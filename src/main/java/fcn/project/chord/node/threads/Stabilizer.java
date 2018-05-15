package fcn.project.chord.node.threads;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import fcn.project.chord.node.ChordNode;
import fcn.project.chord.node.FingerTable;
import fcn.project.chord.node.RequestResponse;
import fcn.project.chord.node.Util;

public class Stabilizer implements Runnable {

	private ChordNode localNode;
	private FingerTable fingerTable;
	public Stabilizer(ChordNode node, FingerTable fingerTable) {
		this.localNode = node;
		this.fingerTable = fingerTable;
	}

	@Override
	public void run() {
		String response;
		String[] ipPort;
		InetSocketAddress pred;
		long localId, succId, predId;
		long succRelId;
		long predRelId;
		InetSocketAddress localAddr = localNode.address();
		localId = localNode.getID();
		Socket connector;


		while(true) {
			try {
				InetSocketAddress succ = localNode.successor();
				if (succ == null || succ.equals(localAddr)) {
					this.fingerTable.update("FILL", 1, null);
					succ = localNode.successor();
				}
				if(succ != null && !succ.equals(localAddr)) { 
					connector = new Socket(succ.getAddress(), succ.getPort());
					PrintStream output = new PrintStream(connector.getOutputStream());
					output.println(RequestResponse.PREDECESSOR);
					try {
						Thread.sleep(60);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					/* GET response */
					BufferedReader reader = new BufferedReader(new InputStreamReader(connector.getInputStream()));		
					response = reader.readLine();
					if (response == null)
						this.fingerTable.update("DELETE_SUCCESSOR", 1, null);
					else {
						ipPort = response.split(":"); 
						pred = new InetSocketAddress(ipPort[0], Integer.valueOf(ipPort[1]));
						if (pred.equals(localAddr)) { 
							//							System.out.println("Stabilizer Notify");
							localNode.notify(succ);
						}
						else { 						
							succId = Util.getHashForNode(succ.getAddress().getHostAddress(), succ.getPort());
							predId = Util.getHashForNode(pred.getAddress().getHostAddress(), pred.getPort());
							succRelId = Util.getRelativeId(succId, localId);
							predRelId = Util.getRelativeId(predId, localId);
							if (predRelId>0 && predRelId < succRelId) {
								this.fingerTable.update("UPDATE_FINGER", 1, pred);
								
								localNode.notify(pred);
							}
						}
					}
					connector.close();
				}
				Thread.sleep(2000);
			}catch(Exception e){
				System.out.println("\n error in stabilizer ");
				e.printStackTrace();
			}
		}

	}
}
