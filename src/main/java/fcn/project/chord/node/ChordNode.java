package fcn.project.chord.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fcn.project.chord.node.comm.ConnectionListener;
import fcn.project.chord.node.threads.FingerFixer;
import fcn.project.chord.node.threads.PredecessorTracker;
import fcn.project.chord.node.threads.Stabilizer;

public class ChordNode {

	private int portNumber;
	private InetAddress myIpAddr;
	private ConnectionListener comm = null;
	private InetSocketAddress predecessor = null;
	private ExecutorService execServ = null;
	private long chordNodeId;
	private FingerTable fingerTable = null;
	private Stabilizer stabilizer = null;
	private PredecessorTracker predTracker = null;
	private FingerFixer  fingerFix = null;
	private Map<String,String> dataMap = new ConcurrentSkipListMap<>();
	public ChordNode(int portNumber, String ip) {		
		try {
			this.portNumber = portNumber;
			this.myIpAddr = InetAddress.getByName(ip);
			chordNodeId = Util.getHashForNode(myIpAddr.getHostAddress(), portNumber);
			this.fingerTable = new FingerTable(this);
			this.execServ = Executors.newFixedThreadPool(5);			
			System.out.println("LOCAL ID ************* : " + chordNodeId);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} 
		System.out.println("NODE created" + portNumber + " " +  myIpAddr);
	}



	public int getPort() {
		return this.portNumber;
	}


	public void join(String ip, String port) throws IOException{


		if(ip != null && port != null){
			String response;

			String request = RequestResponse.FIND_MY_SUCCESSOR + "_" + chordNodeId;
			response = queryRequest(request, ip, port);
			System.out.println(" Response received : " + response);
			if (response != null){
				//				System.out.println(" Response received : " + response);
				String[] resp = response.split(":");
				InetSocketAddress succAddr = new InetSocketAddress(resp[0], Integer.valueOf(resp[1]));
				fingerTable.update("UPDATE_FINGER",1, succAddr);
				System.out.println("Join Notify");
				notify(succAddr);
			}
		}else{
			fingerTable.update("UPDATE_FINGER",1, new InetSocketAddress(myIpAddr,RequestResponse.PORTNUM));
		}
		
		startAllThreads();
	}


	private void startAllThreads() throws IOException {
		this.comm = new ConnectionListener(this);		
		this.stabilizer = new Stabilizer(this,this.fingerTable);
		this.fingerFix = new FingerFixer(this, this.fingerTable);
		this.predTracker = new PredecessorTracker(this);	
		execServ.execute(this.comm);
		execServ.execute(this.stabilizer);
		execServ.execute(this.fingerFix);
		execServ.execute(this.predTracker);
		System.out.println("ALL threads started");
	}

	public void stopAllThreads(){
		execServ.shutdownNow();
	}
	public void notify(InetSocketAddress succAddr) {
		try {
			Thread.sleep(60);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		queryRequest(RequestResponse.NOTIFY+"_"+myIpAddr.getHostAddress()+":"+String.valueOf(this.portNumber), succAddr.getAddress().getHostAddress(), String.valueOf(succAddr.getPort()));
	}

	private String queryRequest(String req, String toIP, String toPORT ){
		Socket connector = null;
		String response = null;
		try {
			/* SEND request to find successor */
			//			System.out.println(" Sending req to : " +toIP);
			InetAddress ipAddr = InetAddress.getByName(toIP);
			int portNum = Integer.parseInt(toPORT);
			connector = new Socket(ipAddr, portNum);
			PrintStream output = new PrintStream(connector.getOutputStream());
			output.println(req);
			try {
				Thread.sleep(60);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			/* GET response */
			BufferedReader reader = new BufferedReader(new InputStreamReader(connector.getInputStream()));		
			response = reader.readLine();

		} catch (IOException e) {
			System.out.println("Error in sendRequest");
		}
		finally{
			if (connector != null){
				try {
					connector.close();
				} catch (IOException e) {
					System.out.println("Error in closing connector");
				}
			}
		}
		return response;

	}

	public InetSocketAddress findSuccessor (long id, Set<InetSocketAddress> hops) {

		// initialize return value as this node's successor (might be null)
		InetSocketAddress successor = successor();
		InetSocketAddress localAddr = new InetSocketAddress(myIpAddr, portNumber);
		// find predecessor
		InetSocketAddress pre = null;
		InetSocketAddress n = localAddr;
		long succRelativeId = 0;
		InetSocketAddress lastAlive = n;
		long succID = 0;
		if(successor != null){
			succID = Util.getHashForNode(successor.getAddress().getHostAddress(), successor.getPort());
			succRelativeId = Util.getRelativeId(succID, chordNodeId);
		}
		long lookupRelativeId = Util.getRelativeId(id, chordNodeId);
		String[] resp;
		long nid;
		InetSocketAddress n_succ = null;

		while(lookupRelativeId > succRelativeId){

			pre = n;
			if (n.equals(localAddr)) {
				n = this.closestPrecedingNode(lookupRelativeId);
			} else {
				String response = this.queryRequest(RequestResponse.CLOSEST + String.valueOf(id), n.getAddress().getHostAddress(), String.valueOf(n.getPort()));
				if(hops != null){
					hops.add(n);
				}
				// if fail to get response, set n to most recently 
				if (response == null) {
					n = lastAlive;
					response = this.queryRequest(RequestResponse.YOUR_SUCCESSOR, n.getAddress().getHostAddress(), String.valueOf(n.getPort()));												
					if (response == null) {
						System.out.println("It's not possible.");
						return localAddr;
					}
					continue;
				}else {
					resp = response.split(":");
					InetSocketAddress result = new InetSocketAddress(resp[0], Integer.valueOf(resp[1]));

					// if n's closest is itself, return n
					if (result.equals(n))
						break;

					// else n's closest is other node "result"
					else {	
						// set n as most recently alive
						lastAlive = n;		
						// ask "result" for its successor
						response = this.queryRequest(RequestResponse.YOUR_SUCCESSOR, result.getAddress().getHostAddress(), String.valueOf(result.getPort()));							
						if(hops != null){
							hops.add(result);
						}
						// if we can get its response, then "result" must be our next n
						if (response!=null) {
							resp = response.split(":");
							n_succ = new InetSocketAddress(resp[0], Integer.valueOf(resp[1]));
							n = result;
						}
						// else n sticks, ask n's successor
						else {								
							response = this.queryRequest(RequestResponse.YOUR_SUCCESSOR, n.getAddress().getHostAddress(), String.valueOf(n.getPort()));
							resp = response.split(":");
							n_succ = new InetSocketAddress(resp[0], Integer.valueOf(resp[1]));
						}
					}
				}
				// compute relative id for while loop judgment					
				succID = Util.getHashForNode(n_succ.getAddress().getHostAddress(), n_succ.getPort());
				nid = Util.getHashForNode(n.getAddress().getHostAddress(), n.getPort());
				succRelativeId = Util.getRelativeId(succID, nid);
				lookupRelativeId = Util.getRelativeId(id, nid);
			}
			if (pre.equals(n))
				break;	
		}
		pre = n;

		// if other node found, ask it for its successor
		if (!pre.equals(localAddr)) {
			String req = RequestResponse.YOUR_SUCCESSOR;
			String response = this.queryRequest(req, pre.getAddress().getHostAddress(), String.valueOf(pre.getPort()));
			resp = response.split(":");
			successor = new InetSocketAddress(resp[0], Integer.valueOf(resp[1]));
		} 
		// if successor is still null, set it as local node, return
		if (successor == null)
			return localAddr;

		return successor;
	}

	public InetSocketAddress successor(){
		return fingerTable.getEntry(1);

	}


	public InetSocketAddress predecessor(){
		return this.predecessor;
	}

	public InetSocketAddress closestPrecedingNode(long lookupRelativeId){
		
		Set<Integer> keySet = this.fingerTable.getKeys();
		Object[] keyArray =  keySet.toArray();
		Arrays.sort(keyArray);
		
		for (int i = keySet.size() - 1; i > -1; i--) {
			InetSocketAddress addr = fingerTable.getEntry((Integer)keyArray[i]);
			if (addr == null) {
				continue;
			}
			long id = Util.getHashForNode(addr.getAddress().getHostAddress(), addr.getPort());
			long relativeId = Util.getRelativeId(id, chordNodeId);			
			if (lookupRelativeId > 0 && lookupRelativeId > relativeId && !addr.equals(address()))  {
				String response  = this.queryRequest(RequestResponse.PING, addr.getAddress().getHostAddress(), String.valueOf(addr.getPort()) );
				if (response!=null &&  response.equals(RequestResponse.PONG)) {
					return addr;
				}
				else {
					fingerTable.update("REMOVE",-1,addr);
				}
			}
		}
		return new InetSocketAddress(myIpAddr, portNumber);		
	}

	public InetSocketAddress address(){
		return new InetSocketAddress(myIpAddr, portNumber);
	}

	public void handleNotification(InetSocketAddress pred) {
		if (predecessor == null || predecessor.equals(address())) {
			this.predecessor = pred;
		}
		else {
			long currPredId = Util.getHashForNode(predecessor.getAddress().getHostAddress(), predecessor.getPort());
			long localNodeRelId = Util.getRelativeId(chordNodeId, currPredId);
			long newPredId = Util.getHashForNode(pred.getAddress().getHostAddress(), pred.getPort());
			long newPredRelativeID = Util.getRelativeId(newPredId, currPredId);
			if (newPredRelativeID > 0 && newPredRelativeID < localNodeRelId)
				this.predecessor = pred;
		}		
	}

	public void deleteSucessor(){

		fingerTable.update("DELETE_SUCCESSOR", -1 , null);
		if (predecessor!= null && predecessor.equals(fingerTable.getEntry(1)))
			predecessor = null;

		fingerTable.update("FILL", -1, null);
		InetSocketAddress successor = successor();

		/* if successor is null or unchanged and predecessor is some other node,
		 *  ask in the opposite direction for finding local node's successor */
		if ((successor == null || (successor.getAddress().equals(myIpAddr) && successor.getPort() == portNumber)) && predecessor!=null && !(predecessor.getAddress().equals(myIpAddr) && predecessor.getPort() == portNumber)) {
			InetSocketAddress p = predecessor;
			InetSocketAddress p_pred = null;
			while (true) {
				String response = queryRequest(RequestResponse.PREDECESSOR, p.getAddress().getHostAddress(), String.valueOf(p.getPort()) );
				if(response == null)
					break;
				String[] ipPort = response.split(":"); 
				p_pred = new InetSocketAddress(ipPort[0], Integer.valueOf(ipPort[1]));
				// if p's predecessor is node is just deleted, 
				// or itself (nothing found in p), or local address,
				// p is current node's new successor, break
				if (p_pred.equals(p) || p_pred.equals(address())|| p_pred.equals(successor)) {
					break;
				}
				// else, keep asking
				else {
					p = p_pred;
				}
			}
			// update successor
			fingerTable.update("UPDATE_FINGER", 1, p);
		}

	}

	public void clearPredecessor() {
		this.predecessor = null;
	}

	public long getID() {
		return chordNodeId;
	}

	public void printFingerTable() {
		System.out.println("\n FINGER TABLE FOR NODE: "+ chordNodeId + "\n");
		for (int i : this.fingerTable.getKeys()){
			System.out.println("KEY: " + i + "VALUE : " + this.fingerTable.getEntry(i));
		}

	}

	public Map<Integer, InetSocketAddress> getFingerTable(){
		return this.fingerTable.getFingerTable();
	}
	public boolean isLocal(InetSocketAddress addr){
		return (addr.getAddress().equals(myIpAddr) && addr.getPort() == portNumber);
	}

	public void saveData(String key, String value){
		dataMap.put(key, value);	
		System.out.println("\n saved data : "+dataMap.get(key));
	}



	public String getData(String key) {

		System.out.println("\n key: " + key  + "Getting data : " + dataMap.get(key));
		return dataMap.get(key);
	}



	public InetAddress getIP() {

		return myIpAddr;
	}



	public void printFingerTableOfNode(String toIP) {
		Socket connector = null;
		String request = RequestResponse.SHARE_FT;
		try {
			/* SEND request to get finger table */
			InetAddress ipAddr = InetAddress.getByName(toIP.trim());
			int portNum = RequestResponse.PORTNUM;
			connector = new Socket(ipAddr, portNum);
			PrintStream output = new PrintStream(connector.getOutputStream());
			output.println(request);
			try {
				Thread.sleep(60);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			/* GET response */
			ObjectInputStream ois = new ObjectInputStream(connector.getInputStream());
			@SuppressWarnings("unchecked")
			Map<Integer, InetSocketAddress> nodeFingerTable = (Map<Integer, InetSocketAddress>) ois.readObject();
			System.out.println("\n FINGER TABLE FOR NODE: "+ toIP + "\n");
			for (int i: nodeFingerTable.keySet()){
				System.out.println("KEY: " + i + " VALUE : " + nodeFingerTable.get(i));
			}
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Error in sendRequest");
			e.printStackTrace();
		}
		finally{
			if (connector != null){
				try {
					connector.close();
				} catch (IOException e) {
					System.out.println("Error in closing connector");
				}
			}
		}
	}
}













