package one.mixin.android.widget.markdown;

import org.commonmark.internal.Delimiter;
import org.commonmark.internal.inline.AsteriskDelimiterProcessor;
import org.commonmark.internal.inline.UnderscoreDelimiterProcessor;
import org.commonmark.internal.util.LinkScanner;
import org.commonmark.internal.util.Parsing;
import org.commonmark.node.*;
import org.commonmark.parser.InlineParser;
import org.commonmark.parser.InlineParserContext;
import org.commonmark.parser.delimiter.DelimiterProcessor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleInlineParser implements InlineParser {

    private static final Pattern TICKS = Pattern.compile("`+");

    private static final Pattern TICKS_HERE = Pattern.compile("^`+");

    private static final Pattern UNICODE_WHITESPACE_CHAR = Pattern.compile("^[\\p{Zs}\t\r\n\f]");

    private static final Pattern FINAL_SPACE = Pattern.compile(" *$");
    private final BitSet specialCharacters;
    private final BitSet delimiterCharacters;
    private final Map<Character, DelimiterProcessor> delimiterProcessors;

    private String input;
    private int index;

    /**
     * Top delimiter (emphasis, strong emphasis or custom emphasis). (Brackets are on a separate stack, different
     * from the algorithm described in the spec.)
     */
    private Delimiter lastDelimiter;

    /**
     * Top opening bracket (<code>[</code> or <code>![)</code>).
     */

    public SimpleInlineParser(InlineParserContext inlineParserContext) {
        this.delimiterProcessors = calculateDelimiterProcessors(inlineParserContext.getCustomDelimiterProcessors());
        this.delimiterCharacters = calculateDelimiterCharacters(this.delimiterProcessors.keySet());
        this.specialCharacters = calculateSpecialCharacters(delimiterCharacters);
    }

    public static BitSet calculateDelimiterCharacters(Set<Character> characters) {
        BitSet bitSet = new BitSet();
        for (Character character : characters) {
            bitSet.set(character);
        }
        return bitSet;
    }

    public static BitSet calculateSpecialCharacters(BitSet delimiterCharacters) {
        BitSet bitSet = new BitSet();
        bitSet.or(delimiterCharacters);
        bitSet.set('\n');
        bitSet.set('`');
        bitSet.set('[');
        bitSet.set(']');
        bitSet.set('\\');
        bitSet.set('!');
        bitSet.set('<');
        bitSet.set('&');
        return bitSet;
    }

    public static Map<Character, DelimiterProcessor> calculateDelimiterProcessors(List<DelimiterProcessor> delimiterProcessors) {
        Map<Character, DelimiterProcessor> map = new HashMap<>();
        addDelimiterProcessors(Arrays.<DelimiterProcessor>asList(new AsteriskDelimiterProcessor(), new UnderscoreDelimiterProcessor()), map);
        addDelimiterProcessors(delimiterProcessors, map);
        return map;
    }

    private static void addDelimiterProcessors(Iterable<DelimiterProcessor> delimiterProcessors, Map<Character, DelimiterProcessor> map) {
        for (DelimiterProcessor delimiterProcessor : delimiterProcessors) {
            char opening = delimiterProcessor.getOpeningCharacter();
            char closing = delimiterProcessor.getClosingCharacter();
            if (opening == closing) {
                DelimiterProcessor old = map.get(opening);
                if (old != null && old.getOpeningCharacter() == old.getClosingCharacter()) {
                    StaggeredDelimiterProcessor s;
                    if (old instanceof StaggeredDelimiterProcessor) {
                        s = (StaggeredDelimiterProcessor) old;
                    } else {
                        s = new StaggeredDelimiterProcessor(opening);
                        s.add(old);
                    }
                    s.add(delimiterProcessor);
                    map.put(opening, s);
                } else {
                    addDelimiterProcessorForChar(opening, delimiterProcessor, map);
                }
            } else {
                addDelimiterProcessorForChar(opening, delimiterProcessor, map);
                addDelimiterProcessorForChar(closing, delimiterProcessor, map);
            }
        }
    }

    private static void addDelimiterProcessorForChar(char delimiterChar, DelimiterProcessor toAdd, Map<Character, DelimiterProcessor> delimiterProcessors) {
        DelimiterProcessor existing = delimiterProcessors.put(delimiterChar, toAdd);
        if (existing != null) {
            throw new IllegalArgumentException("Delimiter processor conflict with delimiter char '" + delimiterChar + "'");
        }
    }

    /**
     * Parse content in block into inline children, using reference map to resolve references.
     */
    @Override
    public void parse(String content, Node block) {
        reset(content.trim());

        Node previous = null;
        while (true) {
            Node node = parseInline(previous);
            previous = node;
            if (node != null) {
                block.appendChild(node);
            } else {
                break;
            }
        }

        processDelimiters(null);
        mergeChildTextNodes(block);
    }

    void reset(String content) {
        this.input = content;
        this.index = 0;
        this.lastDelimiter = null;
    }


    private Text text(String text, int beginIndex, int endIndex) {
        return new Text(text.substring(beginIndex, endIndex));
    }

    private Text text(String text) {
        return new Text(text);
    }

    /**
     * Parse the next inline element in subject, advancing input index.
     * On success, return the new inline node.
     * On failure, return null.
     */
    private Node parseInline(Node previous) {
        char c = peek();
        if (c == '\0') {
            return null;
        }

        Node node;
        switch (c) {
            case '\n':
                node = parseNewline(previous);
                break;
            case '\\':
                node = parseBackslash();
                break;
            case '`':
                node = parseBackticks();
                break;
            default:
                boolean isDelimiter = delimiterCharacters.get(c);
                if (isDelimiter) {
                    DelimiterProcessor delimiterProcessor = delimiterProcessors.get(c);
                    node = parseDelimiters(delimiterProcessor, c);
                } else {
                    node = parseString();
                }
                break;
        }
        if (node != null) {
            return node;
        } else {
            index++;
            // When we get here, it's only for a single special character that turned out to not have a special meaning.
            // So we shouldn't have a single surrogate here, hence it should be ok to turn it into a String.
            String literal = String.valueOf(c);
            return text(literal);
        }
    }

    /**
     * If RE matches at current index in the input, advance index and return the match; otherwise return null.
     */
    private String match(Pattern re) {
        if (index >= input.length()) {
            return null;
        }
        Matcher matcher = re.matcher(input);
        matcher.region(index, input.length());
        boolean m = matcher.find();
        if (m) {
            index = matcher.end();
            return matcher.group();
        } else {
            return null;
        }
    }

    /**
     * Returns the char at the current input index, or {@code '\0'} in case there are no more characters.
     */
    private char peek() {
        if (index < input.length()) {
            return input.charAt(index);
        } else {
            return '\0';
        }
    }

    /**
     * Parse zero or more space characters, including at most one newline.
     */
    /**
     * Parse a newline. If it was preceded by two spaces, return a hard line break; otherwise a soft line break.
     */
    private Node parseNewline(Node previous) {
        index++; // assume we're at a \n

        // Check previous text for trailing spaces.
        // The "endsWith" is an optimization to avoid an RE match in the common case.
        if (previous instanceof Text && ((Text) previous).getLiteral().endsWith(" ")) {
            Text text = (Text) previous;
            String literal = text.getLiteral();
            Matcher matcher = FINAL_SPACE.matcher(literal);
            int spaces = matcher.find() ? matcher.end() - matcher.start() : 0;
            if (spaces > 0) {
                text.setLiteral(literal.substring(0, literal.length() - spaces));
            }
            if (spaces >= 2) {
                return new HardLineBreak();
            } else {
                return new SoftLineBreak();
            }
        } else {
            return new SoftLineBreak();
        }
    }

    /**
     * Parse a backslash-escaped special character, adding either the escaped  character, a hard line break
     * (if the backslash is followed by a newline), or a literal backslash to the block's children.
     */
    private Node parseBackslash() {
        index++;
        Node node;
        if (peek() == '\n') {
            node = new HardLineBreak();
            index++;
        } else if (index < input.length() ) {
            node = text(input, index, index + 1);
            index++;
        } else {
            node = text("\\");
        }
        return node;
    }

    /**
     * Attempt to parse backticks, returning either a backtick code span or a literal sequence of backticks.
     */
    private Node parseBackticks() {
        String ticks = match(TICKS_HERE);
        if (ticks == null) {
            return null;
        }
        int afterOpenTicks = index;
        String matched;
        while ((matched = match(TICKS)) != null) {
            if (matched.equals(ticks)) {
                Code node = new Code();
                String content = input.substring(afterOpenTicks, index - ticks.length());
                content = content.replace('\n', ' ');

                // spec: If the resulting string both begins and ends with a space character, but does not consist
                // entirely of space characters, a single space character is removed from the front and back.
                if (content.length() >= 3 &&
                        content.charAt(0) == ' ' &&
                        content.charAt(content.length() - 1) == ' ' &&
                        Parsing.hasNonSpace(content)) {
                    content = content.substring(1, content.length() - 1);
                }

                node.setLiteral(content);
                return node;
            }
        }
        // If we got here, we didn't match a closing backtick sequence.
        index = afterOpenTicks;
        return text(ticks);
    }

    /**
     * Attempt to parse delimiters like emphasis, strong emphasis or custom delimiters.
     */
    private Node parseDelimiters(DelimiterProcessor delimiterProcessor, char delimiterChar) {
        DelimiterData res = scanDelimiters(delimiterProcessor, delimiterChar);
        if (res == null) {
            return null;
        }
        int length = res.count;
        int startIndex = index;

        index += length;
        Text node = text(input, startIndex, index);

        // Add entry to stack for this opener
        lastDelimiter = new Delimiter(node, delimiterChar, res.canOpen, res.canClose, lastDelimiter);
        lastDelimiter.length = length;
        lastDelimiter.originalLength = length;
        if (lastDelimiter.previous != null) {
            lastDelimiter.previous.next = lastDelimiter;
        }

        return node;
    }

    /**
     * Attempt to parse a link label, returning number of characters parsed.
     */
    int parseLinkLabel() {
        if (index >= input.length() || input.charAt(index) != '[') {
            return 0;
        }

        int startContent = index + 1;
        int endContent = LinkScanner.scanLinkLabelContent(input, startContent);
        // spec: A link label can have at most 999 characters inside the square brackets.
        int contentLength = endContent - startContent;
        if (endContent == -1 || contentLength > 999) {
            return 0;
        }
        if (endContent >= input.length() || input.charAt(endContent) != ']') {
            return 0;
        }
        index = endContent + 1;
        return contentLength + 2;
    }
    /**
     * Parse a run of ordinary characters, or a single character with a special meaning in markdown, as a plain string.
     */
    private Node parseString() {
        int begin = index;
        int length = input.length();
        while (index != length) {
            if (specialCharacters.get(input.charAt(index))) {
                break;
            }
            index++;
        }
        if (begin != index) {
            return text(input, begin, index);
        } else {
            return null;
        }
    }

    /**
     * Scan a sequence of characters with code delimiterChar, and return information about the number of delimiters
     * and whether they are positioned such that they can open and/or close emphasis or strong emphasis.
     *
     * @return information about delimiter run, or {@code null}
     */
    private DelimiterData scanDelimiters(DelimiterProcessor delimiterProcessor, char delimiterChar) {
        int startIndex = index;

        int delimiterCount = 0;
        while (peek() == delimiterChar) {
            delimiterCount++;
            index++;
        }

        if (delimiterCount < delimiterProcessor.getMinLength()) {
            index = startIndex;
            return null;
        }

        String before = startIndex == 0 ? "\n" :
                input.substring(startIndex - 1, startIndex);

        char charAfter = peek();
        String after = charAfter == '\0' ? "\n" :
                String.valueOf(charAfter);

        // We could be more lazy here, in most cases we don't need to do every match case.
        boolean beforeIsWhitespace = UNICODE_WHITESPACE_CHAR.matcher(before).matches();
        boolean afterIsWhitespace = UNICODE_WHITESPACE_CHAR.matcher(after).matches();

        boolean leftFlanking = !afterIsWhitespace && beforeIsWhitespace ;
        boolean rightFlanking = !beforeIsWhitespace && afterIsWhitespace;
        boolean canOpen;
        boolean canClose;
        if (delimiterChar == '_') {
            canOpen = leftFlanking;
            canClose = rightFlanking;
        } else {
            canOpen = leftFlanking && delimiterChar == delimiterProcessor.getOpeningCharacter();
            canClose = rightFlanking && delimiterChar == delimiterProcessor.getClosingCharacter();
        }

        index = startIndex;
        return new DelimiterData(delimiterCount, canOpen, canClose);
    }

    private void processDelimiters(Delimiter stackBottom) {

        Map<Character, Delimiter> openersBottom = new HashMap<>();

        // find first closer above stackBottom:
        Delimiter closer = lastDelimiter;
        while (closer != null && closer.previous != stackBottom) {
            closer = closer.previous;
        }
        // move forward, looking for closers, and handling each
        while (closer != null) {
            char delimiterChar = closer.delimiterChar;

            DelimiterProcessor delimiterProcessor = delimiterProcessors.get(delimiterChar);
            if (!closer.canClose || delimiterProcessor == null) {
                closer = closer.next;
                continue;
            }

            char openingDelimiterChar = delimiterProcessor.getOpeningCharacter();

            // Found delimiter closer. Now look back for first matching opener.
            int useDelims = 0;
            boolean openerFound = false;
            boolean potentialOpenerFound = false;
            Delimiter opener = closer.previous;
            while (opener != null && opener != stackBottom && opener != openersBottom.get(delimiterChar)) {
                if (opener.canOpen && opener.delimiterChar == openingDelimiterChar) {
                    potentialOpenerFound = true;
                    useDelims = delimiterProcessor.getDelimiterUse(opener, closer);
                    if (useDelims > 0) {
                        openerFound = true;
                        break;
                    }
                }
                opener = opener.previous;
            }

            if (!openerFound) {
                if (!potentialOpenerFound) {
                    // Set lower bound for future searches for openers.
                    // Only do this when we didn't even have a potential
                    // opener (one that matches the character and can open).
                    // If an opener was rejected because of the number of
                    // delimiters (e.g. because of the "multiple of 3" rule),
                    // we want to consider it next time because the number
                    // of delimiters can change as we continue processing.
                    openersBottom.put(delimiterChar, closer.previous);
                    if (!closer.canOpen) {
                        // We can remove a closer that can't be an opener,
                        // once we've seen there's no matching opener:
                        removeDelimiterKeepNode(closer);
                    }
                }
                closer = closer.next;
                continue;
            }

            Text openerNode = opener.node;
            Text closerNode = closer.node;

            // Remove number of used delimiters from stack and inline nodes.
            opener.length -= useDelims;
            closer.length -= useDelims;
            openerNode.setLiteral(
                    openerNode.getLiteral().substring(0,
                            openerNode.getLiteral().length() - useDelims));
            closerNode.setLiteral(
                    closerNode.getLiteral().substring(0,
                            closerNode.getLiteral().length() - useDelims));

            removeDelimitersBetween(opener, closer);
            // The delimiter processor can re-parent the nodes between opener and closer,
            // so make sure they're contiguous already. Exclusive because we want to keep opener/closer themselves.
            mergeTextNodesBetweenExclusive(openerNode, closerNode);
            delimiterProcessor.process(openerNode, closerNode, useDelims);

            // No delimiter characters left to process, so we can remove delimiter and the now empty node.
            if (opener.length == 0) {
                removeDelimiterAndNode(opener);
            }

            if (closer.length == 0) {
                Delimiter next = closer.next;
                removeDelimiterAndNode(closer);
                closer = next;
            }
        }

        // remove all delimiters
        while (lastDelimiter != null && lastDelimiter != stackBottom) {
            removeDelimiterKeepNode(lastDelimiter);
        }
    }

    private void removeDelimitersBetween(Delimiter opener, Delimiter closer) {
        Delimiter delimiter = closer.previous;
        while (delimiter != null && delimiter != opener) {
            Delimiter previousDelimiter = delimiter.previous;
            removeDelimiterKeepNode(delimiter);
            delimiter = previousDelimiter;
        }
    }

    /**
     * Remove the delimiter and the corresponding text node. For used delimiters, e.g. `*` in `*foo*`.
     */
    private void removeDelimiterAndNode(Delimiter delim) {
        Text node = delim.node;
        node.unlink();
        removeDelimiter(delim);
    }

    /**
     * Remove the delimiter but keep the corresponding node as text. For unused delimiters such as `_` in `foo_bar`.
     */
    private void removeDelimiterKeepNode(Delimiter delim) {
        removeDelimiter(delim);
    }

    private void removeDelimiter(Delimiter delim) {
        if (delim.previous != null) {
            delim.previous.next = delim.next;
        }
        if (delim.next == null) {
            // top of stack
            lastDelimiter = delim.previous;
        } else {
            delim.next.previous = delim.previous;
        }
    }

    private void mergeTextNodesBetweenExclusive(Node fromNode, Node toNode) {
        // No nodes between them
        if (fromNode == toNode || fromNode.getNext() == toNode) {
            return;
        }

        mergeTextNodesInclusive(fromNode.getNext(), toNode.getPrevious());
    }

    private void mergeChildTextNodes(Node node) {
        // No children or just one child node, no need for merging
        if (node.getFirstChild() == node.getLastChild()) {
            return;
        }

        mergeTextNodesInclusive(node.getFirstChild(), node.getLastChild());
    }

    private void mergeTextNodesInclusive(Node fromNode, Node toNode) {
        Text first = null;
        Text last = null;
        int length = 0;

        Node node = fromNode;
        while (node != null) {
            if (node instanceof Text) {
                Text text = (Text) node;
                if (first == null) {
                    first = text;
                }
                length += text.getLiteral().length();
                last = text;
            } else {
                mergeIfNeeded(first, last, length);
                first = null;
                last = null;
                length = 0;
            }
            if (node == toNode) {
                break;
            }
            node = node.getNext();
        }

        mergeIfNeeded(first, last, length);
    }

    private void mergeIfNeeded(Text first, Text last, int textLength) {
        if (first != null && last != null && first != last) {
            StringBuilder sb = new StringBuilder(textLength);
            sb.append(first.getLiteral());
            Node node = first.getNext();
            Node stop = last.getNext();
            while (node != stop) {
                sb.append(((Text) node).getLiteral());
                Node unlink = node;
                node = node.getNext();
                unlink.unlink();
            }
            String literal = sb.toString();
            first.setLiteral(literal);
        }
    }

    private static class DelimiterData {

        final int count;
        final boolean canClose;
        final boolean canOpen;

        DelimiterData(int count, boolean canOpen, boolean canClose) {
            this.count = count;
            this.canOpen = canOpen;
            this.canClose = canClose;
        }
    }
}
