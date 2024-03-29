package com.ybsystem.tweetmate.models.enums;

import lombok.Getter;

public enum ClickAction {
    MENU("MENU"),
    REPLY("REPLY"),
    RETWEET("RETWEET"),
    QUOTE("QUOTE"),
    LIKE("LIKE"),
    TALK("TALK"),
    DELETE("DELETE"),
    URL("URL"),
    HASH_SEARCH("HASH_SEARCH"),
    HASH_TWEET("HASH_TWEET"),
    USER("USER"),
    PREV_NEXT("PREV_NEXT"),
    DETAIL("DETAIL"),
    COPY("COPY"),
    SHARE("SHARE"),
    TWITTER("TWITTER"),
    NONE("NONE");

    @Getter
    private final String val;

    ClickAction(final String val) {
        this.val = val;
    }

    public static ClickAction toEnum(String val) {
        for (ClickAction obj : ClickAction.values()) {
            if (obj.getVal().equals(val)) {
                return obj;
            }
        }
        return null;
    }
}
