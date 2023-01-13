package com.ooze.manager;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.Protocol;
import com.enterprisedt.net.ftp.SecureFileTransferClient;
import com.ooze.ymir.Ymir;
import java.io.File;
import java.util.Date;
import java.util.Vector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FTPManager {
	static Logger logger = LogManager.getLogger(Ymir.class.getName());
	
	public static String[] FTP_SERVERS = null;
	public static String FTP_login_T = null;
	public static String FTP_password_T = null;
	public static String FTP_login_R = null;
	public static String FTP_password_R = null;
	public static String PROTOCOL = "FTP";
	public static String exit_command = "";
	public static boolean removeFileAfterDownload = true;
	
	public static boolean sendFiles(String local_dir, Vector<String> liste_fichiers, String remote_dir) {
	try {
		SecureFileTransferClient ftp = new SecureFileTransferClient();
		if (PROTOCOL.equalsIgnoreCase("FTPS")) {
		ftp.setProtocol(Protocol.FTPS_EXPLICIT);
		} else if (PROTOCOL.equalsIgnoreCase("FTP")) {
		ftp.setProtocol(Protocol.FTP);
		} else if (PROTOCOL.equalsIgnoreCase("SFTP")) {
		ftp.setProtocol(Protocol.SFTP);
		} 
		ftp.setUserName(FTP_login_T);
		ftp.setPassword(FTP_password_T);
		ftp.getAdvancedFTPSettings().setAutoPassiveIPSubstitution(true);
		ftp.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
		ftp.setContentType(FTPTransferType.BINARY);
		int connexionId = 0;
		boolean timedOut = true;
		while (timedOut && connexionId < FTP_SERVERS.length) {
		try {
			String[] IPandPort = FTP_SERVERS[connexionId].split(":");
			ftp.setRemoteHost(IPandPort[0]);
			ftp.setRemotePort(Integer.parseInt(IPandPort[1]));
			logger.info("T / Connexion au serveur : " + IPandPort[0] + " / " + IPandPort[1]);
			ftp.connect();
			timedOut = false;
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.warn(ex);
			if (ex.getMessage().indexOf("timed out") != -1) {
			logger.warn("--- Time out --- / switching server");
			connexionId++;
			continue;
			} 
			timedOut = false;
			return false;
		} 
		} 
		if (timedOut) {
		logger.warn("Impossible de se connecter / time out");
		return false;
		} 
		logger.info("T / Changement de repertoire : " + remote_dir);
		ftp.changeDirectory(remote_dir);
		String local_file = new String();
		for (int i = 0; i < liste_fichiers.size(); i++) {
		local_file = String.valueOf(local_dir) + "/" + (String)liste_fichiers.get(i);
		if (!FileUtil.isFileLocked(new File(local_file))) {
			logger.info("T / Transfert : " + local_file + " -> " + remote_dir + "/" + (String)liste_fichiers.get(i));
			ftp.uploadFile(local_file, liste_fichiers.get(i));
			System.out.println(new Date() + " - File sent : " + (String)liste_fichiers.get(i));
			if (!Ymir.archiveFile(local_file))
			break; 
		} else {
			System.out.println(new Date() + " - WARN - File " + (String)liste_fichiers.get(i) + " is in use (file not sent).");
			logger.warn("File " + local_file + " is in use (file not sent).");
		} 
		} 
		logger.info("T / ...Deconnexion...");
		ftp.disconnect();
	} catch (Exception ex) {
		ex.printStackTrace();
		logger.warn(ex);
		return false;
	} 
	return true;
	}
	
	public static boolean getFiles(Vector<Object> liste_rep_reception) {
	if (liste_rep_reception.size() == 0)
		return true; 
	try {
		SecureFileTransferClient ftp = new SecureFileTransferClient();
		if (PROTOCOL.equalsIgnoreCase("FTPS")) {
		ftp.setProtocol(Protocol.FTPS_EXPLICIT);
		} else if (PROTOCOL.equalsIgnoreCase("FTP")) {
		ftp.setProtocol(Protocol.FTP);
		} else if (PROTOCOL.equalsIgnoreCase("SFTP")) {
		ftp.setProtocol(Protocol.SFTP);
		} 
		ftp.setUserName(FTP_login_R);
		ftp.setPassword(FTP_password_R);
		ftp.getAdvancedFTPSettings().setAutoPassiveIPSubstitution(true);
		ftp.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
		ftp.setContentType(FTPTransferType.BINARY);
		int connexionId = 0;
		boolean timedOut = true;
		while (timedOut && connexionId < FTP_SERVERS.length) {
		try {
			String[] IPandPort = FTP_SERVERS[connexionId].split(":");
			ftp.setRemoteHost(IPandPort[0]);
			ftp.setRemotePort(Integer.parseInt(IPandPort[1]));
			logger.info("T / Connexion au serveur : " + IPandPort[0] + " / " + IPandPort[1]);
			ftp.connect();
			timedOut = false;
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.warn(ex);
			if (ex.getMessage().indexOf("timed out") != -1) {
			logger.warn("--- Time out --- / switching server");
			connexionId++;
			continue;
			} 
			timedOut = false;
			return false;
		} 
		} 
		if (timedOut) {
		logger.warn("Impossible de se connecter / time out");
		return false;
		} 
		for (int i = 0; i < liste_rep_reception.size(); i++) {
		String remote_dir = ((Vector<String>)liste_rep_reception.get(i)).get(0);
		String local_dir = ((Vector<String>)liste_rep_reception.get(i)).get(1);
		logger.debug("R / Traitement du r: " + remote_dir);
		downloadFiles(ftp, remote_dir, local_dir);
		} 
		logger.info("R / ...Deconnexion...");
		ftp.disconnect();
	} catch (Exception ex) {
		ex.printStackTrace();
		logger.warn(ex);
		return false;
	} 
	return true;
	}
	
	public static void downloadFiles(SecureFileTransferClient ftp, String remote_dir, String local_dir) {
	try {
		boolean widlcard = false;
		String pattern = new String();
		if (remote_dir.contains("*")) {
		widlcard = true;
		boolean windows = false;
		if (remote_dir.lastIndexOf("\\") != -1)
			windows = true; 
		if (windows) {
			pattern = remote_dir.substring(remote_dir.lastIndexOf("\\") + 1, remote_dir.length());
			remote_dir = remote_dir.substring(0, remote_dir.lastIndexOf("\\"));
		} else {
			pattern = remote_dir.substring(remote_dir.lastIndexOf("/") + 1, remote_dir.length());
			remote_dir = remote_dir.substring(0, remote_dir.lastIndexOf("/"));
		} 
		} 
		logger.info("R / Changement de repertoire : " + remote_dir);
		ftp.changeDirectory("/");
		ftp.changeDirectory(remote_dir);
		FTPFile[] files = ftp.directoryList();
		String local_file = new String();
		for (int i = 0; i < files.length; i++) {
		if (!files[i].isDir()) {
			boolean download = true;
			if (widlcard)
			download = wildCardMatch(files[i].getName(), pattern); 
			if (download) {
			local_file = String.valueOf(local_dir) + File.separatorChar + files[i].getName();
			logger.info("R / Transfert : " + remote_dir + "/" + files[i].getName() + " -> " + local_file);
			ftp.downloadFile(local_file, files[i].getName());
			System.out.println(new Date() + " - File received : " + files[i].getName());
			boolean deleted = false;
			if (removeFileAfterDownload)
				try {
				ftp.deleteFile(files[i].getName());
				} catch (Exception ex) {
				logger.warn("R / Impossible d'effacer le fichier distant : " + ex);
				File f = new File(local_file);
				f.delete();
				deleted = true;
				System.out.println(new Date() + " - WARN - File " + files[i].getName() + " was partial (file removed).");
				}  
			if (!deleted)
				SysUtil.execCmd(String.valueOf(exit_command) + " \"" + local_file + "\""); 
			} 
		} 
		} 
	} catch (Exception ex) {
		ex.printStackTrace();
		logger.warn(ex);
	}
	}
	
	public static boolean wildCardMatch(String text, String pattern) {
	String[] cards = pattern.split("\\*");
	byte b;
	int i;
	String[] arrayOfString1;
	for (i = (arrayOfString1 = cards).length, b = 0; b < i; ) {
		String card = arrayOfString1[b];
		int idx = text.indexOf(card);
		if (idx == -1)
		return false; 
		text = text.substring(idx + card.length());
		b++;
	} 
	return true;
	}
}
