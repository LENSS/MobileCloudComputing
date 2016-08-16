package edu.nps.secureshare.mdfs;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.crypto.SecretKey;

import android.util.Log;
import edu.nps.secureshare.crypto.MDFSCipher;
import edu.nps.secureshare.reedsolomon.ReedSolomon;
import edu.nps.secureshare.shamir.ShamirSecret;
import edu.nps.secureshare.shamir.ShamirSecret.ShareInfo;
import edu.nps.secureshare.shamir.ShamirSecret.SplitSecretOutput;
import edu.nps.secureshare.shamir.math.BigIntUtilities;
import edu.nps.secureshare.util.Base64;

public class MDFS {
	private static final String DEBUG_TAG = "MDFS";
	
	public Vector<FragmentContainer> getFragments(String filename, Long timestamp, byte[] file, int k, int n) {
		// AES Encryption
		MDFSCipher myCipher = MDFSCipher.getInstance();
		ShamirSecret secret = new ShamirSecret(new ShamirSecret.PublicInfo(n, k, ShamirSecret.getPrimeUsedFor384bitSecretPayload(), null));
		
		// Holds the FragmentContainers holding all the metadata
		Vector<FragmentContainer> results = new Vector<FragmentContainer>();
		FragmentContainer fc = null; // temp storage before adding to results
		int m = n-k; // m is the number of coding fragments
		byte[] plainByteFile = file;
		byte[] encryptedByteFile = null;
		byte[][] fragments = null;
		
		// DEBUG CODE
		// System.out.println("Input length: " + plainByteFile.length);
		
		// Generate the secret key
		SecretKey skey = myCipher.generateSecretKey();
		byte[] rawSecretKey = skey.getEncoded();
		
		// Convert the key into a reversible String
		String stringKey = Base64.encodeBytes(rawSecretKey);
		
		// Generate key fragments
		SplitSecretOutput secrets = secret.split(BigIntUtilities.createFromStringsBytesAsData(stringKey));
		
		// Encrypt the plainByteFile
		encryptedByteFile = myCipher.encrypt(plainByteFile, rawSecretKey);
		
		// must send the length of the encrypted file to the rsencoder for proper decoding
		long filesize = encryptedByteFile.length;
		
		// DEBUG CODE
		//System.err.println("Encrypted length: " + encryptedByteFile.length);
		
		// generate the RS encoded fragments of encryptedByteFile
		fragments = new ReedSolomon().encoder(encryptedByteFile, k, n);
		
		for (int i = 0; i < k; i++) {
			fc = new FragmentContainer(filename,
					FragmentContainer.DATA_TYPE, filesize, fragments[i], i,
					k, n, secrets.getShareInfos().get(i), timestamp);
			results.add(fc);
		}

		for (int i = 0; i < m; i++) {
			fc = new FragmentContainer(filename,
					FragmentContainer.CODING_TYPE, filesize,
					fragments[i + k], i + k, k, n, secrets.getShareInfos().get(i + k), timestamp);
			results.add(fc);
		}	
		// return the resulting fragments
		return results;
	}
	
	public byte[] getFile(Vector<FragmentContainer> fragments) {
		/* call the code to gather Fragments from other clients and create
		 * a vector containing at least k unique fragments
		 * Search for filename + timestamp.  Compare the getHash value to
		 * the current fragments.  If it doesn't exist, then we need that
		 * fragment.  Hash values are based on filename, timestamp, and fragment
		 * number.  Could also just compare to fragment numbers...
		 * 
		 * Another option is to search for the fragment byt name and compare 
		 * timestamps.  If the filename is the same, but the timestamp is 
		 * different, then search gather for the newer timestamp.
		 * 
		 * I will add temp code to fake the gathered fragments.  Eventaully remove 
		 * the second argument to this function
		 * 
		 * Pseudocode will be
		 * 
		 * int numfrags = 0;
		 * int k = null;
		 * int n = null;
		 * int m = null;
		 * Vector<FragmentContainer> fragments = new Vector<FragmentContainer>();
		 * long timestamp = null;
		 * FragmentContainer fc = null;
		 * while (numFrags < k) {
		 * 		fc = searchFor(filename);
		 *      if (timestamp == null) {// first fragment
		 *      	timestamp = fc.getTimestamp();
		 *      	// Initialize all the necessary variables for the decoder
		 *      	k = fc.getK();
		 *      	n = fc.getN();
		 *      	m = n-k;
		 *      	blocksize = fc.getFragment().length;
		 *      	filesize = fc.getFilesize();
		 *      	fragments.add(fc);
		 *      	numfrags++;
		 *      } else {
		 *      	if (timestamp == fc.getTimestamp()) { //
		 *      		// verify that we don't already have fc
		 *      		fragments.add(fc);
		 *      		numFrags++;
		 *      	}
		 *      }
		 * }
		 * 
		 */
		
		//String directory = "/Users/shuchton/test/"; // temp code
		//String filepath = directory+filename;
		
		// AES Encryption
		MDFSCipher myCipher = MDFSCipher.getInstance();
		Log.i(DEBUG_TAG, "Cipher OK");
		
		byte[] plainByteFile = null;
		byte[] encryptedByteFile = null;
		
		// this is temp because I don't have above code working yet
		FragmentContainer firstFrag = fragments.get(0);
		int k = firstFrag.getK();
		int n = firstFrag.getN();
		int m = n - k;
		//ShareInfo rawSecretKey = firstFrag.getKeyFragment();
		int blocksize = firstFrag.getFragment().length;
		long filesize = firstFrag.getFilesize();
		ShamirSecret secret = new ShamirSecret(new ShamirSecret.PublicInfo(n, k, ShamirSecret.getPrimeUsedFor384bitSecretPayload(), null));
		
		// Jerasure parameters
		int numerased = 0;
		int[] erased = new int[n];
		int[] erasures = new int[n];
		byte[][] data = new byte[k][blocksize];
		byte[][] coding = new byte[m][blocksize];
		
		// initialize erased
		for (int i = 0; i < n; i++) {
			erased[i] = 0;
		}
		
		// initialize data and coding
		for (int i = 0; i < k; i++) {
			data[i] = null;
		}
		for (int i = 0; i < m; i++) {
			coding[i] = null;
		}
		
		Log.i(DEBUG_TAG, String.format("k = %d, n = %d, blocksize = %d\n", k,
				n, blocksize));

		//System.out.println("The file will be save as: " + filepath);
		
		// Store the key fragments
		List<ShareInfo> recoveredKeyShares = new ArrayList<ShareInfo>();
		
		// sort fragments into data and coding and process key
		for (int i = 0; i < fragments.size(); i++) {
			int type = fragments.get(i).getType();
			
			/* KEY ***Do whatever I need to to get the key here*** KEY */
			recoveredKeyShares.add(fragments.get(i).getKeyFragment());
			
			if (type == FragmentContainer.DATA_TYPE) {
				data[fragments.get(i).getFragmentNumber()] = fragments.get(i)
						.getFragment();
			} else {
				coding[fragments.get(i).getFragmentNumber() - k] = fragments
						.get(i).getFragment();
			}
		}
		
		// process erased and erasures
		for (int i = 0; i < k; i++) {
			if (data[i] == null) {
				erased[i] = 1;
				erasures[numerased] = i;
				numerased++;
			}
		}
		for (int i = 0; i < m; i++) {
			if (coding[i] == null) {
				erased[k + i] = 1;
				erasures[numerased] = k + i;
				numerased++;
			}
		}
		
		// -1 indicates the terminus of erasures in the coding
		erasures[numerased] = -1;
		
		// call the native RSDecoder
		Log.i(DEBUG_TAG, "About to call the decoder");
        Log.i(DEBUG_TAG, String.format("Calling decode with %d data fragments, %d coding fragments, %d erased, %d erasures, filesize: %d, blocksize: %d, k: %d, n: %d", data.length, coding.length, erased.length, erasures.length, filesize, blocksize, k, n));
        
        
        encryptedByteFile = new ReedSolomon().decoder(data, coding,
				erasures, erased, filesize, blocksize, k, n);

        Log.i(DEBUG_TAG, "Made it throught the JNI call");
		// DEBUG CODE
		//System.out.println("Encrypted length (decode): " + encryptedByteFile.length);	
		
		// Recover the key
		BigInteger recoveredSecret = secret.combine(recoveredKeyShares).getSecret();
		String recoveredString = BigIntUtilities.createStringFromBigInteger(recoveredSecret);
		byte[] rawSecretKey = null;
		try {
			rawSecretKey = Base64.decode(recoveredString);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// use the key to decrypt the data
		plainByteFile = myCipher.decrypt(encryptedByteFile, rawSecretKey);	
		Log.d(DEBUG_TAG, "The plainbyte size is: " + plainByteFile.length);
		
		// DEBUG CODE
		//System.out.println("Encrypted length (decode): " + plainByteFile.length);	
		
		return plainByteFile;
	}
	
//	public File getFile(String filepath,
//			/*eventially remove*/ Vector<FragmentContainer> fragments) throws IOException {
//		/* call the code to gather Fragments from other clients and create
//		 * a vector containing at least k unique fragments
//		 * Search for filename + timestamp.  Compare the getHash value to
//		 * the current fragments.  If it doesn't exist, then we need that
//		 * fragment.  Hash values are based on filename, timestamp, and fragment
//		 * number.  Could also just compare to fragment numbers...
//		 * 
//		 * Another option is to search for the fragment byt name and compare 
//		 * timestamps.  If the filename is the same, but the timestamp is 
//		 * different, then search gather for the newer timestamp.
//		 * 
//		 * I will add temp code to fake the gathered fragments.  Eventaully remove 
//		 * the second argument to this function
//		 * 
//		 * Pseudocode will be
//		 * 
//		 * int numfrags = 0;
//		 * int k = null;
//		 * int n = null;
//		 * int m = null;
//		 * Vector<FragmentContainer> fragments = new Vector<FragmentContainer>();
//		 * long timestamp = null;
//		 * FragmentContainer fc = null;
//		 * while (numFrags < k) {
//		 * 		fc = searchFor(filename);
//		 *      if (timestamp == null) {// first fragment
//		 *      	timestamp = fc.getTimestamp();
//		 *      	// Initialize all the necessary variables for the decoder
//		 *      	k = fc.getK();
//		 *      	n = fc.getN();
//		 *      	m = n-k;
//		 *      	blocksize = fc.getFragment().length;
//		 *      	filesize = fc.getFilesize();
//		 *      	fragments.add(fc);
//		 *      	numfrags++;
//		 *      } else {
//		 *      	if (timestamp == fc.getTimestamp()) { //
//		 *      		// verify that we don't already have fc
//		 *      		fragments.add(fc);
//		 *      		numFrags++;
//		 *      	}
//		 *      }
//		 * }
//		 * 
//		 */
//		
//		//String directory = "/Users/shuchton/test/"; // temp code
//		//String filepath = directory+filename;
//		
//		// AES Encryption
//		MDFSCipher myCipher = MDFSCipher.getInstance();
//		
//		byte[] plainByteFile = null;
//		byte[] encryptedByteFile = null;
//		
//		// this is temp because I don't have above code working yet
//		FragmentContainer firstFrag = fragments.get(0);
//		int k = firstFrag.getK();
//		int n = firstFrag.getN();
//		int m = n - k;
//		//ShareInfo rawSecretKey = firstFrag.getKeyFragment();
//		int blocksize = firstFrag.getFragment().length;
//		long filesize = firstFrag.getFilesize();
//		ShamirSecret secret = new ShamirSecret(new ShamirSecret.PublicInfo(n, k, ShamirSecret.getPrimeUsedFor384bitSecretPayload(), null));
//		
//		// Jerasure parameters
//		int numerased = 0;
//		int[] erased = new int[n];
//		int[] erasures = new int[n];
//		byte[][] data = new byte[k][blocksize];
//		byte[][] coding = new byte[m][blocksize];
//		
//		// initialize erased
//		for (int i = 0; i < n; i++) {
//			erased[i] = 0;
//		}
//		
//		// initialize data and coding
//		for (int i = 0; i < k; i++) {
//			data[i] = null;
//		}
//		for (int i = 0; i < m; i++) {
//			coding[i] = null;
//		}
//		
//		System.out.println(String.format("k = %d, n = %d, blocksize = %d\n", k,
//				n, blocksize));
//
//		System.out.println("The file will be save as: " + filepath);
//		
//		// Store the key fragments
//		List<ShareInfo> recoveredKeyShares = new ArrayList<ShareInfo>();
//		
//		// sort fragments into data and coding and process key
//		for (int i = 0; i < fragments.size(); i++) {
//			int type = fragments.get(i).getType();
//			
//			/* KEY ***Do whatever I need to to get the key here*** KEY */
//			recoveredKeyShares.add(fragments.get(i).getKeyFragment());
//			
//			if (type == FragmentContainer.DATA_TYPE) {
//				data[fragments.get(i).getFragmentNumber()] = fragments.get(i)
//						.getFragment();
//			} else {
//				coding[fragments.get(i).getFragmentNumber() - k] = fragments
//						.get(i).getFragment();
//			}
//		}
//		
//		// process erased and erasures
//		for (int i = 0; i < k; i++) {
//			if (data[i] == null) {
//				erased[i] = 1;
//				erasures[numerased] = i;
//				numerased++;
//			}
//		}
//		for (int i = 0; i < m; i++) {
//			if (coding[i] == null) {
//				erased[k + i] = 1;
//				erasures[numerased] = k + i;
//				numerased++;
//			}
//		}
//		
//		// I guess -1 indicated the terminus of erasures in the coding
//		// method?
//		erasures[numerased] = -1;
//		
//		// call the native RSDecoder
//		encryptedByteFile = new ReedSolomon().decoder(data, coding,
//				erasures, erased, filesize, blocksize, k, n);	
//		
//		// DEBUG CODE
//		System.out.println("Encrypted length (decode): " + encryptedByteFile.length);	
//		
//		// Recover the key
//		BigInteger recoveredSecret = secret.combine(recoveredKeyShares).getSecret();
//		String recoveredString = BigIntUtilities.createStringFromBigInteger(recoveredSecret);
//		byte[] rawSecretKey = Base64.decode(recoveredString);
//		
//		// use the key to decrypt the data
//		plainByteFile = myCipher.decrypt(encryptedByteFile, rawSecretKey);	
//		
//		// DEBUG CODE
//		System.out.println("Encrypted length (decode): " + plainByteFile.length);	
//		
//		return Utility.writeBytesToFile(filepath, plainByteFile);
//	}

}
