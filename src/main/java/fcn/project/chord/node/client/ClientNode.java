package fcn.project.chord.node.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

import fcn.project.chord.node.RequestResponse;

public class ClientNode {
	private  static BufferedWriter logfileWriter;
	public static void main(String[] args) {
		String input = "";
		String key, response, contact, val;
		boolean exit = false;
		BufferedReader bufferedReader = null;
		
				
		if(args.length == 2){
			try {
				File inputfile = new File(args[0]);
				FileReader fileReader = new FileReader(inputfile);
				bufferedReader = new BufferedReader(fileReader);
				logfileWriter = new BufferedWriter(new FileWriter("ClientNode.log", true));
				String line;
				contact = args[1].trim();
				while ((line = bufferedReader.readLine()) != null) {
					key = line.split("=")[0].trim();
					val = line.split("=")[1].trim();
					executeRequest(key, val, contact);
				}
			} catch (IOException e) {
				System.out.println("Input file not found. Try again.");
				return;
			}finally{
				if(bufferedReader != null)
					try {
						bufferedReader.close();
						logfileWriter.close();
						logfileWriter = null;
					} catch (IOException e) {
						System.out.println("Error closing buffered reader");
						return;
					}
			}
		}
		
		
		Scanner sc = new Scanner(System.in);
		System.out.println("\n ************ Welcome to DFS ************ ");
		while(exit == false){
			System.out.println("\n 1. Save data\n 2. Retrieve data\n\n Enter your choice: ");
			input = sc.next();
			switch(input.trim()){
			case "1":
				System.out.println("\n Enter key : ");
				key = sc.next();
				System.out.println("\n Enter value : ");
				String value = sc.next();
				System.out.println("\n Enter ip of contact node : ");
				contact = sc.next();
				response = executeRequest(key, value, contact);
				if(response.equals("OK")){
					System.out.println("\n Data saved successfully.");				
				}
				break;
			case "2":
				System.out.println("\n Enter key : ");
				key = sc.next();
				System.out.println("\n Enter ip of contact node : ");
				contact = sc.next();
				response = executeRequest(key, null, contact);
				if(response.equals(RequestResponse.ERROR)){
					System.out.println("\n Error in retrieving data. Please try again ");				
				}else{
					System.out.println("\n Retrieved value: " + response);
				}
				break;
			case "3":
				System.out.println("*********** Exiting DFS *************");
				exit = true;
				break;			
			}
		}
	}

	private static String executeRequest(String key, String value, String contact) {

		Socket connector = null;
		String response = null;
		String request = null;
		
		try {			
			//System.out.println("\n Sending request...");
			if(value != null){
				request = RequestResponse.SAVE + ":" + key + "=" + value;
			}else{
				request = RequestResponse.RETRIEVE + ":" + key;	
			}
			InetAddress ipAddr = InetAddress.getByName(contact);
			int portNum = 9999;
			connector = new Socket(ipAddr, portNum);
			PrintStream output = new PrintStream(connector.getOutputStream());
			long startTime = System.currentTimeMillis();
			output.println(request);
			//System.out.println("\n Getting response...");
			Thread.sleep(60);
			BufferedReader reader = new BufferedReader(new InputStreamReader(connector.getInputStream()));		
			response = reader.readLine();
			long endTime = System.currentTimeMillis();
			System.out.println("Request execution time: " + (endTime - startTime));
			if(logfileWriter != null)
				logfileWriter.write(key + ","+ String.valueOf(endTime - startTime) + "," + response);
			
		} catch (Exception e) {
			System.out.println("Error in sending Request : "+ e);

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

}
