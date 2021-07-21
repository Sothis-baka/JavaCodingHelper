package org.intellij.sdk.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class SelectToChange extends AnAction {
    public SelectToChange(){
        super();
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        // Get all the required data from data keys
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        final Document document = editor.getDocument();

        // Work off of the primary caret to get the selection info
        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int start = primaryCaret.getSelectionStart();
        int end = primaryCaret.getSelectionEnd();
        String temp = document.getText().substring(start, end);
        try {
            String result = postIt(temp);

            // Replace the selection with a fixed string.
            // Must do this document change in a write action context.
            WriteCommandAction.runWriteCommandAction(project, () ->
                    document.replaceString(start, end, result)
            );
        } catch (IOException ioException) {
            document.replaceString(start, end, "ioex" + ioException.toString());
            ioException.printStackTrace();
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
            document.replaceString(start, end, "in" + interruptedException.toString());
        }

        // De-select the text range that was just replaced
        primaryCaret.removeSelection();
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        // Get required data keys
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        // Set visibility only in case of existing project and editor and if a selection exists
        e.getPresentation().setEnabledAndVisible( project != null
                && editor != null
                && editor.getSelectionModel().hasSelection() );
    }

    private static String postIt (String input) throws IOException, InterruptedException {
        System.out.println("posting...");
        input = input.trim();
        if(input.length() == 0) {
            return "Selected area is empty";
        }
        String finalInput = input;

        var values = new HashMap<String, String>() {{
            put("userInput", finalInput);
        }};
        var objectMapper = new ObjectMapper();
        String requestBody = objectMapper
                .writeValueAsString(values);
        System.out.println(requestBody);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://powerful-brook-17823.herokuapp.com/text/onlyOnePart?userInput=" + finalInput))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
}
