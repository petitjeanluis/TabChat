package Client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
public class ClientChatLog {
	
	private long chatId;
	private BufferedWriter writer;
	private RandomAccessFile reader;
	
	public static Map<Long,ClientChatLog> getClientLogs(String username){
		Map<Long,ClientChatLog> clientLogs = new LinkedHashMap<Long,ClientChatLog>();
		File dir = new File("TabChatClientData");
		dir.mkdir();
		dir = new File("TabChatClientData/"+username);
		dir.mkdir();
		dir = new File("TabChatClientData/"+username+"/Chats");
		if(dir.mkdir()) return clientLogs;
		String[] list = dir.list();
		for(int i = 0; i < list.length; i++){
			String[] contents = list[i].split("\\.");
			clientLogs.put(Long.parseLong(contents[0]),new ClientChatLog(Long.parseLong(contents[0]),username));
		}
		return clientLogs;
	}
	
	public static boolean findChat(long chatId,String username){
		File dir = new File("TabChatClientData/"+username+"/Chats/"+Long.toString(chatId)+".txt");
		return dir.exists();
	}
	
	//this is called when the chat already exist
	public ClientChatLog(long chatId, String username){
		this.chatId = chatId;
		File dir = new File("TabChatClientData/"+username+"/Chats/"+Long.toString(chatId)+".txt");
		try {
			writer = new BufferedWriter(new FileWriter("TabChatClientData/"+username+"/Chats/"+Long.toString(chatId)+".txt", true));
			reader = new RandomAccessFile(dir,"rw");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//this is called for new chats
	public ClientChatLog(long chatId, ArrayList<String> clientArray, ArrayList<String> messages, String username){
		this.chatId = chatId;
		//creates chats folder if needed
		File dir = new File("TabChatClientData");
		dir.mkdir();
		dir = new File("TabChatClientData/"+username);
		dir.mkdir();
		dir = new File("TabChatClientData/"+username+"/Chats/");
		dir.mkdir();
		//create chat file
		dir = new File("TabChatClientData/"+username+"/Chats/"+Long.toString(chatId)+".txt");
		try {
			dir.createNewFile();
			writer = new BufferedWriter(new FileWriter("TabChatClientData/"+username+"/Chats/"+Long.toString(chatId)+".txt", true));
			reader = new RandomAccessFile(dir,"rw");
			for(String s: clientArray){
				writer.write(s+":");
				writer.flush();
			}
			writer.write(":0               \n");
			writer.flush();
			if(messages != null){
				for(String s : messages){
					writer.write(s+"\n");
					writer.flush();
				}
			}
			updateContentCount(messages.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void updateMessages(ArrayList<String> messages){
		int count = (int)this.getMessageCount();
		for(int i = count; i < messages.size(); i++){
			String[] message = messages.get(i).split(":");
			addContents(message[0],message[1],message[2]);
		}
	}
	
	public synchronized void addContents(String username, String type, String contents){
		try {
			writer.write(username+":"+type+":"+contents+"\n");
			writer.flush();
			updateContentCount(1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized ArrayList<String> getLog(){
		ArrayList<String> log = new ArrayList<String>();
		try {
			reader.seek(0);
			String message = reader.readLine();
			do {
				log.add(message);
				message = reader.readLine();
			} while (message != null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return log;
	} 
	
	public synchronized ArrayList<String> getUsers(){
		ArrayList<String> users = new ArrayList<String>();
		try {
			reader.seek(0);
			String[] contents = reader.readLine().split(":");
			for(int i = 0; i < contents.length; i++){
				if(contents[i].equals("")) break;
				users.add(contents[i]);
			}
			return users;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return users;
	}
	
	public synchronized ArrayList<String> getMessages(){
		ArrayList<String> messages = new ArrayList<String>();
		try {
			reader.seek(0);
			reader.readLine();
			String message = reader.readLine();
			while(message != null){
				messages.add(message);
				message = reader.readLine();
			}
			return messages;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return messages;
	}
	
	public synchronized ArrayList<String> getImages(){
		ArrayList<String> messages = new ArrayList<String>();
		try {
			reader.seek(0);
			reader.readLine();
			String message = reader.readLine();
			while(message != null){
				String[] contents = message.split(":");
				if(contents[1].equals("i")){
					messages.add(message);
				}
				message = reader.readLine();
			}
			return messages;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return messages;
	}
	
	public synchronized void close(){
		try {
			writer.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized long getMessageCount(){
		try {
			reader.seek(0);
			char a = (char)reader.readByte();
			char b;
			while(true){
				if(a == ':'){
					b = (char) reader.readByte();
					if(b == ':') break;
				}
				a = (char)reader.readByte();
			}
			String countString = reader.readLine();
			long count = Long.parseLong(countString.split("\\s+")[0]);
			return count;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public synchronized void updateContentCount(long inc){
		if(inc == 0) return;
		try {
			reader.seek(0);
			char a = (char)reader.readByte();
			char b;
			while(true){
				if(a == ':'){
					b = (char) reader.readByte();
					if(b == ':') break;
				}
				a = (char)reader.readByte();
			}
			long pos = reader.getFilePointer();
			String countString = reader.readLine();
			long count = Long.parseLong(countString.split("\\s+")[0]);
			count += inc;
			countString = formatString(Long.toString(count),16);
			reader.seek(pos);
			reader.write(countString.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//returns a string with length of FIELD_LENGTH, right-padded with SPACE character
	public String formatString(String s, int length){
		if(s.length() > length){
			try {
				throw new NumberFormatException();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		int i = length - s.length();
		for(;i > 0; i--){
			s = s + " ";
		}
		return s;
	}
	
	public Long getChatId(){
		return chatId;
	}
	
	public synchronized String getLastMessage(){
		if(getMessageCount() == 0) return null;
		String message = "";
		try {
			long pos = reader.length() - 2;
			reader.seek(pos);
			char a = (char)reader.readByte();
			pos -= 1;
			while(a != '\n'){
				reader.seek(pos);
				a = (char) reader.readByte();
				pos -= 1;
			}
			return reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return message;
	}
}
