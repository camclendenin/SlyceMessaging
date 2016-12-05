package it.slyce.messaging;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import it.slyce.messaging.listeners.LoadMoreMessagesListener;
import it.slyce.messaging.listeners.UserClicksAvatarPictureListener;
import it.slyce.messaging.listeners.UserSendsMessageListener;
import it.slyce.messaging.message.Message;
import it.slyce.messaging.message.MessageSource;
import it.slyce.messaging.message.SpinnerMessage;
import it.slyce.messaging.message.TextMessage;
import it.slyce.messaging.message.messageItem.MessageItem;
import it.slyce.messaging.message.messageItem.MessageRecyclerAdapter;
import it.slyce.messaging.utils.CustomSettings;
import it.slyce.messaging.utils.DateUtils;
import it.slyce.messaging.utils.Refresher;
import it.slyce.messaging.utils.ScrollUtils;
import it.slyce.messaging.utils.asyncTasks.AddNewMessageTask;
import it.slyce.messaging.utils.asyncTasks.ReplaceMessagesTask;
import it.slyce.messaging.view.ViewUtils;

/**
 * Created by John C. Hunchar on 1/12/16.
 */
public class SlyceMessagingFragment extends Fragment implements OnClickListener {

    public static final String TAG = SlyceMessagingFragment.class.getSimpleName();

    private static final int START_RELOADING_DATA_AT_SCROLL_VALUE = 5000; // TODO: maybe change this? make it customizable?
    //private static final int REQUEST_CODE_PERMISSION_CAMERA_EXTERNAL_STORAGE = 232;
    //private static final int REQUEST_CODE_CAMERA_INTENT = 1;

    private EditText entryField;
    private ImageView sendButton;
    //private ImageView snapButton;
    private List<Message> messages;
    private List<MessageItem> messageItems;
    private MessageRecyclerAdapter recyclerAdapter;
    private RecyclerView recyclerView;

    private LoadMoreMessagesListener loadMoreMessagesListener;
    private UserSendsMessageListener listener;
    private CustomSettings customSettings;
    private Refresher refresher;

    private String defaultAvatarUrl;
    private String defaultDisplayName;
    private String defaultUserId;
    private int startHereWhenUpdate;
    private long recentUpdatedTime;
    private boolean moreMessagesExist;
    private int updateTimestampAtValue;

    private File file;
    private Uri outputFileUri;

    public void setPictureButtonVisible(final boolean pictureButtonVisible) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(pictureButtonVisible ? setPictureButtonVisibleRunnable : setPictureButtonGoneRunnable);
        }
    }

    private Runnable setPictureButtonVisibleRunnable = new Runnable() {
        @Override
        public void run() {
            if (getView() != null) {
                ImageView imageView = (ImageView) getView().findViewById(R.id.slyce_messaging_image_view_snap);
                imageView.setVisibility(View.VISIBLE);
            }
        }
    };

    private Runnable setPictureButtonGoneRunnable = new Runnable() {
        @Override
        public void run() {
            if (getView() != null) {
                ImageView imageView = (ImageView) getView().findViewById(R.id.slyce_messaging_image_view_snap);
                imageView.setVisibility(View.GONE);
            }
        }
    };

    private void addSpinner() {
        messages.add(0, new SpinnerMessage());
        replaceMessages(messages, -1);
    }

    private void removeSpinner() {
        if (messages.get(0) instanceof SpinnerMessage) {
            messages.remove(0);
            messageItems.remove(0);
            recyclerAdapter.notifyItemRemoved(0);
        }
    }

    public void setMoreMessagesExist(boolean moreMessagesExist) {
        if (this.moreMessagesExist == moreMessagesExist) {
            return;
        }
        this.moreMessagesExist = moreMessagesExist;
        if (moreMessagesExist) {
            addSpinner();
        } else {
            removeSpinner();
        }
        loadMoreMessagesIfNecessary();
    }

    public void setLoadMoreMessagesListener(LoadMoreMessagesListener loadMoreMessagesListener) {
        this.loadMoreMessagesListener = loadMoreMessagesListener;
        loadMoreMessagesIfNecessary();
    }

    public void setUserClicksAvatarPictureListener(UserClicksAvatarPictureListener userClicksAvatarPictureListener) {
        customSettings.userClicksAvatarPictureListener = userClicksAvatarPictureListener;
    }

    public void setDefaultAvatarUrl(String defaultAvatarUrl) {
        this.defaultAvatarUrl = defaultAvatarUrl;
    }

    public void setDefaultDisplayName(String defaultDisplayName) {
        this.defaultDisplayName = defaultDisplayName;
    }

    public void setDefaultUserId(String defaultUserId) {
        this.defaultUserId = defaultUserId;
    }

    public void setStyle(int style) {
        TypedArray ta = getContext().obtainStyledAttributes(style, R.styleable.SlyceMessagingTheme);
        customSettings.backgroundColor = ta.getColor(R.styleable.SlyceMessagingTheme_backgroundColor, Color.GRAY);
        if (getView() != null) {
            getView().setBackgroundColor(customSettings.backgroundColor); // the background color
        }
        customSettings.timestampColor = ta.getColor(R.styleable.SlyceMessagingTheme_timestampTextColor, Color.BLACK);
        customSettings.avatarBackground = ta.getResourceId(R.styleable.SlyceMessagingTheme_avatarBackground, R.drawable.shape_oval_white);
        customSettings.externalBubbleTextColor = ta.getColor(R.styleable.SlyceMessagingTheme_externalBubbleTextColor, Color.WHITE);
        customSettings.externalBubbleBackgroundColor = ta.getColor(R.styleable.SlyceMessagingTheme_externalBubbleBackground, Color.WHITE);
        customSettings.localBubbleBackgroundColor = ta.getColor(R.styleable.SlyceMessagingTheme_localBubbleBackground, Color.WHITE);
        customSettings.localBubbleTextColor = ta.getColor(R.styleable.SlyceMessagingTheme_localBubbleTextColor, Color.WHITE);
        customSettings.snackbarBackground = ta.getColor(R.styleable.SlyceMessagingTheme_snackbarBackground, Color.WHITE);
        customSettings.snackbarButtonColor = ta.getResourceId(R.styleable.SlyceMessagingTheme_snackbarButtonColor, R.color.text_blue);
        customSettings.snackbarTitleColor = ta.getColor(R.styleable.SlyceMessagingTheme_snackbarTitleColor, Color.WHITE);
        customSettings.progressSpinnerColor = ta.getColor(R.styleable.SlyceMessagingTheme_progressSpinnerColor, Color.BLACK);
        customSettings.messageInputTextColor = ta.getColor(R.styleable.SlyceMessagingTheme_messageInputTextColor, Color.BLUE);
        customSettings.messageInputTextColorHint = ta.getColor(R.styleable.SlyceMessagingTheme_messageInputTextColorHint, Color.RED);

        entryField.setTextColor(customSettings.messageInputTextColor);
        entryField.setHintTextColor(customSettings.messageInputTextColorHint);
    }

    public void addNewMessages(List<Message> messages, boolean replace) {
        if (replace) {
            this.messages.clear();
            this.messageItems.clear();
        }

        this.messages.addAll(messages);
        new AddNewMessageTask(messages, messageItems, recyclerAdapter,
                new WeakReference<>(recyclerView), getContext().getApplicationContext(), customSettings).execute();
    }

    public void addNewMessage(Message message, boolean replace) {
        List<Message> messages = new ArrayList<>();
        messages.add(message);
        addNewMessages(messages, replace);
    }

    public void setOnSendMessageListener(UserSendsMessageListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        customSettings = new CustomSettings();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = layoutInflater.inflate(R.layout.fragment_slyce_messaging, viewGroup, false);

        entryField = (EditText) view.findViewById(R.id.slyce_messaging_edit_text_entry_field);
        sendButton = (ImageView) view.findViewById(R.id.slyce_messaging_image_view_send);
        //snapButton = (ImageView) view.findViewById(R.id.slyce_messaging_image_view_snap);
        recyclerView = (RecyclerView) view.findViewById(R.id.slyce_messaging_recycler_view);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        entryField.setOnEditorActionListener(keyboardOnEditorActionListener);

        sendButton.setOnClickListener(this);
        //snapButton.setOnClickListener(this);

        messages = new ArrayList<>();
        messageItems = new ArrayList<>();
        recyclerAdapter = new MessageRecyclerAdapter(messageItems, customSettings);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext().getApplicationContext()) {
            @Override
            public boolean canScrollVertically() {
                return !refresher.isRefreshing();
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren(recycler, state);
                } catch (IndexOutOfBoundsException e) {
                    Log.w(TAG, "IndexOutOfBoundsException", e);
                }
            }
        };
        linearLayoutManager.setStackFromEnd(true);

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setOnTouchListener(recyclerViewOnTouchListener);

        startUpdateTimestampsThread();
        startHereWhenUpdate = 0;
        recentUpdatedTime = 0;
        refresher = new Refresher(false);

        loadMoreMessagesIfNecessary();
        startLoadMoreMessagesListener();

        /*if (ContextCompat.checkSelfPermission(getContext().getApplicationContext(),
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getContext().getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION_CAMERA_EXTERNAL_STORAGE);*/
    }

    private TextView.OnEditorActionListener keyboardOnEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if (EditorInfo.IME_ACTION_SEND == i) {
                sendUserTextMessage();
                return true;
            }

            return false;
        }
    };

    private View.OnTouchListener recyclerViewOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            return refresher.isRefreshing();
        }
    };

    private void startUpdateTimestampsThread() {
        ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(1);
        scheduleTaskExecutor.scheduleAtFixedRate(startUpdateTimestampsRunnable, 0, 62, TimeUnit.SECONDS);
    }

    private Runnable startUpdateTimestampsRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = startHereWhenUpdate; i < messages.size() && i < messageItems.size(); i++) {
                try {
                    MessageItem messageItem = messageItems.get(i);
                    Message message = messageItem.getMessage();
                    if (DateUtils.dateNeedsUpdated(message.getDate(), messageItem.getDate())) {
                        messageItem.updateDate(message.getDate());
                        updateTimestampAtValue(i);
                    } else if (i == startHereWhenUpdate) {
                        i++;
                    }
                } catch (RuntimeException exception) {
                    Log.d("debug", exception.getMessage());
                    exception.printStackTrace();
                }
            }
        }
    };

    private void startLoadMoreMessagesListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recyclerView.setOnScrollChangeListener(recyclerViewOnScrollChangeListener);
        } else {
            recyclerView.addOnScrollListener(recyclerViewOnScrollListener);
        }
    }

    @SuppressLint("NewApi")
    private View.OnScrollChangeListener recyclerViewOnScrollChangeListener = new View.OnScrollChangeListener() {
        @Override
        public void onScrollChange(View view, int i, int i1, int i2, int i3) {
            loadMoreMessagesIfNecessary();
        }
    };

    private RecyclerView.OnScrollListener recyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            loadMoreMessagesIfNecessary();
        }
    };

    private void loadMoreMessagesIfNecessary() {
        if (shouldReloadData()) {
            recentUpdatedTime = new Date().getTime();
            loadMoreMessages();
        }
    }

    private void loadMoreMessages() {
        new AsyncTask<Void, Void, Void>() {
            private WeakReference<List<Message>> fragmentMessages = new WeakReference<>(SlyceMessagingFragment.this.messages);
            private boolean spinnerExists;
            private List<Message> messages;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                if (fragmentMessages != null && fragmentMessages.get() != null) {
                    refresher.setIsRefreshing(true);
                    spinnerExists = moreMessagesExist && fragmentMessages.get().get(0) instanceof SpinnerMessage;
                    if (spinnerExists) {
                        fragmentMessages.get().remove(0);
                    }
                }
            }

            @Override
            protected Void doInBackground(Void... voids) {
                messages = loadMoreMessagesListener.loadMoreMessages();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if (fragmentMessages != null && fragmentMessages.get() != null) {
                    int upTo = messages.size();
                    for (int i = messages.size() - 1; i >= 0; i--) {
                        Message message = messages.get(i);
                        fragmentMessages.get().add(0, message);
                    }
                    if (spinnerExists && moreMessagesExist) {
                        messages.add(0, new SpinnerMessage());
                    }
                    refresher.setIsRefreshing(false);
                    replaceMessages(messages, upTo);
                }
            }
        }.execute();
    }

    public void replaceMessages(List<Message> messages) {
        replaceMessages(messages, -1);
    }

    public void messageSent(TextMessage textMessage, boolean successful, String failureMessage) {
        if (successful) {
            entryField.setText("");
            addNewMessage(textMessage, false);
            ScrollUtils.scrollToBottomAfterDelay(recyclerView, recyclerAdapter);
        } else {
            Snackbar snackbar = Snackbar.make(recyclerView, failureMessage, Snackbar.LENGTH_SHORT);
            ViewGroup group = (ViewGroup) snackbar.getView();
            for (int i = 0; i < group.getChildCount(); i++) {
                View v = group.getChildAt(i);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(customSettings.snackbarTitleColor);
                }
            }
            snackbar.getView().setBackgroundColor(customSettings.snackbarBackground);
            snackbar.show();
        }
    }

    private void replaceMessages(List<Message> messages, int upTo) {
        if (getContext() != null) {
            new ReplaceMessagesTask(messages, messageItems, recyclerAdapter,
                    getContext().getApplicationContext(), refresher, upTo).execute();
        }
    }

    private boolean shouldReloadData() {
        int scrollOffset = recyclerView.computeVerticalScrollOffset();
        return !(loadMoreMessagesListener == null || !moreMessagesExist)
                && scrollOffset < START_RELOADING_DATA_AT_SCROLL_VALUE
                && recentUpdatedTime + 1000 < new Date().getTime();
    }

    private void updateTimestampAtValue(final int i) {
        if (getActivity() != null) {
            updateTimestampAtValue = i;
            getActivity().runOnUiThread(updateTimestampAtValueRunnable);
        }
    }

    private Runnable updateTimestampAtValueRunnable = new Runnable() {
        @Override
        public void run() {
            recyclerAdapter.notifyItemChanged(updateTimestampAtValue);
        }
    };

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.slyce_messaging_image_view_send) {
            sendUserTextMessage();
        }/* else if (v.getId() == R.id.slyce_messaging_image_view_snap) {
            entryField.setText("");
            final File mediaStorageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            final File root = new File(mediaStorageDir, "SlyceMessaging");
            root.mkdirs();
            final String fname = "img_" + System.currentTimeMillis() + ".jpg";
            file = new File(root, fname);
            outputFileUri = Uri.fromFile(file);
            Intent takePhotoIntent = new CameraActivity.IntentBuilder(getContext().getApplicationContext())
                    .skipConfirm()
                    .to(file)
                    .zoomStyle(ZoomStyle.SEEKBAR)
                    .updateMediaStore()
                    .build();
            Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickPhotoIntent.setType("image/*");
            Intent chooserIntent = Intent.createChooser(pickPhotoIntent, "Take a photo or select one from your device");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
            try {
                startActivityForResult(chooserIntent, REQUEST_CODE_CAMERA_INTENT);
            } catch (RuntimeException exception) {
                Log.d("debug", exception.getMessage());
                exception.printStackTrace();
            }
        }*/
    }

    /*@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQUEST_CODE_PERMISSION_CAMERA_EXTERNAL_STORAGE == requestCode || (data == null && file.exists())) {
            return;
        }

        try {
            if (REQUEST_CODE_CAMERA_INTENT == requestCode && Activity.RESULT_OK == resultCode) {
                final boolean isCamera;
                if (data == null) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();
                    isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                }

                Uri selectedImageUri;
                if (isCamera && data != null) { // if there is no picture
                    return;
                }
                if (isCamera || data.getData() == null) {
                    selectedImageUri = outputFileUri;
                } else {
                    selectedImageUri = data.getData();
                }
                MediaMessage message = new MediaMessage();
                message.setUrl(selectedImageUri.toString());
                message.setDate(System.currentTimeMillis());
                message.setDisplayName(defaultDisplayName);
                message.setSource(MessageSource.LOCAL_USER);
                message.setAvatarUrl(defaultAvatarUrl);
                message.setUserId(defaultUserId);
                addNewMessage(message);
                ScrollUtils.scrollToBottomAfterDelay(recyclerView, recyclerAdapter);
                if (listener != null)
                    listener.onUserSendsMediaMessage(selectedImageUri);
            }
        } catch (RuntimeException exception) {
            Log.d("debug", exception.getMessage());
            exception.printStackTrace();
        }
    }*/

    private void sendUserTextMessage() {
        String text = ViewUtils.getStringFromEditText(entryField);
        if (TextUtils.isEmpty(text)) {
            return;
        }

        // Build messageData object
        TextMessage message = new TextMessage();
        message.setDate(System.currentTimeMillis());
        message.setAvatarUrl(defaultAvatarUrl);
        message.setSource(MessageSource.LOCAL_USER);
        message.setDisplayName(defaultDisplayName);
        message.setText(text);
        message.setUserId(defaultUserId);

        if (listener != null) {
            listener.onUserSendsTextMessage(message);
        }
    }
}
