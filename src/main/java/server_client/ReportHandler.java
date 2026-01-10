package server_client;

import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Optional;

public class ReportHandler {
    public static String showDateRangeDialog(String city) {
        if (city == null || city.trim().isEmpty()) {
            showAlert("Error", "Please enter or select a city before generating the report.");
            return null;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Report Period Selection");
        dialog.setHeaderText("Select dates for the historical report:\nCity: " + city);

        DatePicker startPicker = new DatePicker(LocalDate.now().minusDays(3));
        DatePicker endPicker = new DatePicker(LocalDate.now());

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20;");
        content.getChildren().addAll(
                new Label("Start datum (YYYY-MM-DD):"), startPicker,
                new Label("End datum (YYYY-MM-DD):"), endPicker
        );
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                LocalDate start = startPicker.getValue();
                LocalDate end = endPicker.getValue();

                if (start == null || end == null) return null;
                if (start.isAfter(end)) {
                    showAlert("Date error", "The start date cannot be after the end date.");
                    return null;
                }

                return "GET_REPORT " + city + " " + start + " " + end;
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public static void handleReportResponse(String fullResponse) {
        if (fullResponse == null || !fullResponse.startsWith("REPORT_LATEX:")) {
            if (fullResponse != null && fullResponse.startsWith("ERROR:")) {
                showAlert("Server error", fullResponse.replace("ERROR:", ""));
            }
            return;
        }

        String latexCode = fullResponse
                .replace("REPORT_LATEX:", "")
                .replace("###END###", "")
                .trim();

        String fileName = "Report_" + System.currentTimeMillis() + ".tex";
        File reportFile = new File(fileName);

        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writer.print(latexCode);
            System.out.println("[CLIENT] LaTeX file generated: " + reportFile.getAbsolutePath());

            openFile(reportFile);

        } catch (IOException e) {
            showAlert("Saving error", "Unable to save the .tex file: " + e.getMessage());
        }
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            System.err.println("The system cannot automatically open the file.");
        }
    }
}
