package com.ooze.manager;

import com.ooze.ymir.Ymir;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SysUtil {
	static Logger logger = LogManager.getLogger(Ymir.class.getName());
	
	public static void execute(String cmd) {
		logger.info("Executing command : " + cmd);
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			logger.info("Waiting command to end : " + cmd);
			p.waitFor();
			logger.info("Command " + cmd + " ended.");
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public static Vector<Object> execCmd(String cmd) {
		logger.info("Executing command : " + cmd);
		Vector<Object> result = new Vector<Object>();
		int exit = 0;
		StringBuffer outBuf = null;
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		InputStream inStream = null;
		try {
			process = runtime.exec(cmd);
			inStream = process.getInputStream();
			outBuf = new StringBuffer();
			int ch;
			while ((ch = inStream.read()) != -1)
				outBuf.append((new StringBuilder(String.valueOf((char)ch))).toString()); 
		} catch (IOException e) {
			result.add(Integer.valueOf(-1));
			result.add(e.toString());
			return result;
		} finally {
			try {
				if (process != null) {
					exit = process.exitValue();
					process.destroy();
				} 
				if (inStream != null)
					inStream.close(); 
			} catch (Exception exception) {}
		} 
		result.add(Integer.valueOf(exit));
		result.add(outBuf);
		logger.info("Command " + cmd + " ended - Result : " + result);
		return result;
	}
}
