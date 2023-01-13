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
		String cmd = "";
		if (args.length > 0)
			cmd = args[0]; 
		try {
			if ("stop".equals(cmd)) {
				stopProcess();
			}else {
				System.out.println(new Date() + " - Ymir v2.1 is starting - (c) 2012 / Syntesys Group");
				startProcess();
			}
		}catch (Exception ex) {
			System.err.println(new Date() + " - An unexpected error occured." + ex);
			logger.fatal("An unexpected error occured", ex);
		}
	}
	
	public static void startProcess() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						Ymir.handleJVMShutdown();
					}
				});

		logger.info("--------------------------------------------");
		logger.info(" Demarrage du Connecteur FTPS/SFTP Ymir v2.1");
		logger.info("      (c) 2010-2012 / Syntesys Group");
		logger.info("--------------------------------------------");

		License.setLicenseDetails("Syntesys", "360-0090-4146-3624");
		Properties conf = null;

		try {
			conf = FileUtil.readPropertiesFile("ymir.properties");
		}catch (Exception e) {
			e.printStackTrace();
			logger.fatal("Impossible de lire le fichier de configuration.");
			System.exit(-1);
		}

		if (!exit) {
			FTPManager.FTP_SERVERS = conf.getProperty("FTP_SERVERS").split(",");
			if (FTPManager.FTP_SERVERS[0] == null || FTPManager.FTP_SERVERS[0].length() == 0 || FTPManager.FTP_SERVERS[0].equalsIgnoreCase("null")) {
				logger.fatal("Pas d'adresse/port de serveur FTPS/SFTP !");
				exit = true;
			}
		}

		if (!exit) {
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
			logger.info("Protocole : " + FTPManager.PROTOCOL);
		}

		if (!exit) {
			FTPManager.FTP_login_T = conf.getProperty("FTP_LOGIN_T");
			if (FTPManager.FTP_login_T == null || FTPManager.FTP_login_T.length() == 0 || FTPManager.FTP_login_T.equalsIgnoreCase("null")) {
				logger.fatal("Pas de login de Transmission !");
				exit = true;
			}else {
				logger.info("Login de Transmission : " + FTPManager.FTP_login_T);
			}
		}

		if (!exit) {
			FTPManager.FTP_password_T = conf.getProperty("FTP_PASSWORD_T");
			if (FTPManager.FTP_password_T == null || FTPManager.FTP_password_T.length() == 0 || FTPManager.FTP_password_T.equalsIgnoreCase("null")) {
				logger.fatal("Pas de mot de passe !");
				exit = true;
			}
		}

		if (!exit) {
			FTPManager.FTP_login_R = conf.getProperty("FTP_LOGIN_R");
			if (FTPManager.FTP_login_R == null || FTPManager.FTP_login_R.length() == 0 || FTPManager.FTP_login_R.equalsIgnoreCase("null")) {
				logger.fatal("Pas de login de Reception !");
				exit = true;
			}else {
				logger.info("Login de Reception : " + FTPManager.FTP_login_R);
			}
		}
		if (!exit) {
			FTPManager.FTP_password_R = conf.getProperty("FTP_PASSWORD_R");
			if (FTPManager.FTP_password_R == null || FTPManager.FTP_password_R.length() == 0 || FTPManager.FTP_password_R.equalsIgnoreCase("null")) {
				logger.fatal("Pas de mot de passe !");
				exit = true;
			}
		}

		if (!exit) {
			archive_dir = new File(conf.getProperty("ARCHIVE_FOLDER"));
			logger.info("Repertoire d'archivage des fichiers emis : " + archive_dir.toString());
			if (!archive_dir.exists()) {
				logger.fatal("Le repertoire d'archivage n'existe pas.");
				exit = true;
			}
		}

		if (!exit) {
			sleep_duration = Integer.parseInt(conf.getProperty("SLEEP_DURATION"));
			logger.info("Duree d'endormissement : " + sleep_duration);
		}

		if (!exit) {
			FTPManager.exit_command = conf.getProperty("EXIT_COMMAND");
			logger.info("Exit de fin de transfert : " + FTPManager.exit_command);
			if (!FileUtil.fileExists(FTPManager.exit_command)) {
				logger.fatal("La commande d'exit n'existe pas.");
				exit = true;
			}else if (FTPManager.exit_command.contains(" ")) {
				FTPManager.exit_command = "\"" + FTPManager.exit_command + "\"";
			}
		}

		if (!exit) {
			String removeFileAfterDownload = conf.getProperty("REMOVE_REMOTE_FILE_AFTER_DOWNLOAD");
			if (removeFileAfterDownload.equalsIgnoreCase("true")) {
				FTPManager.removeFileAfterDownload = true;
			}else {
				FTPManager.removeFileAfterDownload = false;
			}
			logger.info("Remove remote file after download : " + FTPManager.removeFileAfterDownload);
		}

		if (!exit) {
			int retention_period = Integer.parseInt(conf.getProperty("RETENTION_PERIOD"));
			logger.info("Duree de retention des fichiers archives : " + retention_period);
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
			logger.info("Purge des vieux fichiers archives ---");
			File[] children = archive_dir.listFiles();
			int nb = 0;
			for (int j = 0; j < children.length; j++) {
				File file = children[j];
				if (!file.isDirectory()) {
					Date fileDate = new Date(file.lastModified());
					if (fileDate.before(cal.getTime())) {
						logger.info("Suppression du fichier : " + file.getName() + " - " + fileDate);
						file.delete();
						nb++;
					}
				}
			}
			logger.info("--- Fin de la purge : " + nb + " fichiers supprimes.");
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
					logger.fatal("Fichier de configuration incorrect.");
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
						logger.fatal("Le repertoire : " + local_dir + " n'existe pas.");
						break;
					}
					exit = true;
					logger.fatal("Fichier de configuration incorrect.");
					break;
				}
			}

			in.close();
			logger.info("Listes des repertoires a traiter en emission : " + liste_rep_emission);
			logger.info("Listes des repertoires a traiter en reception : " + liste_rep_reception);
		}catch (Exception e) {
			e.printStackTrace();
			exit = true;
		}
		logger.info("--- Fin initYmir()");

		while (!exit) {
			for (int i = 0; i < liste_rep_emission.size(); i++) {
				String local_dir = ((Vector<String>)liste_rep_emission.get(i)).get(0);
				String remote_dir = ((Vector<String>)liste_rep_emission.get(i)).get(1);
				logger.debug("T / Traitement du repertoire : " + local_dir);

				File dir = new File(local_dir);

				if (!dir.exists()) {
					logger.warn("Repertoire INEXISTANT : " + dir.toString());
				} else {
					File[] children = dir.listFiles();
					if (children != null && children.length > 0) {
						Vector<String> liste_fichiers = new Vector<String>();
						for (int j = 0; j < children.length; j++) {
							File file = children[j];
							if (!file.isDirectory()) {
								liste_fichiers.add(file.getName().toString());
							}else {
								logger.debug("--- Pas de traitement du repertoire --- : " + file.getPath());
							}
						}
						logger.debug("Liste des fichiers trouves : " + liste_fichiers);
						if (liste_fichiers.size() > 0)
							FTPManager.sendFiles(local_dir, liste_fichiers, remote_dir); 
					}
				}
			}

			FTPManager.getFiles(liste_rep_reception);
			if (!exit)
				try {
					logger.debug("Endormissement du Connecteur FTPS/SFTP Ymir");
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
		if (!exit) {
			System.out.println(new Date() + " - Shutdown requested / Ymir is stopping.");
			logger.warn("Shutdown requested / Arret du Connecteur FTPS/SFTP Ymir.");
			exit = true;
		}
	}
	
	public static boolean archiveFile(String file) {
		logger.info("Archivage du fichier : " + file);
		File physicalFile = new File(file);
		if (physicalFile.renameTo(new File(archive_dir, physicalFile.getName()))) {
			logger.info("Archivage du fichier " + physicalFile.toString() + " dans " + archive_dir.toString());
			return true;
		}
		if (FileUtil.fileExists(file)) {
			logger.warn("Un fichier portant le meme nom a deja ete archive.");
			File oldOne = new File(String.valueOf(archive_dir.getPath()) + "/" + physicalFile.getName());
			oldOne.delete();
			if (!physicalFile.renameTo(new File(archive_dir, physicalFile.getName()))) {
				logger.fatal("Impossible d'archiver le fichier " + physicalFile.toString() + " dans " + archive_dir.toString());
				exit = true;
				return false;
			}
			return true;
		}
		logger.warn("Impossible d'archiver : le fichier n'existe plus.");
		return true;
	}
}
