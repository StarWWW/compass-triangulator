package compasstri;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The seller and the price of a compass for sale, read from the lines a server's auction house puts under the item
 * ("Seller: Notch", "Price: $1.5K", "Satıcı: Ahmet", "Fiyat: $250K"...). Servers word these differently, so the
 * lines are matched loosely; config/compasstri.properties can set exact patterns instead.
 *
 * @param seller  null when no line names one
 * @param price   null when no line has one
 */
public record Listing(@Nullable String seller, @Nullable String price) {
    public static final Listing NONE = new Listing(null, null);

    // U: \w and \b know letters such as "ı", so "Satıcı:" ends a word
    private static final Pattern SELLER_WORDS = Pattern.compile(
        "(?iuU)^\\W*(seller|satıcı|satici|owner|sahibi|sahip|listed by|sold by|by|player|oyuncu|vendeur|verkäufer|vendedor|venditore)\\b");
    private static final Pattern PRICE_WORDS = Pattern.compile(
        "(?iuU)^\\W*(price|fiyat|cost|ücret|tutar|preis|prix|precio|preço|prezzo|bid|buy now)\\b");
    /** "$1.5K", "$1,500,000", "$ 12.3M". */
    private static final Pattern MONEY = Pattern.compile("\\$\\s?[0-9][0-9.,]*\\s?[kKmMbBtT]?");
    private static final Pattern LEGACY_FORMAT = Pattern.compile("§.");

    /** The waypoint name: "Notch Lodestone $1.5K", leaving out what is not known. */
    public String waypointName() {
        StringBuilder name = new StringBuilder();
        if (seller != null) name.append(seller).append(' ');
        name.append("Lodestone");
        if (price != null) name.append(' ').append(price);
        // Xaero's waypoint files use ':' between fields
        return name.toString().replace(':', ' ').trim();
    }

    /**
     * @param lines          the item's lore, as plain text
     * @param sellerPattern  a regular expression whose first group is the seller, or null for the loose matching
     * @param pricePattern   the same for the price
     */
    public static Listing parse(List<String> lines, @Nullable String sellerPattern, @Nullable String pricePattern) {
        String seller = null;
        String price = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = clean(lines.get(i));
            String next = i + 1 < lines.size() ? clean(lines.get(i + 1)) : "";
            if (seller == null) seller = sellerPattern != null ? group(sellerPattern, line) : labelled(SELLER_WORDS, line, next);
            if (price == null) price = pricePattern != null ? group(pricePattern, line) : labelled(PRICE_WORDS, line, next);
        }
        if (price == null && pricePattern == null) {
            // no "Price:" line: the first amount of money anywhere
            for (String raw : lines) {
                Matcher m = MONEY.matcher(clean(raw));
                if (m.find()) {
                    price = m.group().replace(" ", "");
                    break;
                }
            }
        }
        if (price != null) {
            Matcher m = MONEY.matcher(price);
            if (m.find()) price = m.group().replace(" ", "");
        }
        return new Listing(blankToNull(seller), blankToNull(price));
    }

    private static String clean(String line) {
        return LEGACY_FORMAT.matcher(line).replaceAll("").trim();
    }

    /** "Seller: Notch" -> "Notch"; "Seller:" with the value on the next line -> the next line. */
    private static @Nullable String labelled(Pattern words, String line, String next) {
        Matcher m = words.matcher(line);
        if (!m.find()) return null;
        String rest = line.substring(m.end()).replaceFirst("^[\\s:»>\\-–•|=]+", "").trim();
        if (rest.isEmpty()) rest = next;
        // "by Notch (online)" -> "Notch"
        int space = rest.indexOf(' ');
        if (words == SELLER_WORDS && space > 0) rest = rest.substring(0, space);
        return rest;
    }

    private static @Nullable String group(String pattern, String line) {
        try {
            Matcher m = Pattern.compile(pattern).matcher(line);
            return m.find() && m.groupCount() >= 1 ? m.group(1).trim() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @Nullable String blankToNull(@Nullable String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
