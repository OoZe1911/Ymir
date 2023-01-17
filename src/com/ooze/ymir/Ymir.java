package com.ooze.ymir;

import com.enterprisedt.util.license.License;
import com.ooze.manager.FTPManager;
import com.ooze.manager.FileUtil;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Ymir {
	static Logger logger = LogManager.getLogger(Ymir.class.getName());
	static File archive_dir = null;
	static int sleep_duration = 30;
	static boolean exit = false;

	public static void main(String[] args) {
		System.out.println(new Date() + " - Ymir v2.1 is starting - (c) 2012 / Syntesys Group");
		startProcess();
	}
	
	public static void startProcess() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						Ymir.handleJVMShutdown();
					}
				});

		logger.info("--------------------------------------");
		logger.info("          Starting Ymir v2.2          ");
		logger.info("--------------------------------------");

		License.setLicenseDetails("Syntesys", "360-0090-4146-3624");
		Properties conf = null;

		try {
			conf = FileUtil.readPropertiesFile("ymir.properties");
		}catch (Exception e) {
			e.printStackTrace();
			logger.fatal("Can not read configuration file : ymir.properties");
			System.exit(-1);
		}

		FTPManager.SERVERS = conf.getProperty("SERVERS").split(",");
		if (FTPManager.SERVERS[0] == null || FTPManager.SERVERS[0].length() == 0 || FTPManager.SERVERS[0].equalsIgnoreCase("null")) {
			logger.fatal("No host and port defined !");
			exit = true;
		} else {
			String hosts = new String();
			for(int i=0;i<FTPManager.SERVERS.length;i++) {
				hosts = hosts + FTPManager.SERVERS[i] + " ";
			}
			logger.info("Host to reach : " + hosts);
		}

		String protocol = conf.getProperty("PROTOCOL");
		if (protocol == null || protocol.length() == 0 || protocol.equalsIgnoreCase("null")) {
			FTPManager.PROTOCOL = "FTP";
		}else if (protocol.equalsIgnoreCase("FTP")) {
			FTPManager.PROTOCOL = "FTP";
		}else if (protocol.equalsIgnoreCase("FTPS")) {
			FTPManager.PROTOCOL = "FTPS";
		}else if (protocol.equalsIgnoreCase("SFTP")) {
			FTPManager.PROTOCOL = "SFTP";
		}else {
			FTPManager.PROTOCOL = "FTP";
		}
		logger.info("Protocol : " + FTPManager.PROTOCOL);

		FTPManager.S_login = conf.getProperty("S_LOGIN");
		if (FTPManager.S_login == null || FTPManager.S_login.length() == 0 || FTPManager.S_login.equalsIgnoreCase("null")) {
			logger.fatal("No sending login found !");
			exit = true;
		}else {
			logger.info("Sending login : " + FTPManager.S_login);
		}

		FTPManager.S_password = conf.getProperty("S_PASSWORD");
		if (FTPManager.S_password == null || FTPManager.S_password.length() == 0 || FTPManager.S_password.equalsIgnoreCase("null")) {
			logger.fatal("No password found for sending login");
			exit = true;
		}

		FTPManager.R_login = conf.getProperty("R_LOGIN");
		if (FTPManager.R_login == null || FTPManager.R_login.length() == 0 || FTPManager.R_login.equalsIgnoreCase("null")) {
			logger.fatal("No receiving login found !");
			exit = true;
		}else {
			logger.info("Receiving login : " + FTPManager.R_login);
		}


		FTPManager.R_password = conf.getProperty("R_PASSWORD");
		if (FTPManager.R_password == null || FTPManager.R_password.length() == 0 || FTPManager.R_password.equalsIgnoreCase("null")) {
			logger.fatal("No password found for receiving login");
			exit = true;
		}

		archive_dir = new File(conf.getProperty("ARCHIVE_FOLDER"));
		logger.info("Archived folder for files sent : " + archive_dir.toString());
		if (!archive_dir.exists()) {
			logger.fatal("Archived folder does not exist");
			exit = true;
		}

		sleep_duration = Integer.parseInt(conf.getProperty("SLEEP_DURATION"));
		logger.info("Sleeping dureation (in seconds) : " + sleep_duration);

		FTPManager.exit_command = conf.getProperty("EXIT_COMMAND");
		if(FTPManager.exit_command == null || FTPManager.exit_command.length() ==0) {
			FTPManager.exit_command = null;
			logger.info("No exit command");
		} else {
			logger.info("Exit command used after a file is received : " + FTPManager.exit_command);
			if (!FileUtil.fileExists(FTPManager.exit_command)) {
				logger.fatal("Exit command does not exist");
				exit = true;
			}else if (FTPManager.exit_command.contains(" ")) {
				FTPManager.exit_command = "\"" + FTPManager.exit_command + "\"";
			}
		}

		String removeFileAfterDownload = conf.getProperty("REMOVE_REMOTE_FILE_AFTER_DOWNLOAD");
		if (removeFileAfterDownload.equalsIgnoreCase("true")) {
			FTPManager.removeFileAfterDownload = true;
		}else {
			FTPManager.removeFileAfterDownload = false;
		}
		logger.info("Remove remote file after download : " + FTPManager.removeFileAfterDownload);

		if (!exit) {
			int retention_period = Integer.parseInt(conf.getProperty("RETENTION_PERIOD"));
			logger.info("Retention period for archived files (in days) : " + retention_period);
			Calendar cal = Calendar.getInstance(TimeZone.getDefault());
			boolean chgtAnnee = false;
			if (cal.get(5) < retention_period - 1 && cal.get(2) == 1)
				chgtAnnee = true; 
			for (int i = 1; i < retention_period; i++)
				cal.roll(6, false); 
			cal.set(11, 0);
			cal.set(12, 0);
			cal.set(13, 0);
			cal.set(14, 0);
			if (chgtAnnee)
				cal.roll(1, false); 
			logger.info("Removing old files ---");
			File[] children = archive_dir.listFiles();
			int nb = 0;
			for (int j = 0; j < children.length; j++) {
				File file = children[j];
				if (!file.isDirectory()) {
					Date fileDate = new Date(file.lastModified());
					if (fileDate.before(cal.getTime())) {
						logger.info("File deleted : " + file.getName() + " - " + fileDate);
						file.delete();
						nb++;
					}
				}
			}
			logger.info("--- " + nb + " old files deleted.");
		}

		if (!exit) {
			System.out.println(new Date() + " - Ymir is ready for business.");
		}else {
			System.out.println(new Date() + " - Error detected.");
		}

		Vector<Object> liste_rep_emission = new Vector<Object>();
		Vector<Object> liste_rep_reception = new Vector<Object>();
		try {
			FileInputStream fstream = new FileInputStream("ymir.conf");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			while ((strLine = br.readLine()) != null) {
				if (strLine.contains("<Sending>")) {
					strLine = br.readLine();
					if (strLine.contains("<Local>")) {
						String local_dir = strLine.substring(strLine.indexOf("<Local>") + 7, strLine.indexOf("</Local>"));
						strLine = br.readLine();
						String remote_dir = strLine.substring(strLine.indexOf("<Remote>") + 8, strLine.indexOf("</Remote>"));
						Vector<String> local_remote = new Vector<String>();
						local_remote.add(local_dir);
						local_remote.add(remote_dir);
						liste_rep_emission.add(local_remote);
						continue;
					}
					exit = true;
					logger.fatal("Incorrect configuration file : ymir.conf");
					break;
				}

				if (strLine.contains("<Receiving>")) {
					strLine = br.readLine();
					if (strLine.contains("<Remote>")) {
						String remote_dir = strLine.substring(strLine.indexOf("<Remote>") + 8, strLine.indexOf("</Remote>"));
						strLine = br.readLine();
						String local_dir = strLine.substring(strLine.indexOf("<Local>") + 7, strLine.indexOf("</Local>"));
						if (FileUtil.fileExists(local_dir)) {
							Vector<String> remote_local = new Vector<String>();
							remote_local.add(remote_dir);
							remote_local.add(local_dir);
							liste_rep_reception.add(remote_local);
							continue;
						}
						exit = true;
						logger.fatal("Local folder : " + local_dir + " does not exist");
						break;
					}
					exit = true;
					logger.fatal("Incorrect configuration file : ymir.conf");
					break;
				}
			}
			in.close();
			logger.info("Local folders used to send files (local source/remote destination): " + liste_rep_emission);
			logger.info("Remote folders used to download files (remote source/local destination) : " + liste_rep_reception);
		}catch (Exception e) {
			e.printStackTrace();
			exit = true;
		}
		logger.info("--- End initYmir()");

		while (!exit) {
			for (int i = 0; i < liste_rep_emission.size(); i++) {
				@SuppressWarnings("unchecked")
				String local_dir = ((Vector<String>)liste_rep_emission.get(i)).get(0);
				@SuppressWarnings("unchecked")
				String remote_dir = ((Vector<String>)liste_rep_emission.get(i)).get(1);
				logger.debug("Sending / Processing local folder : " + local_dir);

				File dir = new File(local_dir);

				if (!dir.exists()) {
					logger.warn("Folder does not exist : " + dir.toString());
				} else {
					File[] children = dir.listFiles();
					if (children != null && children.length > 0) {
						Vector<String> liste_fichiers = new Vector<String>();
						for (int j = 0; j < children.length; j++) {
							File file = children[j];
							if (!file.isDirectory()) {
								liste_fichiers.add(file.getName().toString());
							}else {
								logger.debug("--- Folder ignored --- : " + file.getPath());
							}
						}
						logger.debug("Local files found : " + liste_fichiers);
						if (liste_fichiers.size() > 0)
							FTPManager.sendFiles(local_dir, liste_fichiers, remote_dir); 
					}
				}
			}

			FTPManager.getFiles(liste_rep_reception);
			if (!exit)
				try {
					logger.debug("Ymir is sleeping now");
					Thread.sleep((1000 * sleep_duration));
				}catch (InterruptedException ex) {
					ex.printStackTrace();
					logger.warn(ex);
					exit = true;
				} 

		}
		logger.info("Ymir is stopping.");
		System.out.println(new Date() + " - Ymir is stopping.");
	}
	
	public static void handleJVMShutdown() {
		stopProcess();
	}
	
	public static void stopProcess() {
		System.out.println(new Date() + " - Shutdown requested / Ymir is stopping.");
		logger.warn("Shutdown requested / Ymir is stopping.");
		exit=true;
	}
	
	public static boolean archiveFile(String file) {
		logger.info("Archiving file : " + file);
		File physicalFile = new File(file);
		if (physicalFile.renameTo(new File(archive_dir, physicalFile.getName()))) {
			logger.info("Archiving file " + physicalFile.toString() + " in " + archive_dir.toString());
			return true;
		}
		if (FileUtil.fileExists(file)) {
			logger.warn("A file with a same filename has been already archived");
			File oldOne = new File(String.valueOf(archive_dir.getPath()) + "/" + physicalFile.getName());
			oldOne.delete();
			if (!physicalFile.renameTo(new File(archive_dir, physicalFile.getName()))) {
				logger.fatal("Can not archives file " + physicalFile.toString() + " in " + archive_dir.toString());
				exit = true;
				return false;
			}
			return true;
		}
		logger.warn("File does not existe anymore, can not archive.");
		return true;
	}
}
