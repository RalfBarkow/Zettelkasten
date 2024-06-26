package de.danielluedecke.zettelkasten.data;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.dreyeck.zettelkasten.xml.Zettel;
import de.danielluedecke.zettelkasten.view.Display;

public class HistoryTest {
    private History historyManager;
    private Display mockDisplay;
    private Zettel mockZettel;

    @BeforeMethod
    public void setUp() {
        mockDisplay = mock(Display.class);
        mockZettel = mock(Zettel.class);
        when(mockDisplay.getDisplayedZettel()).thenReturn(mockZettel); // Ensure a Zettel is displayed
        historyManager = new History(mockDisplay);
    }

    @Test
    public void testAddToHistory() {
        assertFalse(historyManager.canHistoryBack(), "Initially, canHistoryBack should be false");

        historyManager.addToHistory(1);
        assertFalse(historyManager.canHistoryBack(), "After adding the first entry, canHistoryBack should still be false");

        historyManager.addToHistory(2);
        assertTrue(historyManager.canHistoryBack(), "After adding the second entry, canHistoryBack should be true");
    }

    @Test
    public void testHistoryBack() {
        historyManager.addToHistory(1);
        historyManager.addToHistory(2);
        assertEquals(historyManager.historyBack(), 1, "historyBack should return the previous entry");
    }

    @Test
    public void testHistoryFore() {
        historyManager.addToHistory(1);
        historyManager.addToHistory(2);
        historyManager.historyBack();
        assertEquals(historyManager.navigateForwardInHistory(), 2, "historyForward should return the next entry");
    }

    @Test
    public void testCanHistoryBack() {
        historyManager.addToHistory(1);
        historyManager.addToHistory(2);
        assertTrue(historyManager.canHistoryBack(), "canHistoryBack should be true after adding two entries");
        historyManager.historyBack();
        assertFalse(historyManager.canHistoryBack(), "canHistoryBack should be false after going back to the first entry");
    }

    @Test
    public void testCanHistoryForward() {
        historyManager.addToHistory(1);
        historyManager.addToHistory(2);
        historyManager.historyBack();
        assertTrue(historyManager.canHistoryForward(), "canHistoryForward should be true after going back to the first entry");
        historyManager.navigateForwardInHistory();
        assertFalse(historyManager.canHistoryForward(), "canHistoryForward should be false after going forward to the last entry");
    }

    @Test
    public void testNavigateForwardInHistory() {
        historyManager.addToHistory(1);
        historyManager.addToHistory(2);
        historyManager.historyBack();
        assertEquals(historyManager.navigateForwardInHistory(), 2, "navigateForwardInHistory should call historyForward and return the next entry");
    }
}
