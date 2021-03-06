package edu.tamu.lenss.mdfs.comm;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

import adhoc.etc.IOUtilities;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.MDFSDirectory;
import edu.tamu.lenss.mdfs.models.DeleteFile;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;

public class DeleteFileHandler {
	
	public DeleteFileHandler(){
	}
	
	public void processPacket(DeleteFile delete){
		if(delete.getFileIds() == null || delete.getFileNames() == null){
			return;
		}
		
		if(delete.getFileIds().size() != delete.getFileNames().size()){
			return;
		}
		new DeleteFiileThread(delete.getFileNames(), delete.getFileIds()).start();
	}
	
	protected void deleteFiles(Set<Long> mergeFileIds){
		// Start a Thread to delete file
		if(!mergeFileIds.isEmpty())
			new DeleteFiileThread(mergeFileIds).start();
	}
	
	public void sendFileDeletionPacket(DeleteFile delete){
		ServiceHelper.getInstance().getMyNode().sendAODVDataContainer(delete);
	}
	
	private class DeleteFiileThread extends Thread{
		private Set<Long> fileIds;
		private List<String> fNames; 
		private List<Long> fIds;
		
		private DeleteFiileThread(Set<Long> mergeFileIds){
			fileIds = mergeFileIds;
		}
		

		private DeleteFiileThread(List<String> fNames, List<Long> fIds){
			this.fIds = fIds;
			this.fNames = fNames;
		}
		
		@Override
		public void run() {
			File rootDir = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT);
			if(!rootDir.exists())
				return;
			
			String fName;
			long fileId; 
			for(int i=0; i<fIds.size(); i++){
				fileId =  fIds.get(i);
				fName = fNames.get(i);
				
				// Clean up the MDFSDirectory
				MDFSDirectory directory = ServiceHelper.getInstance().getDirectory();
				directory.removeDecryptedFile(fileId);
				directory.removeEncryptedFile(fileId);
				directory.removeFile(fileId);
				directory.removeFileFragment(fileId);
				directory.removeKeyFragment(fileId);
				
				// Remove Encrypted File
				File file = new File(rootDir, "encrypted/" + fName);
				if(file.exists())
					file.delete();

				// Remove Decrypted File
				file = new File(rootDir, "decrypted/" + fName);
				if(file.exists())
					file.delete();
				
				// Delete File Folder
				File fileDir = new File(rootDir, MDFSFileInfo.getDirName(fName, fileId));
					
				try {
					IOUtilities.cleanCache(fileDir, 0);
					fileDir.delete();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}				
				
			}
		}
	}
}
