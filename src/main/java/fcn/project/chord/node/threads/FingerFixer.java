package fcn.project.chord.node.threads;

import java.net.InetSocketAddress;

import fcn.project.chord.node.ChordNode;
import fcn.project.chord.node.FingerTable;

public class FingerFixer implements Runnable {

	private ChordNode localNode;
	private FingerTable fingerTable;
	public FingerFixer(ChordNode node, FingerTable fingerTable) {
		this.localNode = node;
		this.fingerTable = fingerTable;
	}

	@Override
	public void run() {
		InetSocketAddress succ;
		int i;
		long localId = localNode.getID();
		long id;
		System.out.println("Finger fixer started");
		while(true) {
			try {
				for(i = 2 ; i< 33; i++){					
					id =  (long) ((localId + Math.pow(2, i - 1)) % Math.pow(2, 32));
					succ = localNode.findSuccessor(id,null);
					InetSocketAddress currEntry = this.fingerTable.getEntry(i);
					if(currEntry == null){
						/* key is not present */
						if (this.fingerTable.isUnique(i,succ)){
							this.fingerTable.update("UPDATE_FINGER", i, succ);
						}
					}else{
						if (this.fingerTable.isUnique(i,succ)){
							this.fingerTable.update("UPDATE_FINGER", i, succ);
						}else{
							this.fingerTable.update("REMOVE_ENTRY", i, succ);
						}
					}
				}
				Thread.sleep(2000);
				
			}catch(Exception e){
				System.out.println("\n error in finger fixer ");
				e.printStackTrace();
			}
		}

	}

}
