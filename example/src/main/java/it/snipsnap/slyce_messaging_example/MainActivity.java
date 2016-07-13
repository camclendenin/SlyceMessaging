package it.snipsnap.slyce_messaging_example;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.slyce.messaging.SlyceMessagingFragment;
import it.slyce.messaging.listeners.LoadMoreMessagesListener;
import it.slyce.messaging.listeners.UserSendsMessageListener;
import it.slyce.messaging.message.MediaMessage;
import it.slyce.messaging.message.Message;
import it.slyce.messaging.message.MessageSource;
import it.slyce.messaging.message.TextMessage;

public class MainActivity extends AppCompatActivity {

    private static String[] latin = {
            "Vestibulum dignissim enim a mauris malesuada fermentum. Vivamus tristique consequat turpis, pellentesque.",
            "Quisque nulla leo, venenatis ut augue nec, dictum gravida nibh. Donec augue nisi, volutpat nec libero.",
            "Cras varius risus a magna egestas.",
            "Mauris tristique est eget massa mattis iaculis. Aenean sed purus tempus, vestibulum ante eget, vulputate mi. Pellentesque hendrerit luctus tempus. Cras feugiat orci.",
            "Morbi ullamcorper, sapien mattis viverra facilisis, nisi urna sagittis nisi, at luctus lectus elit.",
            "Phasellus porttitor fermentum neque. In semper, libero id mollis.",
            "Praesent fermentum hendrerit leo, ac rutrum ipsum vestibulum at. Curabitur pellentesque augue.",
            "Mauris finibus mi commodo molestie placerat. Curabitur aliquam metus vitae erat vehicula ultricies. Sed non quam nunc.",
            "Praesent vel velit at turpis vestibulum eleifend ac vehicula leo. Nunc lacinia tellus eget ipsum consequat fermentum. Nam purus erat, mollis sed ullamcorper nec, efficitur.",
            "Suspendisse volutpat enim eros, et."
    };

    private static String[] urls = {
            "http://en.l4c.me/fullsize/googleplex-mountain-view-california-1242979177.jpg",
            "http://entropymag.org/wp-content/uploads/2014/10/outer-space-wallpaper-pictures.jpg",
            "http://www.bolwell.com/wp-content/uploads/2013/09/bolwell-metal-fabrication-raw-material.jpg",
            "http://www.bytscomputers.com/wp-content/uploads/2013/12/pc.jpg",
            "https://content.edmc.edu/assets/modules/ContentWebParts/AI/Locations/New-York-City/startpage-masthead-slide.jpg"
    };

    private static Message getRandomMessage() {
        Message message;
        if (Math.random() < 0.9) {
            TextMessage textMessage = new TextMessage();
            textMessage.setText(latin[(int) (Math.random() * 10)]);
            message = textMessage;
        } else {
            MediaMessage mediaMessage = new MediaMessage();
            mediaMessage.setUrl(urls[(int)(Math.random() * 5)]);
            message = mediaMessage;
        }
        message.setDate(new Date().getTime());
        if (Math.random() > 0.5) {
            message.setAvatarUrl("https://lh3.googleusercontent.com/-Y86IN-vEObo/AAAAAAAAAAI/AAAAAAAKyAM/6bec6LqLXXA/s0-c-k-no-ns/photo.jpg");
            message.setUserId("LP");
            message.setOrigin(MessageSource.EXTERNAL_USER);
        } else {
            message.setAvatarUrl("https://scontent-lga3-1.xx.fbcdn.net/v/t1.0-9/10989174_799389040149643_722795835011402620_n.jpg?oh=bff552835c414974cc446043ac3c70ca&oe=580717A5");
            message.setUserId("MP");
            message.setOrigin(MessageSource.LOCAL_USER);
        }
        return message;
    }

    SlyceMessagingFragment slyceMessagingFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(it.snipsnap.slyce_messaging_example.R.layout.activity_main);

        slyceMessagingFragment = (SlyceMessagingFragment) getFragmentManager().findFragmentById(R.id.fragment_for_scout);
        slyceMessagingFragment.setDefaultAvatarUrl("https://scontent-lga3-1.xx.fbcdn.net/v/t1.0-9/10989174_799389040149643_722795835011402620_n.jpg?oh=bff552835c414974cc446043ac3c70ca&oe=580717A5");
        slyceMessagingFragment.setDefaultDisplayName("Matthew Page");
        slyceMessagingFragment.setDefaultUserId("uhtnaeohnuoenhaeuonthhntouaetnheuontheuo");

        slyceMessagingFragment.setOnSendMessageListener(new UserSendsMessageListener() {
            @Override
            public void onUserSendsTextMessage(String text) {
                Log.d("inf", "******************************** " + text);
            }

            @Override
            public void onUserSendsMediaMessage(Uri imageUri) {
                Log.d("inf", "******************************** " + imageUri);
            }
        });

        slyceMessagingFragment.setLoadMoreMessagesListener(new LoadMoreMessagesListener() {
            @Override
            public List<Message> loadMoreMessages() {
                Log.d("info", "loadMoreMessages()");

                ArrayList<Message> messages = new ArrayList<>();
                for (int i = 0; i < 50; i++)
                    messages.add(getRandomMessage());

                Log.d("info", "loadMoreMessages() returns");
                return messages;
            }
        });

        slyceMessagingFragment.setMoreMessagesExist(true);


    }


}