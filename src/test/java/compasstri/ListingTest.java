package compasstri;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListingTest {
    private static Listing parse(String... lines) {
        return Listing.parse(List.of(lines), null, null);
    }

    @Test
    void englishLabels() {
        Listing l = parse("A compass", "Seller: Notch", "Price: $1.5K", "Expires: 1d 2h");
        assertEquals("Notch", l.seller());
        assertEquals("$1.5K", l.price());
        assertEquals("Notch Lodestone $1.5K", l.waypointName());
    }

    @Test
    void turkishLabelsAndColourCodes() {
        Listing l = parse("§7Satıcı: §fAhmet", "§7Fiyat: §a$250K");
        assertEquals("Ahmet", l.seller());
        assertEquals("$250K", l.price());
    }

    @Test
    void otherSeparatorsAndValueOnTheNextLine() {
        Listing l = parse("Owner » Steve (online)", "Price:", "$1,500,000");
        assertEquals("Steve", l.seller());
        assertEquals("$1,500,000", l.price());
    }

    @Test
    void anAmountWithoutALabel() {
        Listing l = parse("Click to buy", "$12.3M each");
        assertNull(l.seller());
        assertEquals("$12.3M", l.price());
        assertEquals("Lodestone $12.3M", l.waypointName());
    }

    @Test
    void nothingKnown() {
        assertEquals("Lodestone", parse("Just a compass").waypointName());
        assertEquals("Lodestone", Listing.NONE.waypointName());
    }

    @Test
    void ownPatterns() {
        Listing l = Listing.parse(List.of("[AH] Notch sells for 2.5K coins"), "\\[AH\\] (\\S+) sells", "for (\\S+) coins");
        assertEquals("Notch", l.seller());
        assertEquals("2.5K", l.price());
        // a broken pattern is ignored instead of crashing
        assertNull(Listing.parse(List.of("Seller: Notch"), "(", null).seller());
    }

    @Test
    void noColonsInTheWaypointName() {
        assertEquals("Not ch Lodestone $1", new Listing("Not:ch", "$1").waypointName());
    }
}
