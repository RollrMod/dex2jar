/*
 * Copyright (c) 2009-2021 Panxiaobo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.dex2jar.test;

import com.googlecode.d2j.node.DexClassNode;
import com.googlecode.d2j.node.DexMethodNode;
import com.googlecode.d2j.smali.Smali;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * auto create test case from d2j-error-zips/*-error.zip,
 * the error.zip is generated by BaksmaliBaseDexExceptionHandler
 *
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 */
public class D2jErrorZipsTest {
    private static FileSystemProvider zipFSP;

    @BeforeAll
    static void setup() {
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            String scheme = provider.getScheme();
            if (scheme.equals("zip") || scheme.equals("jar")) {
                zipFSP = provider;
                break;
            }
        }
        assertNotNull(zipFSP);
    }


    @ParameterizedTest
    @MethodSource("findZips")
    void test(Path zipPath) {
        Map<String, ?> env = new HashMap<>();
        try (FileSystem fs = zipFSP.newFileSystem(zipPath, env)) {
            List<Path> methods = Files.walk(fs.getPath("/"))
                    .filter(Files::isReadable)
                    .filter(Files::isRegularFile)
                    .filter(px -> {
                        String fn = px.getFileName().toString();
                        return fn.startsWith("m-") && fn.endsWith(".txt") || fn.equals("summary.txt");
                    })
                    .collect(Collectors.toList());

            for (Path m : methods) {
                processEachEntry(zipPath.getFileName().toString(), m);
            }
        } catch (Exception ex) {
            fail(ex);
        }
    }


    private static void processEachEntry(String zipFileName, Path zipEntry) throws IOException, IllegalAccessException {
        String smaliContent;
        if (zipEntry.getFileName().toString().equals("summary.txt")) {
            smaliContent = parseSmaliContentFromSummary(zipEntry);
        } else {
            smaliContent = parseSmaliContent(zipEntry);
        }
        if (smaliContent == null) {
            return;
        }

        DexClassNode classNode = Smali.smaliFile2Node(zipEntry.toString(), smaliContent);
        assertNotNull(classNode);

        if (classNode.methods.size() > 1) { // split into methods
            for (DexMethodNode m : classNode.methods) {
                DexClassNode sub = new DexClassNode(0, "Lx;", "Ly;", new String[0]);
                sub.methods.add(m);
                TestUtils.translateAndCheck(null, sub);
            }
        } else {
            TestUtils.translateAndCheck(null, classNode);
        }
    }

    public static Stream<Arguments> findZips() {
        URL url = D2jErrorZipsTest.class.getResource("/smalis/writeString.smali");
        assertNotNull(url);

        final String file = url.getFile();
        assertNotNull(file);

        final Path basePath = new File(file).toPath().getParent().getParent().resolve("d2j-error-zips");

        final Set<Path> files = new TreeSet<>();
        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().endsWith(".zip")) {
                        files.add(file);
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files.stream()
                .map(Arguments::of);
    }

    private static String parseSmaliContentFromSummary(Path zipEntry) throws IOException {
        List<String> lines = Files.readAllLines(zipEntry, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        boolean hasMethod = false;
        boolean inMethodContent = false;
        for (String ln : lines) {
            if (!inMethodContent) {
                if (ln.startsWith(".method")) {
                    // append here to keep the line number
                    sb.append(".class LTT;.super Ljava/lang/Object;");
                    sb.append(ln);
                    inMethodContent = true;
                    hasMethod = true;
                }
            } else {
                sb.append(ln);
                if (ln.startsWith(".end method")) {
                    inMethodContent = false;
                }
            }
            sb.append("\n");
        }
        if (!hasMethod) {
            return null;
        }
        return sb.toString();
    }

    private static String parseSmaliContent(Path m) throws IOException {
        List<String> lines = Files.readAllLines(m, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();

        boolean found = false;
        for (String ln : lines) {
            if (!found) {
                if (ln.startsWith(".method")) {
                    // append here to keep the line number
                    sb.append(".class LTT;.super Ljava/lang/Object;");
                    sb.append(ln).append("\n");
                    found = true;
                } else {
                    sb.append("\n");
                }
            } else {
                sb.append(ln).append("\n");
            }
        }

        return sb.toString();
    }
}
