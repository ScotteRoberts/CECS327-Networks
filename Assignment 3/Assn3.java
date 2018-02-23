import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTP;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Assn3 {
	private class Argument {
		public String command;
		public String target;
		public Argument (String command, String target) {
			this.command = command;
			this.target = target;
		}
	}

	private FTPClient client;

	public static void main(String[] args) {
		if(args.length >= 2) {
			Assn3 app = new Assn3();
			app.client = new FTPClient();

			// Connect and login to the FTP Client.
			try {
				app.client.connect(args[0]);
				String[] loginCredentials = args[1].split("\\:");
				app.client.login(loginCredentials[0], loginCredentials[1]);
				//app.client.enterLocalPassiveMode();
			}
			catch(IOException e) {
				System.out.println(e);
			}

			// Execute any Command-line arguments.
			int iter = 2;
			while(iter < args.length) {
				Argument arg = app.parseArgument(args[iter]);
				app.commandMenu(arg.command, arg.target);
				iter++;
			}

			// Logout and disconnect from the FTP Client.
			try {
				app.client.logout();
				app.client.disconnect();
				System.out.println("Successful Logout.");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} 
		else {
			System.out.println("Please enter your credential command arguments.");
		}	
	}
	
	// Personalized Command-line Parser
	private Argument parseArgument(String arg)
	{
		String[] parsedArg = arg.split("\\s+");
		String command = parsedArg[0];
		String argTarget = "";

		// This method works when the 'Target' has single quotes around it.
		// Does not function with (') in normal words.
		if (arg.contains("'")) {
			try {
				Pattern p = Pattern.compile("(?:^|\\s)'([^']*?)'(?:$|\\s)");
				Matcher m = p.matcher(arg);
				if(m.find()) {
					argTarget = m.group(1);
				}
			} catch (PatternSyntaxException ex) {
				ex.printStackTrace();
			}
		}
		// This method works when there are not single quotes around the target.
		else {
			if(parsedArg.length > 1) {
				for (int i = 1; i < parsedArg.length - 2; i++) {
					argTarget += parsedArg[i] + " ";
				}
			}
			argTarget += parsedArg[parsedArg.length - 1];
		}
		
		return new Argument(command, argTarget);
	}

	// The switch menu directs the flow of the program.
	private void commandMenu(String command, String argTarget) {
		switch(command) {
			case "ls":
				listFiles();
				break;
			case "cd":
				changeDirectory(argTarget);
				break;
			case "delete":
				deleteFile(argTarget);
				break;
			case "get":
				getItem(argTarget);
				break;
			case "put":
				putItem(argTarget);
				break;
			case "mkdir":
				makeDirectory(argTarget);
				break;
			case "rmdir":
				removeDirectory(argTarget);
				break;
			default:
				System.out.println("Command \"" + command + "\" was not recognized.");
		}
	}
	
	// "ls" command
	private void listFiles() {
		try {
			System.out.println(client.printWorkingDirectory());
	        FTPFile[] files = client.listFiles(client.printWorkingDirectory());
	        for (FTPFile file : files) {
	            System.out.println(file.getName());
	        }
		} catch(IOException e) {
			System.out.println(e);
		}
	}

	// "cd" command
	private void changeDirectory(String directory) {
		try {
			client.changeWorkingDirectory(directory);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// "delete" command
	private void deleteFile(String fileName) {
		try {
			client.deleteFile(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// "get" command for downloading files and directories.
	private void getItem(String itemName) {
		try {
			// Checks if the item exists as a directory
			if(client.changeWorkingDirectory(itemName)) {
				String dir = client.printWorkingDirectory();
				System.out.println(dir);
				String saveDir = Paths.get("").toAbsolutePath().toString();
				downloadDirectory(dir, "", saveDir);
			}
			// Else the item is a file.
			else {
				downloadSingleFile(itemName, Paths.get("").toAbsolutePath().toString() + File.separator + itemName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// "get" command helper for single file.
	private boolean downloadSingleFile(String remoteFilePath, String localFilePath) {
		boolean isDownloaded = false;
		try {
			File downloadedFile = new File(localFilePath);

			File parentDir = downloadedFile.getParentFile();
    		if (!parentDir.exists()) {
        		parentDir.mkdir();
    		}

			FileOutputStream outputStream = new FileOutputStream(downloadedFile);
			client.setFileType(FTP.BINARY_FILE_TYPE);
			isDownloaded = client.retrieveFile(remoteFilePath, outputStream);

		} catch(IOException e) {
			e.printStackTrace();
		}
		return isDownloaded;
	}

	// "get" command helper for downloading directories.
	private void downloadDirectory(String parentDir, String currentDir, String saveDir) throws IOException {
		String dirToList = parentDir;
		if (!currentDir.equals("")) {
			dirToList += "/" + currentDir;
		}

		FTPFile[] subFiles = client.listFiles(dirToList);
		if (subFiles != null && subFiles.length > 0) {
			for (FTPFile aFile : subFiles) {
				String currentFileName = aFile.getName();
				System.out.println("Getting: " + currentFileName);
				if (currentFileName.equals(".") || currentFileName.equals("..")) {
					// skip parent directory and the directory itself
					continue;
				}
				String filePath = parentDir + "/" + currentDir + "/" + currentFileName;
				System.out.println("\nThe path right now..." + filePath);
				if (currentDir.equals("")) {
					filePath = parentDir + "/" + currentFileName;
				}
	
				String newDirPath = saveDir + parentDir + File.separator + currentDir + File.separator + currentFileName;
				if (currentDir.equals("")) {
					newDirPath = parentDir + File.separator + currentFileName;
				}
				
				if (aFile.isDirectory()) {
					// create the directory in saveDir
					File newDir = new File(newDirPath);
					boolean created = newDir.mkdirs();
					if (created) {
						System.out.println("CREATED the directory: " + newDirPath);
					} else {
						System.out.println("COULD NOT create the directory: " + newDirPath);
					}
	
					// download the sub directory
					downloadDirectory(dirToList, currentFileName, saveDir);
				} 
				else {
					// download the file
					boolean success = downloadSingleFile(filePath, newDirPath);
					if (success) {
						System.out.println("DOWNLOADED the file: " + filePath);
					} 
					else {
						System.out.println("COULD NOT download the file: "
								+ filePath);
					}
				}
			}
		}
		else {
			System.out.println("Directory is empty or null. Fix later...");
		}
	}

	private void putItem(String itemName) {
		try {
			// Checks if the item exists as a directory
			File localFile = new File(itemName);
			String remoteDirPath = client.printWorkingDirectory();
			String localDirPath = Paths.get("").toAbsolutePath().toString();
			if(localFile.isDirectory()) {
				// System.out.println(dir);
				uploadDirectory(remoteDirPath, localDirPath, "");
			}
			// Else the item is a file.
			else {
				uploadSingleFile(localDirPath, remoteDirPath);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// "put" command
	public boolean uploadSingleFile(String localFilePath, String remoteFilePath) throws IOException {
		File localFile = new File(localFilePath);
		InputStream inputStream = new FileInputStream(localFile);
		try {
			client.setFileType(FTP.BINARY_FILE_TYPE);
			return client.storeFile(remoteFilePath, inputStream);
		} finally {
			inputStream.close();
		}
	}

	public void uploadDirectory(String remoteDirPath, String localParentDir, String remoteParentDir) throws IOException {
		System.out.println("LISTING directory: " + localParentDir);
	
		File localDir = new File(localParentDir);
		File[] subFiles = localDir.listFiles();
		if (subFiles != null && subFiles.length > 0) {
			for (File item : subFiles) {
				String remoteFilePath = remoteDirPath + "/" + remoteParentDir + "/" + item.getName();
				if (remoteParentDir.equals("")) {
					remoteFilePath = remoteDirPath + "/" + item.getName();
				}
				if (item.isFile()) {
					// upload the file
					String localFilePath = item.getAbsolutePath();
					System.out.println("About to upload the file: " + localFilePath);
					boolean uploaded = uploadSingleFile(localFilePath, remoteFilePath);
					if (uploaded) {
						System.out.println("UPLOADED a file to: "
								+ remoteFilePath);
					} else {
						System.out.println("COULD NOT upload the file: "
								+ localFilePath);
					}
				} else {
					// create directory on the server
					boolean created = client.makeDirectory(remoteFilePath);
					if (created) {
						System.out.println("CREATED the directory: "
								+ remoteFilePath);
					} else {
						System.out.println("COULD NOT create the directory: "
								+ remoteFilePath);
					}
	
					// upload the sub directory
					String parent = remoteParentDir + "/" + item.getName();
					if (remoteParentDir.equals("")) {
						parent = item.getName();
					}
	
					localParentDir = item.getAbsolutePath();
					uploadDirectory(remoteDirPath, localParentDir, parent);
				}
			}
		}
	}

	// "mkdir" command
	private void makeDirectory(String dirName) {
		try {
			client.makeDirectory(dirName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// "rmdir" command
	private void removeDirectory(String parentDir) {
		try {
			removeDirectoryHelper(parentDir, "");
		} catch(IOException e) {
			e.printStackTrace();
		}
		
	}

	// "rmdir" command helper
	private void removeDirectoryHelper(String parentDir, String currentDir) throws IOException {
        String dirToList = parentDir;
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir;
		}
        FTPFile[] subFiles = client.listFiles(dirToList);
		
        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile aFile : subFiles) {
				System.out.println("RMDIR: Files listed" + aFile);
                String currentFileName = aFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and the directory itself
                    continue;
                }
                String filePath = parentDir + "/" + currentDir + "/" + currentFileName;
                if (currentDir.equals("")) {
                    filePath = parentDir + "/" + currentFileName;
                }
 
                if (aFile.isDirectory()) {
                    // remove the sub directory
                    removeDirectoryHelper(dirToList, currentFileName);
                } else {
                    // delete the file
                    boolean deleted = client.deleteFile(filePath);
                    if (deleted) {
                        System.out.println("DELETED the file: " + filePath);
                    } else {
                        System.out.println("CANNOT delete the file: "
                                + filePath);
                    }
                }
            }
            // finally, remove the directory itself
            boolean removed = client.removeDirectory(dirToList);
            if (removed) {
                System.out.println("REMOVED the directory: " + dirToList);
            } else {
                System.out.println("CANNOT remove the directory: " + dirToList);
            }
        }
    }
}