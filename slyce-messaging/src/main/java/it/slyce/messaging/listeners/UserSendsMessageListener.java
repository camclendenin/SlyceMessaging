package it.slyce.messaging.listeners;

import android.net.Uri;

import it.slyce.messaging.message.TextMessage;

/**
 * Created by matthewpage on 6/21/16.
 */
public interface UserSendsMessageListener {
    public void onUserSendsTextMessage(TextMessage textMessage);
    public void onUserSendsMediaMessage(Uri imageUri);
}
