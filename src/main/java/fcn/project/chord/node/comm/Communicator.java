package fcn.project.chord.node.comm;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import fcn.project.chord.node.ChordNode;
import fcn.project.chord.node.RequestResponse;
import fcn.project.chord.node.Util;

public class Communicator implements Runnable{

	private ChordNode localNode;
	private InputStream iStream = null;
	private OutputStream oStream = null;
	private Socket sock = null;
	private FileWriter logfileWriter = null;
	public Communicator(Socket childSocket, ChordNode node, FileWriter logfilewriter2) throws IOException {
		this.sock = childSocket;
		this.localNode = node;
		this.iStream = childSocket.getInputStream();
		this.oStream = childSocket.getOutputStream();
		this.logfileWriter = logfilewriter2;
	}

	public void run() {
		//		System.out.println("Communicator running...");
		BufferedReader reader = new BufferedReader(new InputStreamReader(this.iStream));
		String request = null;
		try {
			//			System.out.println("Reading request...");


			request = reader.readLine();
			//			System.out.println(" REQUEST RECEIVED : " + request );
			if (request.contains(RequestResponse.FIND_MY_SUCCESSOR)){
				String nodeId = request.substring(request.lastIndexOf('_')+1);
				InetSocketAddress successor = localNode.findSuccessor(Long.valueOf(nodeId), null);
				String successorIPPort = successor.getAddress().getHostAddress()+ ":" + String.valueOf(successor.getPort());
				//				System.out.println("Sucesssor found : " + successorIPPort);

				/* write to response */
				PrintStream output = new PrintStream(oStream);
				output.println(successorIPPort);
			}
			else if (request.contains(RequestResponse.YOUR_SUCCESSOR)){				
				InetSocketAddress successor = localNode.successor();
				String successorIPPort = successor.getAddress().getHostAddress()+ ":" + String.valueOf(successor.getPort());
				//				System.out.println("Sucesssor found : " + successorIPPort);

				/* write to response */
				PrintStream output = new PrintStream(oStream);
				output.println(successorIPPort);
			}
			else if(request.contains(RequestResponse.PING)){
				/* write to response */
				PrintStream output = new PrintStream(oStream);
				output.println(RequestResponse.PONG);				
			}
			else if(request.contains(RequestResponse.NOTIFY)){
				String[] ipPort = request.split("_");
				ipPort = ipPort[1].split(":");
				InetSocketAddress pred = new InetSocketAddress(ipPort[0], Integer.valueOf(ipPort[1]));
				localNode.handleNotification(pred);
			}
			else if(request.contains(RequestResponse.PREDECESSOR)){
				PrintStream output = new PrintStream(oStream);
				InetSocketAddress pred = localNode.predecessor();
				output.println(pred.getAddress().getHostAddress() + ":" + String.valueOf(pred.getPort()));
			}
			else if(request.contains(RequestResponse.CLOSEST)){
				String[] req = request.split("_");
				long id = Long.parseLong(req[1]);
				long relativeId = Util.getRelativeId(id, localNode.getID());
				InetSocketAddress closestPrecedingNode = localNode.closestPrecedingNode(relativeId);
				PrintStream output = new PrintStream(oStream);
				output.println(closestPrecedingNode.getAddress().getHostAddress() + ":" + String.valueOf(closestPrecedingNode.getPort()));

			}else if(request.contains(RequestResponse.SAVE)){
				String[] req = request.split(":");
				String result = null;
				String key = req[1].split("=")[0];
				String value = req[1].split("=")[1];
				long hashOfKey = Util.hashKey(key);		
				Set<InetSocketAddress> hops = new HashSet<>();
				InetSocketAddress successor = localNode.findSuccessor(hashOfKey, hops);
				if (localNode.isLocal(successor)){
					System.out.println("\n Saving data on local node");
					localNode.saveData(key, value);
					result = "OK";
				}else{
					System.out.println("\n forwarding req to successor : " + successor.getAddress());
					result = forwardRequestToSuccessor(key, value,successor);
					System.out.println("Result : " + result);
				}
				PrintStream output = new PrintStream(oStream);
				output.println(result);
				System.out.println("   HOPS : " + hops.size() );
				InetSocketAddress[] hopsArr = new InetSocketAddress[hops.size()];
				hopsArr = hops.toArray(hopsArr);
				for(int i = 0; i < hops.size(); i++){
					System.out.println(i + "." + " " + hopsArr[i]);
				}
				logfileWriter.append(hops.size() + " " + key + " " + successor.getAddress().getHostAddress() + "\n");
				logfileWriter.close();			
			}else if (request.contains(RequestResponse.RETRIEVE)){
				String[] req = request.split(":");
				String key = req[1];
				String result = null;
				long hashOfKey = Util.hashKey(key);		
				System.out.println("hash value of the key is " + hashOfKey);
				Set<InetSocketAddress> hops = new HashSet<>();
				InetSocketAddress successor = localNode.findSuccessor(hashOfKey, hops);
				if (localNode.isLocal(successor)){
					System.out.println("\n Getting data from local node");
					result = localNode.getData(key);
					System.out.println("");
					if(result == null)
						result = RequestResponse.ERROR;
				}else{
					System.out.println("successor to send data is " + successor.getAddress());
					result = forwardRequestToSuccessor(key, null, successor);
				}
				PrintStream output = new PrintStream(oStream);
				output.println(result);
				InetSocketAddress[] hopsArr = new InetSocketAddress[hops.size()];
				hopsArr = hops.toArray(hopsArr);
				for(int i = 0; i < hops.size(); i++){
					System.out.println(i + "." + " " + hopsArr[i]);
				}
				logfileWriter.append(hops.size() + " " + key + " " + successor.getAddress().getHostAddress() + "\n");
				logfileWriter.close();	
			}else if(request.contains(RequestResponse.PUT)){
				String[] req = request.split(":");
				String result = null;
				String key = req[1].split("=")[0];
				String value = req[1].split("=")[1];
				System.out.println("\n Saving data on local node");
				localNode.saveData(key, value);
				result = "OK";
				PrintStream output = new PrintStream(oStream);
				output.println(result);
			}else if(request.contains(RequestResponse.GET)){
				String[] req = request.split(":");
				String key = req[1];
				String result = null;
				System.out.println("\n Getting data from local node");
				result = localNode.getData(key);
				System.out.println("");
				if(result == null)
					result = RequestResponse.ERROR;
				PrintStream output = new PrintStream(oStream);
				output.println(result);
			}else if(request.contains(RequestResponse.SHARE_FT)){
				ObjectOutputStream oos = new ObjectOutputStream(oStream);
				oos.writeObject(localNode.getFingerTable());
			}
		} catch (IOException e) {
			System.out.println("Cannot read line from input stream.");
		}finally{
			if(sock != null){
				try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}

	private String forwardRequestToSuccessor(String key, String value, InetSocketAddress successor) {
		String result = null;
		String request = null;
		Socket connector = null;
		try{		
			if(value == null){
				request = RequestResponse.GET + ":" + key;  
			}else{
				request = RequestResponse.PUT + ":" + key + "=" + value;  
			}
			int portNum = 9999;
			connector = new Socket(successor.getAddress(), portNum);
			PrintStream output = new PrintStream(connector.getOutputStream());

			output.println(request);
			System.out.println("\n Getting response...");
			Thread.sleep(1000);
			BufferedReader reader = new BufferedReader(new InputStreamReader(connector.getInputStream()));		
			result = reader.readLine();
		}catch(Exception e){
			System.out.println("\n Error in getting data from successor : "+ e);
		}finally{
			try {
				connector.close();
			} catch (IOException e) {
				System.out.println("\n Error in closing socket : " + e);
			}
		}
		return result;
	}
}
