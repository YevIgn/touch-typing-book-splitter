package touch.typing.book.splitter;

import org.junit.Test;

import java.net.URL;
import java.nio.file.Paths;

// Not a real test, exists to manually check splitter correctness.
public class SplitterDemo {
    @Test
    public void fileNamePassedAsFirstArgument() throws Exception {
        URL resource = SplitterDemo.class.getResource("/sample");
        String path = Paths.get(resource.toURI()).toFile().getCanonicalPath();

        Splitter.main(path);
    }
}
