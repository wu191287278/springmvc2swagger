import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

/**
 * @author yu.wu
 */
public class SpringMvc2SwaggerAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        String title = "Generate swagger doc";

        try {
            InputDialogWrapper inputDialogWrapper = new InputDialogWrapper(project);
            inputDialogWrapper.setTitle(title);
            inputDialogWrapper.show();
        } catch (Exception e) {
            Messages.showErrorDialog(e.toString(), title);
        }
    }
}
