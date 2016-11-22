package it.slyce.messaging.utils.asyncTasks;

import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

import it.slyce.messaging.message.Message;
import it.slyce.messaging.message.MessageSource;
import it.slyce.messaging.message.messageItem.MessageItem;
import it.slyce.messaging.message.messageItem.MessageRecyclerAdapter;
import it.slyce.messaging.utils.CustomSettings;
import it.slyce.messaging.utils.MessageUtils;
import it.slyce.messaging.utils.ScrollUtils;

public class AddNewMessageTask extends AsyncTask {
    private List<Message> messages;
    private List<MessageItem> mMessageItems;
    private MessageRecyclerAdapter mRecyclerAdapter;
    private WeakReference<RecyclerView> mRecyclerView;
    private Context context;
    private CustomSettings customSettings;
    private int rangeStartingPoint;

    public AddNewMessageTask(
            List<Message> messages,
            List<MessageItem> mMessageItems,
            MessageRecyclerAdapter mRecyclerAdapter,
            WeakReference<RecyclerView> mRecyclerView,
            Context context,
            CustomSettings customSettings) {
        this.messages = messages;
        this.mMessageItems = mMessageItems;
        this.mRecyclerAdapter = mRecyclerAdapter;
        this.mRecyclerView = mRecyclerView;
        this.context = context;
        this.customSettings = customSettings;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        this.rangeStartingPoint = mMessageItems.size() - 1;
        for (Message message : messages) {
            if (context == null) {
                return null;
            }
            mMessageItems.add(message.toMessageItem(context)); // this call is why we need the AsyncTask
        }

        for (int i = rangeStartingPoint; i < mMessageItems.size(); i++) {
            MessageUtils.markMessageItemAtIndexIfFirstOrLastFromSource(i, mMessageItems);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        if (o != null)
            return;
        boolean isAtBottom = mRecyclerView.get() != null && !mRecyclerView.get().canScrollVertically(1);
        boolean isAtTop = mRecyclerView.get() != null && !mRecyclerView.get().canScrollVertically(-1);
        mRecyclerAdapter.notifyItemRangeInserted(rangeStartingPoint + 1, messages.size() - rangeStartingPoint - 1);
        mRecyclerAdapter.notifyItemChanged(rangeStartingPoint);
        if (mRecyclerView.get() != null && (isAtBottom || messages.get(messages.size() - 1).getSource() == MessageSource.LOCAL_USER)) {
            mRecyclerView.get().scrollToPosition(mRecyclerAdapter.getItemCount() - 1);
        } else {
            if (isAtTop) {
                ScrollUtils.scrollToTopAfterDelay(mRecyclerView.get(), mRecyclerAdapter);
            }
            Snackbar snackbar = Snackbar.make(mRecyclerView.get(), "New message!", Snackbar.LENGTH_SHORT)
                    .setAction("VIEW", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mRecyclerView.get().smoothScrollToPosition(mRecyclerAdapter.getItemCount() - 1);
                        }
                    });
            ViewGroup group = (ViewGroup) snackbar.getView();
            for (int i = 0; i < group.getChildCount(); i++) {
                View v = group.getChildAt(i);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(customSettings.snackbarTitleColor);
                }
            }
            snackbar.setActionTextColor(ContextCompat.getColor(context, customSettings.snackbarButtonColor));
            snackbar.getView().setBackgroundColor(customSettings.snackbarBackground);
            snackbar.show();
        }
    }
}
