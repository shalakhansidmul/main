package fcn.project.chord;

import java.io.IOException;
import java.util.Scanner;

import fcn.project.chord.node.ChordNode;
import fcn.project.chord.node.NodeFactory;


public class Main 
{
	public ChordNode node;
	private NodeFactory nodeFact = NodeFactory.getInstance();


	public Main(String portNum, String ip, String portnumofConntact, String ipOfContact) throws IOException {
		super();
		this.node = nodeFact.getNode(portNum, ip);		
		this.node.join(ipOfContact, portnumofConntact);
	}

	public static void main( String[] args )
	{
		Main m = null;
		String portnum = "9999" ;
		String input = "";
		Scanner sc = new Scanner(System.in);
		int opt;
		try{
			if(args.length == 1){
				System.out.println("Creating node : " + args[0]);
				m = new Main(portnum, args[0], null, null);
			}else if (args.length == 2){
				System.out.println("Creating Node : " + args[0] +" Joinging on node : " + args[1]);
				m = new Main(portnum, args[0],  portnum, args[1]);
			}
			while(true){
				System.out.println("1.Print finger table for node\n2.Quit");
				opt = sc.nextInt();
				switch(opt){
				case 1:
					System.out.println("Enter node ip: ");
					input = sc.next();
					if(input.trim().equalsIgnoreCase(args[0])){
						m.node.printFingerTable();
					}else{
						m.node.printFingerTableOfNode(input);
					}
					break;
				case 2:
					sc.close();
					m.node.stopAllThreads();
					System.exit(0);
				default: 
					System.out.println("Invalid input, please try again.");
					break;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
