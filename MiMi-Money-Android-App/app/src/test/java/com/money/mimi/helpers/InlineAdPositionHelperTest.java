package com.money.mimi.helpers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InlineAdPositionHelperTest {

    @Test
    public void insertsBannerBeforeFirstRowAndAfterEveryThreeRows() {
        assertTrue(InlineAdPositionHelper.isAdPosition(0, true));
        assertFalse(InlineAdPositionHelper.isAdPosition(1, true));
        assertFalse(InlineAdPositionHelper.isAdPosition(3, true));
        assertTrue(InlineAdPositionHelper.isAdPosition(4, true));
        assertTrue(InlineAdPositionHelper.isAdPosition(8, true));
    }

    @Test
    public void mapsAdapterAndDataPositionsInBothDirections() {
        int[] expectedAdapterPositions = {1, 2, 3, 5, 6, 7, 9};
        for (int dataPosition = 0; dataPosition < expectedAdapterPositions.length; dataPosition++) {
            int adapterPosition = InlineAdPositionHelper.toAdapterPosition(dataPosition, true);
            assertEquals(expectedAdapterPositions[dataPosition], adapterPosition);
            assertEquals(dataPosition, InlineAdPositionHelper.toDataPosition(adapterPosition, true));
        }
        assertEquals(-1, InlineAdPositionHelper.toDataPosition(0, true));
        assertEquals(-1, InlineAdPositionHelper.toDataPosition(4, true));
    }

    @Test
    public void countsInitialAndTrailingBannersWithoutShowingAnEmptyBanner() {
        assertEquals(0, InlineAdPositionHelper.getItemCount(0, true));
        assertEquals(2, InlineAdPositionHelper.getItemCount(1, true));
        assertEquals(5, InlineAdPositionHelper.getItemCount(3, true));
        assertEquals(6, InlineAdPositionHelper.getItemCount(4, true));
        assertEquals(4, InlineAdPositionHelper.getItemCount(4, false));
    }

    @Test
    public void keepsPinnedSupportRowBeforeFirstBannerAndConversation() {
        int leadingSupportRows = 1;
        assertFalse(InlineAdPositionHelper.isAdPositionAfterLeadingItems(0, true, leadingSupportRows));
        assertTrue(InlineAdPositionHelper.isAdPositionAfterLeadingItems(1, true, leadingSupportRows));
        assertEquals(-1, InlineAdPositionHelper.toDataPositionAfterLeadingItems(0, true, leadingSupportRows));
        assertEquals(-1, InlineAdPositionHelper.toDataPositionAfterLeadingItems(1, true, leadingSupportRows));
        assertEquals(0, InlineAdPositionHelper.toDataPositionAfterLeadingItems(2, true, leadingSupportRows));
        assertEquals(2, InlineAdPositionHelper.toAdapterPositionAfterLeadingItems(0, true, leadingSupportRows));
        assertEquals(1, InlineAdPositionHelper.getItemCountWithLeadingItems(0, true, leadingSupportRows));
        assertEquals(3, InlineAdPositionHelper.getItemCountWithLeadingItems(1, true, leadingSupportRows));
    }
}
