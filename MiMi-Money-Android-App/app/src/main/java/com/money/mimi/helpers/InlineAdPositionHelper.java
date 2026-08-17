package com.money.mimi.helpers;

/**
 * Maps data rows to RecyclerView positions when a banner is shown before the
 * first row and after each group of data rows.
 */
public final class InlineAdPositionHelper {

    public static final int DEFAULT_ITEMS_PER_AD = 3;
    public static final int INVALID_POSITION = -1;

    private InlineAdPositionHelper() {
    }

    public static boolean isAdPosition(int adapterPosition, boolean adsEnabled) {
        return isAdPosition(adapterPosition, adsEnabled, DEFAULT_ITEMS_PER_AD);
    }

    static boolean isAdPosition(int adapterPosition, boolean adsEnabled, int itemsPerAd) {
        return adsEnabled
                && adapterPosition >= 0
                && adapterPosition % (itemsPerAd + 1) == 0;
    }

    public static int toDataPosition(int adapterPosition, boolean adsEnabled) {
        return toDataPosition(adapterPosition, adsEnabled, DEFAULT_ITEMS_PER_AD);
    }

    static int toDataPosition(int adapterPosition, boolean adsEnabled, int itemsPerAd) {
        if (!adsEnabled) {
            return adapterPosition;
        }
        if (adapterPosition < 0 || isAdPosition(adapterPosition, true, itemsPerAd)) {
            return INVALID_POSITION;
        }
        return adapterPosition - 1 - (adapterPosition / (itemsPerAd + 1));
    }

    public static int toAdapterPosition(int dataPosition, boolean adsEnabled) {
        return toAdapterPosition(dataPosition, adsEnabled, DEFAULT_ITEMS_PER_AD);
    }

    static int toAdapterPosition(int dataPosition, boolean adsEnabled, int itemsPerAd) {
        if (!adsEnabled || dataPosition < 0) {
            return dataPosition;
        }
        return dataPosition + 1 + (dataPosition / itemsPerAd);
    }

    public static int getItemCount(int dataSize, boolean adsEnabled) {
        return getItemCount(dataSize, adsEnabled, DEFAULT_ITEMS_PER_AD);
    }

    static int getItemCount(int dataSize, boolean adsEnabled, int itemsPerAd) {
        if (dataSize <= 0) {
            return 0;
        }
        if (!adsEnabled) {
            return dataSize;
        }
        return dataSize + 1 + (dataSize / itemsPerAd);
    }

    /** Maps positions when immutable rows (such as Support) precede the ad/chat stream. */
    public static boolean isAdPositionAfterLeadingItems(
            int adapterPosition, boolean adsEnabled, int leadingItemCount
    ) {
        return adapterPosition >= leadingItemCount
                && isAdPosition(adapterPosition - leadingItemCount, adsEnabled);
    }

    public static int toDataPositionAfterLeadingItems(
            int adapterPosition, boolean adsEnabled, int leadingItemCount
    ) {
        if (adapterPosition < leadingItemCount) return INVALID_POSITION;
        return toDataPosition(adapterPosition - leadingItemCount, adsEnabled);
    }

    public static int toAdapterPositionAfterLeadingItems(
            int dataPosition, boolean adsEnabled, int leadingItemCount
    ) {
        int inlinePosition = toAdapterPosition(dataPosition, adsEnabled);
        return inlinePosition < 0 ? inlinePosition : inlinePosition + leadingItemCount;
    }

    public static int getItemCountWithLeadingItems(
            int dataSize, boolean adsEnabled, int leadingItemCount
    ) {
        return Math.max(0, leadingItemCount) + getItemCount(dataSize, adsEnabled);
    }
}
