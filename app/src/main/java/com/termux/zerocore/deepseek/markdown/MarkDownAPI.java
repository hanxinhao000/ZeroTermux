package com.termux.zerocore.deepseek.markdown;

import android.content.Context;

import androidx.annotation.NonNull;

import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.parser.delimiter.DelimiterProcessor;
import org.commonmark.parser.delimiter.DelimiterRun;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.MarkwonConfiguration;

public class MarkDownAPI extends AbstractMarkwonPlugin {
    private static MarkDownAPI markDownAPI;
    private Context mContext;
    public static MarkDownAPI create(Context context) {
        if (markDownAPI == null) {
            synchronized (MarkDownAPI.class) {
                if (markDownAPI == null) {
                    markDownAPI = new MarkDownAPI(context);
                }
                return markDownAPI;
            }
        } else {
            return markDownAPI;
        }
    }

    private MarkDownAPI(Context context) {
        mContext = context;
    }


    @Override
    public void configureConfiguration(@NonNull MarkwonConfiguration.Builder builder) {
        super.configureConfiguration(builder);
        builder.linkResolver(new CustomLinkResolver(mContext));
    }

    @Override
    public void configureParser(@NonNull Parser.Builder builder) {
        super.configureParser(builder);
        builder.customDelimiterProcessor(new DelimiterProcessor() {
            @Override
            public char getOpeningCharacter() {
                return 0;
            }

            @Override
            public char getClosingCharacter() {
                return 0;
            }

            @Override
            public int getMinLength() {
                return 0;
            }

            @Override
            public int getDelimiterUse(DelimiterRun opener, DelimiterRun closer) {
                return 0;
            }

            @Override
            public void process(Text opener, Text closer, int delimiterUse) {

            }
        });
    }
    public void release() {
        mContext = null;
    }
}
