import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.github.wu191287278.springmvc2swagger.SwaggerDocs;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.ui.DialogWrapper;
import javax.swing.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author yu.wu
 */
public class InputDialogWrapper extends DialogWrapper {

    private final JLabel outputLabel = new JLabel("Output Directory: ");

    private final JTextField output = new JTextField();

    private final JLabel parsingLabel = new JLabel("Parsing: ");

    private final JLabel fileLabel = new JLabel();

    private final JButton submit = new JButton("Confirm");

    private final SwaggerDocs swaggerDocs = new SwaggerDocs();

    private final Project project;

    private final String basePath;

    private final String defaultOutPath;

    private static final String CACHE_OUTPUT_KEY = "springmvc2swagger.config.output";

    public InputDialogWrapper(Project project) {
        super(true);
        this.project = project;
        this.basePath = project.getBasePath();
        this.defaultOutPath = this.basePath + "/docs";
        init();
    }

    @Override
    protected JComponent createNorthPanel() {
        return null;
    }

    @Override
    protected JComponent createSouthPanel() {
        JPanel south = new JPanel();
        submit.setHorizontalAlignment(SwingConstants.CENTER);
        submit.setVerticalAlignment(SwingConstants.CENTER);
        south.add(submit);
        submit.addActionListener(e -> {
            output.setEnabled(false);
            submit.setEnabled(false);
            String outputPath = output.getText();

            if (StringUtils.isBlank(outputPath)) {
                output.setText(defaultOutPath);
            }
            PropertiesComponent.getInstance(project).setValue(CACHE_OUTPUT_KEY, output.getText());

            CompletableFuture.runAsync(() -> {
                parsingLabel.setText("Parsing: ");
                swaggerDocs.parseAndWrite(basePath,
                        getLibraries(),
                        output.getText(),
                        fileLabel::setText);
            }).whenComplete((unused, throwable) -> {
                output.setEnabled(true);
                submit.setEnabled(true);
                parsingLabel.setText("Parsing: ");
                if (throwable != null) {
                    fileLabel.setText(throwable.getMessage());
                } else {
                    fileLabel.setText("Finish");
                }
            });
        });
        return submit;
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel center = new JPanel();
        center.setLayout(new GridLayout(4, 10));

        output.setText(defaultOutPath);

        String outputPath = PropertiesComponent.getInstance(project).getValue(CACHE_OUTPUT_KEY);
        if (outputPath != null) {
            output.setText(outputPath);
        }

        center.add(outputLabel);
        center.add(output);

        parsingLabel.setText("Source: ");
        center.add(parsingLabel);

        fileLabel.setText(basePath);
        center.add(fileLabel);

        return center;
    }

    public List<String> getLibraries() {
        Set<String> libs = new HashSet<>();
        @NotNull Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            ModuleRootManager instance = ModuleRootManager.getInstance(module);
            for (OrderEntry orderEntry : instance.getOrderEntries()) {
                String[] urls = orderEntry.getUrls(OrderRootType.CLASSES);
                for (String url : urls) {
                    if (!url.endsWith(".jar!/")) {
                        continue;
                    }
                    url = url.replace("jar://", "")
                            .replace(".jar!/", ".jar");
                    libs.add(url);
                }
            }
        }
        return new ArrayList<>(libs);
    }
}
