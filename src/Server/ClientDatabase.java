package Server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class ClientDatabase {
	
	/* 
	 * This will be the format written in the client databse file
	 * Each user contains 88 characters of space, file is indexed by 88 
	 * BEGINNING OF FILE
	 * [Username-20_Characters] [ChatIndex-3_Digits] [Name-20_Characters] [Last-20_Characters] [Password-20_Characters][\n]
	 * END OF FILE
	 * */
	
	private final int USER_SPACE = 88;
	private final int FIELD_LENGTH = 20;
	private BufferedWriter writer;
	private RandomAccessFile reader;
	
	//creates new database file, if needed
	public ClientDatabase(){		
		//creates directories for server
		File dir = new File("TabChatServerData");
		dir.mkdir();
		try {
			//creates client database txt file
			File database = new File("TabChatServerData/ClientDatabase.txt");
			database.createNewFile();
			writer = new BufferedWriter(new FileWriter("TabChatServerData/ClientDatabase.txt", true));
			reader = new RandomAccessFile(database,"rw");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//returns arraylist of all clients
	public synchronized ArrayList<String> getClients(){
		ArrayList<String> clients = new ArrayList<String>();
		try {
			reader.seek(0);
			String userData = reader.readLine();
			while(userData != null){
				String[] contents = userData.split("\\s+");
				clients.add(contents[0]);
				userData = reader.readLine();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return clients;
	}
	
	//adds a new client to the database
	//returns 0 for invalid name, 1 for invalid last, 2 for invalid pass, 3 invalid username, 4 username taken
	//returns -1 for succesfull entry
	public synchronized int addClient(String name, String last, String pass, String username){
		//verify correct length
		if(name.length()>FIELD_LENGTH) return 0;
		if(last.length()>FIELD_LENGTH) return 1;
		if(pass.length()>FIELD_LENGTH) return 2;
		if(username.length()>FIELD_LENGTH) return 3;
		//verify username is not taken
		if(findClient(username) != -1) return 4;
		//format input to write
		name = formatString(name);
		last = formatString(last);
		pass = formatString(pass);
		username = formatString(username);
		try {
			writer.write(username + " " + "000" + " " + name + " " + last + " " + pass + "\n");
			writer.flush();
			Server.cm.addClient(username);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	//returns a string with length of FIELD_LENGTH, right-padded with SPACE character
	public String formatString(String s){
		int i = FIELD_LENGTH - s.length();
		for(;i > 0; i--){
			s = s + " ";
		}
		return s;
	}
	
	//attemps to find client
	//returns client index or -1 if client not found
	public synchronized int findClient(String username){
		try {
			reader.seek(0);//must reset since we are using a global reader
			int index = 0;
			String userData = reader.readLine();
			while(userData != null){
				String[] contents = userData.split("\\s+");
				if(username.equals(contents[0])){
					return index;
				}
				userData = reader.readLine();
				index++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;//client not found
	}
	
	//client loging verification, returns client index or -1 for failed verification
	public synchronized int verifyClient(String user, String pass){
		int index = findClient(user);
		if(index == -1) return -1;
		try {
			reader.seek(index*USER_SPACE);
			String[] contents = reader.readLine().split("\\s+");
			if(contents[4].equals(pass)){
				return index;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	//changes the password of client, returns if successful 
	public synchronized boolean changePass(String user, String oldPass, String newPass){
		if(verifyClient(user,oldPass) != -1){
			try {
				reader.seek(findClient(user)*USER_SPACE+67);
				newPass = formatString(newPass);
				reader.write(newPass.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}else{
			return false;
		}
	}
	
	//changes the name of client, returns if successful
	public synchronized boolean changeName(String user, String newName){
		int index = findClient(user);
		if(index != -1){
			try {
				reader.seek(index*USER_SPACE + 25);
				newName = formatString(newName);
				reader.write(newName.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}else{
			return false;
		}
	}
	
	//changes the last name of the client, returns if successful
	public synchronized boolean changeLast(String user, String newLast){
		int index = findClient(user);
		if(index != -1){
			try {
				reader.seek(index*USER_SPACE + 46);
				newLast = formatString(newLast);
				reader.write(newLast.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}else{
			return false;
		}
	}
	
	//changes the username of the client, returns if successful
	public synchronized boolean changeUsername(String user, String newUser){
		int index = findClient(user);
		if(index != -1){
			try {
				reader.seek(index*USER_SPACE);
				newUser = formatString(newUser);
				reader.write(newUser.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}else{
			return false;
		}
	}
	
	//changes chat index, returns if successful
	public synchronized boolean changeChatIndex(String user, int newIndex){
		if((newIndex < 0) || (newIndex > 999)) return false;
		int index = findClient(user);
		if(index != -1){
			try {
				reader.seek(index*USER_SPACE+21);
				String parsedString = String.format("%03d ", newIndex);
				reader.write(parsedString.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}else{
			return false;
		}
	}
	
	public synchronized String getUsername(int id){
		try {
			reader.seek(id*USER_SPACE);
			String[] user = reader.readLine().split("\\s+");
			return user[0];
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
