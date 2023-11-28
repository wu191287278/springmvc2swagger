package com.github.wu191287278.springmvc2swagger;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import com.github.wu191287278.springmvc2swagger.visitor.JavaxRsVisitorAdapter;
import com.github.wu191287278.springmvc2swagger.visitor.RestVisitorAdapter;
import com.google.common.collect.ImmutableMap;
import io.swagger.models.*;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yu.wu
 */
public class SwaggerDocs {

    private Logger log = LoggerFactory.getLogger(SwaggerDocs.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private String title = "Api Documentation";

    private String description = "";

    private String version = "1.0.0";

    private String basePath = "/";

    private String host = "localhost";

    public SwaggerDocs() {
    }

    public SwaggerDocs(String title, String description, String version, String basePath, String host) {
        this.title = title;
        this.description = description;
        this.version = version;
        this.basePath = basePath;
        this.host = host;
    }

    public List<String> parseAndWrite(String sourceDirectory, List<String> libraries, String outDirectory) {
        return parseAndWrite(sourceDirectory, libraries, outDirectory, s -> {
        });
    }

    public List<String> parseAndWrite(String sourceDirectory, List<String> libraries, String outDirectory, Consumer<String> consumer) {
        List<String> paths = new ArrayList<>();
        Map<String, Swagger> m = parse(sourceDirectory, null, libraries, consumer);
        List<Map<String, String>> urls = new ArrayList<>();

        for (Map.Entry<String, Swagger> entry : m.entrySet()) {
            String path = outDirectory + "/" + entry.getKey() + ".json";
            writeTo(path, entry.getValue());
            paths.add(path);
        }
        File outDirectoryFile = new File(outDirectory);
        Collection<File> files = FileUtils.listFiles(outDirectoryFile, new String[]{"json"}, true);
        for (File swaggerFile : files) {
            try {
                JsonNode jsonNode = objectMapper.readValue(swaggerFile, JsonNode.class);
                boolean swagger = jsonNode.has("swagger");
                if (swagger) {
                    ImmutableMap<String, String> of = ImmutableMap.of("name", swaggerFile.getName().replace(".json", ""),
                            "url", "./" + swaggerFile.getAbsolutePath()
                                    .replace(outDirectoryFile.getAbsolutePath(), "")
                    );
                    urls.add(of);
                }
            } catch (Exception ignore) {

            }
        }
        writeUI(outDirectory, urls);
        return paths;
    }

    public void writeUI(String path, List<Map<String, String>> urls) {
        File file = new File(path);
        try {
            try (InputStream in = SwaggerDocs.class.getClassLoader().getResourceAsStream("static/swagger-ui.html");
                 BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath() + "/swagger-ui.html"))) {
                if (in == null) return;
                String html = IOUtils.toString(in, StandardCharsets.UTF_8);
                html = String.format(html, new ObjectMapper().writeValueAsString(urls));
                IOUtils.write(html, out);
            }

            File dist = new File(path, "dist.zip");
            try (InputStream in = SwaggerDocs.class.getClassLoader().getResourceAsStream("static/dist.zip");
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(dist))) {
                if (in != null) {
                    IOUtils.copy(in, out);
                }
            } catch (IOException e) {
            }
            try {
                File distDir = new File(path, "dist");
                distDir.mkdirs();
                unzip(dist, distDir);
            } catch (Exception e) {
            }
        } catch (IOException ignore) {

        }
    }


    public void writeTo(String path, Swagger swagger) {
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (file.exists()) {
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                SwaggerParser swaggerParser = new SwaggerParser();
                Swagger old = swaggerParser.parse(IOUtils.toString(in, StandardCharsets.UTF_8));
                if (old.getInfo() != null) {
                    swagger.setInfo(old.getInfo());
                }
                if (old.getBasePath() != null) {
                    swagger.setBasePath(old.getBasePath());
                }
                if (old.getHost() != null) {
                    swagger.setHost(old.getHost());
                }
                if (old.getSchemes() != null) {
                    swagger.setSchemes(old.getSchemes());
                }
                if (old.getSecurity() != null) {
                    swagger.setSecurity(old.getSecurity());
                }
                if (old.getSecurityDefinitions() != null) {
                    swagger.setSecurityDefinitions(old.getSecurityDefinitions());
                }

            } catch (Exception ignore) {
            }
        }
        try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swagger);
            out.write(json);
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Swagger> parse(String sourceDirectory, String basePackage, List<String> libraries, Consumer<String> consumer) {
        log.info("Parsing " + sourceDirectory);
        List<File> filteredDirectories = getSourceDirectories(sourceDirectory, basePackage);
        List<File> sourceDirectories = getSourceDirectories(sourceDirectory, basePackage);
        Map<String, Swagger> swaggerMap = new TreeMap<>();
        for (File filteredDirectory : filteredDirectories) {
            String projectPath = filteredDirectory.getAbsolutePath().replace("src/main/java", "")
                    .replace("src\\main\\java", "");
            CombinedTypeSolver typeSolver = new CombinedTypeSolver();
            for (File sourceFile : sourceDirectories) {
                typeSolver.add(new JavaParserTypeSolver(sourceFile));
            }

            try {
                typeSolver.add(new ReflectionTypeSolver(false));
                JarTypeSolver jarTypeSolver = null;
                for (String library : libraries) {
                    jarTypeSolver = JarTypeSolver.getJarTypeSolver(library);
                }
                if (jarTypeSolver != null) {
                    typeSolver.add(jarTypeSolver);
                }
            } catch (Exception e) {
                log.warn(e.getMessage());
            }


            final RestVisitorAdapter restVisitorAdapter = new RestVisitorAdapter(consumer)
                    .setCamel(true);
            final JavaxRsVisitorAdapter javaxRsVisitorAdapter = new JavaxRsVisitorAdapter();
            Info info = new Info()
                    .title(this.title)
                    .description(this.description)
                    .version(this.version);
            final Swagger swagger = new Swagger()
                    .info(info)
                    .paths(new TreeMap<>())
                    .schemes(Arrays.asList(Scheme.HTTP, Scheme.HTTPS))
                    .host(this.host)
                    .basePath(this.basePath)
                    .securityDefinition("api_key", new ApiKeyAuthDefinition("Authorization", In.HEADER));


            ParserConfiguration parserConfiguration = new ParserConfiguration();
            parserConfiguration.setSymbolResolver(new JavaSymbolSolver(typeSolver));

            SourceRoot sourceRoot = new SourceRoot(Paths.get(filteredDirectory.getAbsolutePath()), parserConfiguration);
            List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParseParallelized();

            for (ParseResult<CompilationUnit> parseResult : parseResults) {
                parseResult.ifSuccessful(r -> r.accept(javaxRsVisitorAdapter, swagger));
                parseResult.ifSuccessful(r -> r.accept(restVisitorAdapter, swagger));
            }
            for (Map.Entry<String, Model> entry : javaxRsVisitorAdapter.getModelMap().entrySet()) {
                swagger.model(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Model> entry : restVisitorAdapter.getModelMap().entrySet()) {
                swagger.model(entry.getKey(), entry.getValue());
            }

            if (swagger.getPaths() != null && !swagger.getPaths().isEmpty()) {
                String projectName = new File(projectPath).getName();
                swagger.getInfo().title(title);
                swagger.host(host);
                swagger.basePath(basePath);
                swaggerMap.put(projectName, swagger);
                for (Path path : swagger.getPaths().values()) {
                    for (Operation operation : path.getOperations()) {
                        List<String> tags = operation.getTags();
                        Map<String, List<String>> security = Stream.of("api_key")
                                .collect(Collectors.toMap(s -> s, s -> new ArrayList<>()));
                        operation.setSecurity(Collections.singletonList(security));
                    }
                }
            }

            if (swagger.getTags() != null) {
                Map<String, Tag> tagMap = swagger.getTags()
                        .stream()
                        .collect(Collectors.toMap(Tag::getName, t -> t));
                swagger.tags(new ArrayList<>(new TreeMap<>(tagMap).values()));
            }
        }

        return swaggerMap;
    }

    private List<File> getSourceDirectories(String sourceDirectory, String basePackage) {
        List<File> files = new ArrayList<>();
        filterSourceDirectory(sourceDirectory, basePackage == null ? "" : basePackage, files);
        return files;
    }

    private void filterSourceDirectory(String sourceDirectory, String basePackage, List<File> files) {
        File parentDirectoryFile = new File(sourceDirectory);
        if (!parentDirectoryFile.isDirectory()) return;

        File sourceDirectoryFile = new File(sourceDirectory, "/src/main/java/" + basePackage.replace(".", "/"));

        if (!sourceDirectoryFile.exists()) {
            File[] listFiles = parentDirectoryFile.listFiles();
            if (listFiles != null) {
                for (File file : listFiles) {
                    filterSourceDirectory(file.getAbsolutePath(), basePackage, files);
                }
            }
        } else {
            files.add(sourceDirectoryFile);
        }
    }

    public static void unzip(File zipFile, File outpath) {
        long startTime = System.currentTimeMillis();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));) {

            ZipEntry entry = zis.getNextEntry();
            FileOutputStream fos = null;
            while (entry != null) {
                File file = new File(outpath, entry.getName());
                System.out.println("正在解压缩: " + entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    //输出文件
                    fos = new FileOutputStream(file);
                    int len = 0;
                    byte[] butter = new byte[1024];
                    while ((len = zis.read(butter)) > 0) {
                        fos.write(butter, 0, len);
                    }
                }
                entry = zis.getNextEntry();
            }
            fos.close();

        } catch (Exception e) {
        }
    }


}


