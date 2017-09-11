package Client;

import javafx.scene.control.Label;

public class ImageLabel extends ChatLabel{
	public ImageLabel(Label messageLabel, double height, boolean isMe){
		super(messageLabel,height, isMe);
		if(isMe){
			messageLabel.getStyleClass().add("imageLabelMe");
		}else{
			messageLabel.getStyleClass().add("imageLabelOther");
		}
	}
}
