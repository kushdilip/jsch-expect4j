package com.dk.sshapp;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.oro.text.regex.MalformedPatternException;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;

/**
 * Hello world!
 *
 */
public class SSHClient 
{
	private static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
	private static String ENTER_CHARACTER = "\r";
	private static final int SSH_PORT = 22;
	private List<String> lstCmds = new ArrayList<String>();
	private static String[] linuxPromptRegEx = new String[]{"/home/sshtest#",
		"$", "~$","Password:",
		"[sudo] password for sshtest:",
		"Do you want to continue [Y/n]?"
		};
	
	private static String rootPassword = "kush.1dilip";
	
	private Expect4j expect = null;
	private StringBuilder buffer = new StringBuilder();
	private String userName;
	private String password;
	private String host;
	
	/**
	 *
	 * @param host
	 * @param userName
	 * @param password
	 */
	public SSHClient(String host, String userName, String password) {
		super();
		this.userName = userName;
		this.password = password;
		this.host = host;
	}
	
	public String execute(List<String> cmdsToExecute){
		this.lstCmds = cmdsToExecute;
		
		Closure closure = new Closure() {
			public void run(ExpectState expectState) throws Exception {
				buffer.append(expectState.getBuffer());
			}
		};
		List<Match> lstPattern =  new ArrayList<Match>();
		for (String regexElement : linuxPromptRegEx) {
			try {
				Match mat = new RegExpMatch(regexElement, closure);
				lstPattern.add(mat);
			} catch (MalformedPatternException e) {
				e.printStackTrace();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		
		try {
			
			expect = SSH();
			
			boolean isSuccess = true;
			for(String strCmd : lstCmds) {
				isSuccess = isSuccess(lstPattern,strCmd,2000);
				if (!isSuccess) {
					isSuccess = isSuccess(lstPattern,strCmd,2000);
				}
			}
			
			//root login
			System.out.println("root login:" + isSuccess(lstPattern, "su root",2000));
			System.out.println(isSuccess(lstPattern, rootPassword,2000));

			//installing synaptic
			System.out.println("synaptic install :" + isSuccess(lstPattern,"sudo apt-get install bluefish",2000));
			System.out.println(isSuccess(lstPattern,"Y",12000));
			
			checkResult(expect.expect(lstPattern));
			
			
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			closeConnection();
		}
		return buffer.toString();
		
	}
	
	/**
	 *
	 * @param objPattern
	 * @param strCommandPattern
	 * @return
	 */
	private boolean isSuccess(List<Match> objPattern,String strCommandPattern,int sleepTime) {
		try {
			boolean isFailed = checkResult(expect.expect(objPattern));
			
			if (!isFailed) {
				expect.send(strCommandPattern);
				expect.send(ENTER_CHARACTER);
				Thread.sleep(sleepTime);
				return true;
			}
			return false;
		} catch (MalformedPatternException ex) {
			ex.printStackTrace();
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	/**
	 *
	 * @param hostname
	 * @param username
	 * @param password
	 * @param port
	 * @return
	 * @throws Exception
	 */
	private Expect4j SSH() throws Exception {
		JSch jsch = new JSch();
		Session session = jsch.getSession(userName, host, SSH_PORT);
		if (password != null) {
			session.setPassword(password);
		}
		
		Hashtable<String,String> config = new Hashtable<String,String>();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.connect(60000);
		ChannelShell channel = (ChannelShell) session.openChannel("shell");
		Expect4j expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
		channel.connect();
		return expect;
	}
	/**
	 *
	 * @param intRetVal
	 * @return
	 */
	private boolean checkResult(int intRetVal) {
		if (intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE) {
			return true;
		}
		return false;
	}
	/**
	 *
	 */
	private void closeConnection() {
		if (expect!=null) {
			expect.close();
		}
	}
	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		SSHClient ssh = new SSHClient("192.168.56.101", "sshtest", "sshtest");
		List<String> cmdsToExecute = new ArrayList<String>();
		cmdsToExecute.add("ls");
		cmdsToExecute.add("pwd");
		cmdsToExecute.add("mkdir testdir");
		String outputLog = ssh.execute(cmdsToExecute);
		System.out.println("outputlog: " + outputLog);
	}
}
