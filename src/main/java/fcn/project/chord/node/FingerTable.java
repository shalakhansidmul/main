package fcn.project.chord.node;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

public class FingerTable implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Map<Integer, InetSocketAddress> fingerTable = null;
	ChordNode localNode;
	public FingerTable(ChordNode node) {
		super();
		this.localNode = node;
		this.fingerTable = new ConcurrentSkipListMap<Integer, InetSocketAddress>();
	}
	
	public void updateEntry(Integer key, InetSocketAddress value){
		
		fingerTable.put(key, value);
		printFingerTable();
	}

	public Map<Integer, InetSocketAddress> getFingerTable(){
		return this.fingerTable;
	}
	private void printFingerTable() {
		for(Map.Entry<Integer, InetSocketAddress> ent : fingerTable.entrySet()){
			System.out.println("KEY: " + ent.getKey() + " VALUE: " + ent.getValue().getHostString());
		}
	}

	public InetSocketAddress getEntry(int i) {
		return fingerTable.get(i);
	}
	
	public void removeNode(InetSocketAddress nodeAddr){
		for(Map.Entry<Integer, InetSocketAddress> ent : fingerTable.entrySet()){
			if (ent.getValue()!=null && ent.getValue().equals(nodeAddr)){
				ent.setValue(null);
			}
		}
	}
	
	public void update(String action, int key, InetSocketAddress addr) {
		synchronized (fingerTable) {

			if (action.equalsIgnoreCase("REMOVE")){
				this.removeNode(addr);
			}
			else if (action.equalsIgnoreCase("UPDATE_FINGER")){
				fingerTable.put(key, addr);				
			}
			else if (action.equalsIgnoreCase("DELETE_ENTRY")){
				for (int i = 32; i > 0; i--) {
					InetSocketAddress ithfinger = fingerTable.get(i);
					if (ithfinger != null && ithfinger.equals(addr))
						fingerTable.put(i, null);
				}
			}
			else if(action.equalsIgnoreCase("FILL")){
				InetSocketAddress successor = fingerTable.get(1);
				if (successor == null || successor.equals(localNode.address())) {
					for (int i = 2; i <= 32; i++) {
						InetSocketAddress ithfinger = fingerTable.get(i);
						if (ithfinger!=null && !ithfinger.equals(localNode)) {
							for (int j = i-1; j >1; j--) {
								fingerTable.put(j, ithfinger);								
							}
							fingerTable.put(1, ithfinger);
							if (!ithfinger.equals(localNode.address()))
								localNode.notify(ithfinger);
							break;
						}
					}
				}
				successor = fingerTable.get(1);
				if ((successor == null || successor.equals(localNode.address())) && localNode.predecessor()!=null && !localNode.predecessor().equals(localNode.address())) {
					fingerTable.put(1, localNode.predecessor());
					localNode.notify(localNode.predecessor());
				}
			}
			else if (action.equalsIgnoreCase("DELETE_SUCCESSOR")){

				InetSocketAddress successor = fingerTable.get(1);
				if (successor == null)
					return;
				
				for (int i = 32; i > 0; i--) {
					InetSocketAddress ithfinger = fingerTable.get(i);
					if(ithfinger.equals(successor)){
						fingerTable.put(i, null);
					}
				}
			}else if (action.equalsIgnoreCase("REMOVE_ENTRY")){
				this.fingerTable.remove(key);
			}
		}
	}

	public boolean isUnique(int key, InetSocketAddress succ) {
		for (Entry<Integer, InetSocketAddress> e: fingerTable.entrySet()){
			if(e.getKey() < key){
				if(e.getValue().getAddress().equals(succ.getAddress())){
					return false;
				}
			}else{
				return true;
			}
		}
		return true;
	}
	
	public Set<Integer> getKeys(){
		return this.fingerTable.keySet();
	}
}
