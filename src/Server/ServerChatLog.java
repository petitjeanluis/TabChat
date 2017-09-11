package Server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class ServerChatLog {
	
	public long chatId;
	private BufferedWriter writer;
	private RandomAccessFile reader;
	
	//checks wether chat exist or not
	public static boolean findChat(long chatId){
		File dir = new File("TabChatServerData/Chats/"+Long.toString(chatId)+".txt");
		return dir.exists();
	}
	
	//called when the chat already exist
	public ServerChatLog(long chatId){
		this.chatId = chatId;
		File dir = new File("TabChatServerData/Chats/"+Long.toString(chatId)+".txt");
		try {
			writer = new BufferedWriter(new FileWriter("TabChatServerData/Chats/"+Long.toString(chatId)+".txt", true));
			reader = new RandomAccessFile(dir,"rw");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//called when the chat does not exist
	public ServerChatLog(long chatId, ArrayList<String> clientArray, ArrayList<String> messages){
		this.chatId = chatId;
		//creates chats folder if needed
		File dir = new File("TabChatServerData");
		dir.mkdir();
		dir = new File("TabChatServerData/Chats");
		dir.mkdir();
		//create chat file
		dir = new File("TabChatServerData/Chats/"+Long.toString(chatId)+".txt");
		try {
			dir.createNewFile();
			writer = new BufferedWriter(new FileWriter("TabChatServerData/Chats/"+Long.toString(chatId)+".txt", true));
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
		} catch (IOException e) {
			e.printStackTrace();
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
	
	//returns full log with usernames
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
			if(getMessageCount() == 0) return " ";
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
	
	public synchronized void close(){
		try {
			writer.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
