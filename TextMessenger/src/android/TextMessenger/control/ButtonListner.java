package android.TextMessenger.control;

import android.TextMessenger.model.ChatManager;
import android.TextMessenger.model.ClassConstants;
import android.TextMessenger.view.AddChat;
import android.TextMessenger.view.AddFriend;
import android.TextMessenger.view.ChatScreen;
import android.TextMessenger.view.ChatsView;
import android.TextMessenger.view.Connect;
import android.TextMessenger.view.ContactsView;
import android.TextMessenger.view.R;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class ButtonListner implements OnClickListener{
	Activity parent;

	public ButtonListner(Activity parent) {
		this.parent = parent;
	}

	@Override
	public void onClick(View v) {
		if(v.equals(parent.findViewById(R.id.connectButton))){
			Log.d("KLIK", "DER BLEV KLIKKET");
			Connect c = (Connect)parent;
			c.clickConnect();
		
		}
		else if(v.equals(parent.findViewById(R.id.sendMessageButton))){
			ChatScreen chatS = (ChatScreen)parent;
			chatS.sendMessage();
		}
		else if (v.equals(parent.findViewById(R.id.addcontact))){
			((ContactsView) parent).addContact();
		}
		else if (v.equals(parent.findViewById(R.id.addchat))){
			((ChatsView) parent).newChat();
		}
		else if (v.equals(parent.findViewById(R.id.find))){
			AddFriend add = (AddFriend) parent;
			ClassConstants.getInstance().getContactManager().addContact(Integer.parseInt(add.getContactId()), "Searching for" + add.getContactId(), true);
			add.finish();
		}
		else if (v.equals(parent.findViewById(R.id.startChat))){
			AddChat addChat = (AddChat) parent;
			addChat.createChat();
			addChat.finish();
		}
	}
}
