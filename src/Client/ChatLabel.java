package Client;

import javafx.scene.control.Label;

public class ChatLabel {
	private Label label;
	private double height;
	private double y;
	private boolean isMe;
	
	public ChatLabel(Label label, double height, boolean isMe){
		this.label = label;
		this.height = height;
		this.isMe = isMe;
		y = -100;
	}
	
	public double getHeight(){
		return height;
	}
	
	public Label getLabel(){
		return label;
	}
	
	public double getY(){
		return y;
	}
	
	public boolean isMe(){
		return isMe;
	}
	
	public void setY(double y){
		label.setLayoutY(y);
		this.y = y;
	}
}
