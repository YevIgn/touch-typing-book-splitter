package touch.typing.book.splitter;

import com.beust.jcommander.IDefaultProvider;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableMap;
import cz.jirutka.unidecode.Unidecode;
import lombok.RequiredArgsConstructor;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

@Parameters(separators = "=")
public final class Splitter {
    private static final String SOURCE_OPTION = "--source";
    private static final String TARGET_OPTION = "--target";
    private static final String ENGLISH_MODEL = "en-sent.bin";
    private static final int DEFAULT_ESTIMATED_FRAGMENT_SIZE = 1000;
    private static final Map<String, Function<String, String>> DEFAULT_PROVIDERS = ImmutableMap
            .<String, Function<String, String>>builder()
            .put(SOURCE_OPTION, Function.identity())
            .put(TARGET_OPTION, fileName -> fileName + "_processed")
            .build();

    @Parameter(names = SOURCE_OPTION, description = "Source text path.")
    private String source;

    @Parameter(names = TARGET_OPTION, description = "Resulting dictionary path.")
    private String target;

    @Parameter(names = "--size", description = "Minimal size of the fragment of resulting dictionary."
            + " Fragments are built up sentence by sentence from the source."
            + " After fragment size reaches or exceeds the given size the app writes to the target dictionary."
            + " Defaults one thousand (1000) characters per fragment including whitespaces and punctuation marks.")
    private int estimatedFragmentSize = DEFAULT_ESTIMATED_FRAGMENT_SIZE;

    @Parameter(names = "--unidecode", description = "Determines whether or not text should be converted "
            + " to fit into ASCII character set. Defaults to true")
    private boolean shouldUnidecode = true;

    @Parameter(names = "--sentence_model", description = "Path to the OpenNLP 1.5 series model file to use"
            + " for splitting source text into sentences. Defaults to built-in english sentence detector model.")
    private String modelResourcePath = ENGLISH_MODEL;

    public static void main(String... args) throws IOException {
        Splitter splitter = new Splitter();
        JCommander.newBuilder()
                .addObject(splitter)
                .defaultProvider(new DefaultSourceTargetProvider(args))
                .acceptUnknownOptions(true)
                .build()
                .parse(args);

        splitter.splitToDictionary();
    }

    private void splitToDictionary() throws IOException {
        try (InputStream modelInput = openModelStream();
             FileReader inReader = new FileReader(source);
             Scanner in = new Scanner(inReader);
             PrintWriter out = new PrintWriter(target)) {
            SentenceModel model = new SentenceModel(modelInput);
            SentenceDetectorME detector = new SentenceDetectorME(model);
            String text = in.useDelimiter("\\Z")
                    .next();
            StringBuilder fragment = new StringBuilder(estimatedFragmentSize);

            // Substitute all whitespace character sequences (including line feeds and carriage returns
            // with a single space character.
            text = text.replaceAll("\\s+", " ");

            if (shouldUnidecode) {
                text = Unidecode.toAscii()
                        .decode(text);
            }
            for (Span sentenceInfo : detector.sentPosDetect(text)) {
                fragment.append(text, sentenceInfo.getStart(), sentenceInfo.getEnd())
                        .append(' ');
                // Add one character to account for trailing space that will be removed.
                if (fragment.length() >= estimatedFragmentSize + 1) {
                    fragment.deleteCharAt(fragment.length() - 1);
                    fragment.append("\r\n");
                    out.println(fragment);
                    fragment.setLength(0);
                }
            }
        }
    }

    private InputStream openModelStream() throws IOException {
        InputStream modelInput = Splitter.class.getResourceAsStream(modelResourcePath);
        return modelInput == null ? new FileInputStream(modelResourcePath) : modelInput;
    }

    @RequiredArgsConstructor
    private static class DefaultSourceTargetProvider implements IDefaultProvider {
        private final String[] arguments;

        @Override
        public String getDefaultValueFor(String optionName) {
            return isOption(arguments[0]) ? null : DEFAULT_PROVIDERS
                    .getOrDefault(optionName, args -> null).apply(arguments[0]);
        }

        private static boolean isOption(String value) {
            return value.startsWith("--");
        }
    }
}
