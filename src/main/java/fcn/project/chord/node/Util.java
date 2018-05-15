package fcn.project.chord.node;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class Util {
	private static MessageDigest sha1Digest;
	
	public static long getHashForNode(String ip, int port){
		long hashedValue = 0;
		InetSocketAddress nodeAddr = new InetSocketAddress(ip, port);
		hashedValue = sha1(nodeAddr.hashCode());
		return (long) (hashedValue % Math.pow(2, 32));
	}
	
	private static long sha1(int hashCode) {
		long hashedValue = 0;
		byte[] hashedBytes;
		byte[] bytesToHash = getBytesToHash(hashCode);
		try {
	    	sha1Digest = MessageDigest.getInstance("SHA-1");
	    	sha1Digest.reset();
	    	sha1Digest.update(bytesToHash);
	    	hashedBytes = sha1Digest.digest();
	    	hashedValue = getLongFromHash(hashedBytes);
	    } catch (NoSuchAlgorithmException e) {
	    	System.out.println("Error in SHA hashing for : " + hashCode);
	    }
	    return hashedValue;
	}

	private static long getLongFromHash(byte[] hashedBytes) {
		int m = 0, n = 1;
		byte[] returnVal = new byte[4];
		for (m = 0; m < 4; m++) {
			byte temp = hashedBytes[m];
			n = 1;
			while(n<5){
				temp = (byte) (temp ^ hashedBytes[m+n]);
				n++;
			}
			returnVal[m] = temp;
		}
		long ans = (returnVal[0] & 0xFF) << 24;
		ans |= (returnVal[1] & 0xFF) << 16;
		ans |= (returnVal[2] & 0xFF) << 8;
		ans |= (returnVal[3] & 0xFF);
		ans &=(long)0xFFFFFFFFl;
		return ans;
	}

	private static byte[] getBytesToHash(int hashCode) {

		byte[] bytesToHash  = new byte[4];
		bytesToHash[0] = (byte) (hashCode >> 24);
		bytesToHash[1] = (byte) (hashCode >> 16);
		bytesToHash[2] = (byte) (hashCode >> 8);
		bytesToHash[3] = (byte) (hashCode);		
		return bytesToHash;
	}
	
	public static long getRelativeId(long a, long b){
		long relativeID = a - b;
		if (relativeID < 0) {
			relativeID += Math.pow(2, 32);
		}
		return relativeID;
	}
	
	public static long hashKey(String key){		
			int i = key.hashCode();
			long hashVal = (long) (sha1(i) % Math.pow(2, 32));
			System.out.println("Key : " +key + " hash val : " + hashVal);
			return hashVal;
	}
}
