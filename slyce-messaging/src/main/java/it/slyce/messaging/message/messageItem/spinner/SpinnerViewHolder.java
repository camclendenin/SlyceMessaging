package it.slyce.messaging.message.messageItem.spinner;

import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ProgressBar;

import it.slyce.messaging.R;
import it.slyce.messaging.message.messageItem.MessageViewHolder;
import it.slyce.messaging.utils.CustomSettings;

/**
 * Created by matthewpage on 7/5/16.
 */
public class SpinnerViewHolder extends MessageViewHolder {

    public ProgressBar spinner;

    public SpinnerViewHolder(View itemView, CustomSettings customSettings) {
        super(itemView, customSettings);
        this.spinner = (ProgressBar) itemView.findViewById(R.id.loading_bar);
        this.spinner.getIndeterminateDrawable().setColorFilter(customSettings.progressSpinnerColor, PorterDuff.Mode.MULTIPLY);
    }
}