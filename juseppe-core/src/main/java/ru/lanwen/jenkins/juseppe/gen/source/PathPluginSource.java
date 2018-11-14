package ru.lanwen.jenkins.juseppe.gen.source;

import org.slf4j.*;
import ru.lanwen.jenkins.juseppe.beans.*;
import ru.lanwen.jenkins.juseppe.gen.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.String.*;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class PathPluginSource implements PluginSource {

    private static final Logger  LOG = LoggerFactory.getLogger(PathPluginSource.class);
    private final        Path    pluginsDir;
    private final        boolean recursiveWatch;

    public PathPluginSource(Path pluginsDir, boolean recursiveWatch) {
        this.pluginsDir = pluginsDir;
        this.recursiveWatch = recursiveWatch;
    }

    @Override
    public List<Plugin> plugins() {
        try (Stream<Path> paths = (recursiveWatch) ? Files.walk(pluginsDir) : Files.list(pluginsDir)) {
            List<Path> l = paths
                    .filter(path -> path.toString().endsWith(".hpi") || path.toString().endsWith(".jpi"))
                    .collect(Collectors.toList());
            return l.stream()
                    .parallel()
                    .map(path -> {
                        try {
                            LOG.trace("Process file {}", path);
                            return HPI.loadHPI(path.toFile())
                                    .withUrl(pluginsDir.relativize(path).toString());
                        } catch (Exception e) {
                            LOG.error("Fail to get the {} info", path.toAbsolutePath(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .peek(new Progressor(".... processed {} of " + l.size() + " plugins"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(format("Can't read path %s", pluginsDir.toAbsolutePath()), e);
        }
    }

    private static class Progressor implements Consumer<Plugin> {
        private static final long   INTERVAL = 1000;
        private final        String msg;
        private              int    count;
        private              long   t;

        private Progressor(String msg) {
            this.msg = msg;
            t = System.currentTimeMillis() + INTERVAL;
        }

        @Override
        public void accept(Plugin plugin) {
            synchronized (this) {
                count++;
                if (t < System.currentTimeMillis()) {
                    t = System.currentTimeMillis() + INTERVAL;
                    LOG.info(msg, count);
                }
            }
        }
    }
}
