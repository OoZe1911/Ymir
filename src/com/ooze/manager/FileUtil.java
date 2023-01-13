package com.ooze.manager;

import com.ooze.ymir.Ymir;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileUtil {
	static Logger logger = LogManager.getLogger(Ymir.class.getName());
	
	public static boolean compressFileZIP(String pathFile, String fileName, boolean preserveFilename) {
		boolean compressionOk = false;
		long size = (new File(pathFile)).length();
		if (size <= 6L) {
			logger.warn("Le fichier " + pathFile + " est trop petit pour etre compresse.");
			return false;
		} 
		String zipFileName = String.valueOf(pathFile) + ".zip";
		try {
			byte[] buffer = new byte[6];
			FileInputStream in = new FileInputStream(pathFile);
			in.read(buffer);
			in.close();
			if (buffer[0] == 80 && buffer[1] == 75 && buffer[2] == 3 && buffer[3] == 4 && (buffer[4] == 20 || buffer[4] == 10) && buffer[5] == 0) {
				logger.warn("Le fichier " + pathFile + " est deja compresse.");
				compressionOk = false;
			} else {
				ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
				out.setLevel(9);
				out.putNextEntry(new ZipEntry(fileName));
				byte[] buf = new byte[1024];
				FileInputStream fileIn = new FileInputStream(pathFile);
				int len = 0;
				while ((len = fileIn.read(buf)) > 0)
					out.write(buf, 0, len); 
				fileIn.close();
				out.closeEntry();
				out.close();
				if (preserveFilename) {
					File file = new File(pathFile);
					file.delete();
					File file_new = new File(zipFileName);
					file_new.renameTo(new File(pathFile));
				} else {
					File file = new File(pathFile);
					file.delete();
				} 
				compressionOk = true;
			} 
		} catch (IllegalArgumentException iae) {
			iae.printStackTrace();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} 
		return compressionOk;
	}
	
	public static boolean compressFileGZIP(String pathFile, boolean preserveFilename) {
		boolean compressionOk = false;
		try {
			long size = (new File(pathFile)).length();
			if (size <= 4L) {
				logger.warn("Le fichier " + pathFile + " est trop petit pour etre compresse.");
				return false;
			} 
			byte[] buffer = new byte[4];
			FileInputStream tmpin = new FileInputStream(pathFile);
			tmpin.read(buffer);
			tmpin.close();
			if ((buffer[0] == 31 && (buffer[1] & 0xFF) == 139 && buffer[2] == 8) || ((
				buffer[0] & 0xFF) == 139 && buffer[1] == 31 && buffer[3] == 8)) {
				logger.warn("Le fichier " + pathFile + " est deja compresse.");
				return false;
			} 
			GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(String.valueOf(pathFile) + ".gz"));
			FileInputStream in = new FileInputStream(pathFile);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
				out.write(buf, 0, len); 
			in.close();
			out.finish();
			out.close();
			if (preserveFilename) {
				File file = new File(pathFile);
				file.delete();
				File file_new = new File(String.valueOf(pathFile) + ".gz");
				file_new.renameTo(new File(pathFile));
			} else {
				File file = new File(pathFile);
				file.delete();
			} 
			compressionOk = true;
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return compressionOk;
	}
	
	public static Properties readPropertiesFile(String path) throws Exception {
		Properties props = new Properties();
		FileInputStream fis = new FileInputStream(path);
		props.load(fis);
		return props;
	}
	
	public static boolean fileExists(String filename) {
		File file = new File(filename);
		if (file.exists())
			return true; 
		return false;
	}
	
	public static boolean isFileLocked(File file) {
		try {
			if (System.getProperty("os.name").indexOf("Windows") == -1) {
				String result = ((StringBuffer)SysUtil.execCmd("fuser " + file.getPath()).get(1)).toString();
				if (result.length() > 0)
					return true; 
				return false;
			} 
			long taille = file.length();
			Thread.sleep(2000L);
			if (taille < file.length())
				return true; 
			return false;
		} catch (Exception e) {
			logger.warn("Impossible de controler le fichier " + file.getPath());
			return true;
		} 
	}
	
	public static String calculateHash(String filename) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			String hash = "";
			File file = new File(filename);
			byte[] data = new byte[(int)file.length()];
			FileInputStream fis = new FileInputStream(file);
			fis.read(data);
			fis.close();
			md.update(data);
			byte[] digest = md.digest();
			for (int i = 0; i < digest.length; i++) {
				String hex = Integer.toHexString(digest[i]);
				if (hex.length() == 1)
					hex = "0" + hex; 
				hex = hex.substring(hex.length() - 2);
				hash = String.valueOf(hash) + hex;
			} 
			return hash;
		} catch (Exception ex) {
			logger.error("Impossible de calculer le hash du fichier : " + filename + " - Erreur : " + ex);
			ex.printStackTrace();
			return null;
		} 
	}
}
