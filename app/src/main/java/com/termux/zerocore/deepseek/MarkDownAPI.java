package com.termux.zerocore.deepseek;

import androidx.annotation.NonNull;

import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.parser.delimiter.DelimiterProcessor;
import org.commonmark.parser.delimiter.DelimiterRun;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.simple.ext.SimpleExtPlugin;

public class MarkDownAPI extends AbstractMarkwonPlugin {
    public static MarkDownAPI create() {
        return new MarkDownAPI();
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
}
