package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.model.Expense;
import org.example.service.ExpenseService;

import java.time.LocalDate;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class EditExpenseController {

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextField amountField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextArea noteArea;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label titleLabel;

    private Expense expenseToEdit;

    @FXML
    public void initialize() {

        categoryComboBox.getItems().addAll(
            "🍔 Food",
            "🚗 Transport",
            "🛍️ Shopping",
            "🎬 Entertainment",
            "💡 Bills",
            "🏥 Healthcare",
            "📚 Education",
            "🏠 Housing",
            "💰 Savings",
            "📱 Technology",
            "✈️ Travel",
            "🎁 Gifts",
            "👔 Clothing",
            "🏋️ Fitness",
            "📝 Other"
        );

        setupValidation();
    }

    public void setExpense(Expense expense) {
        this.expenseToEdit = expense;

        if (expense != null) {

            String categoryToFind = expense.getCategory();
            for (String item : categoryComboBox.getItems()) {
                if (item.contains(categoryToFind)) {
                    categoryComboBox.setValue(item);
                    break;
                }
            }

            if (categoryComboBox.getValue() == null) {
                categoryComboBox.setValue("📝 Other");
            }

            amountField.setText(String.valueOf(expense.getAmount()));

            try {
                datePicker.setValue(LocalDate.parse(expense.getDate()));
            } catch (Exception e) {
                datePicker.setValue(LocalDate.now());
            }

            noteArea.setText(expense.getNote() != null && !expense.getNote().equals("No note") ? expense.getNote() : "");
        }
    }

    private void setupValidation() {

        amountField.textProperty().addListener((_, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d{0,2}")) {
                amountField.setText(oldValue);
            }
        });
    }

    @FXML
    private void handleSave() {
        if (validateInput()) {

            String categoryWithEmoji = categoryComboBox.getValue();
            String category = categoryWithEmoji.substring(categoryWithEmoji.indexOf(" ") + 1);

            double amount = Double.parseDouble(amountField.getText());
            String date = datePicker.getValue().toString();
            String note = noteArea.getText().trim();

            if (note.isEmpty()) {
                note = "No note";
            }

            boolean success = ExpenseService.updateExpense(
                expenseToEdit.getExpenseId(),
                category,
                amount,
                date,
                note
            );

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success",
                    "✅ Expense updated successfully!\n\nAmount: ৳" + String.format("%.2f", amount) +
                    "\nCategory: " + category +
                    "\nDate: " + date);
                closeDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                    "❌ Failed to update expense!\n\nPlease try again.");
            }
        }
    }

    @FXML
    private void handleCancel() {

        boolean hasChanges = false;

        if (expenseToEdit != null) {
            String currentCategory = categoryComboBox.getValue();
            if (currentCategory != null) {
                currentCategory = currentCategory.substring(currentCategory.indexOf(" ") + 1);
                hasChanges = !currentCategory.equals(expenseToEdit.getCategory());
            }

            try {
                double currentAmount = Double.parseDouble(amountField.getText());
                hasChanges = hasChanges || currentAmount != expenseToEdit.getAmount();
            } catch (NumberFormatException ignored) {}

            String currentNote = noteArea.getText().trim();
            String originalNote = expenseToEdit.getNote() != null && !expenseToEdit.getNote().equals("No note") ? expenseToEdit.getNote() : "";
            hasChanges = hasChanges || !currentNote.equals(originalNote);
        }

        if (hasChanges) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Cancel");
            confirmAlert.setHeaderText("Discard Changes?");
            confirmAlert.setContentText("You have unsaved changes. Are you sure you want to cancel?");

            var result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                closeDialog();
            }
        } else {
            closeDialog();
        }
    }

    private boolean validateInput() {

        if (categoryComboBox.getValue() == null || categoryComboBox.getValue().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                "⚠️ Please select a category!");
            categoryComboBox.requestFocus();
            return false;
        }

        if (amountField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                "⚠️ Please enter an amount!");
            amountField.requestFocus();
            return false;
        }

        try {
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "⚠️ Amount must be greater than 0!");
                amountField.requestFocus();
                return false;
            }
            if (amount > 1000000) {
                showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "⚠️ Amount seems too large! Please check.");
                amountField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                "⚠️ Please enter a valid amount!");
            amountField.requestFocus();
            return false;
        }

        if (datePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                "⚠️ Please select a date!");
            datePicker.requestFocus();
            return false;
        }

        if (datePicker.getValue().isAfter(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                "⚠️ Date cannot be in the future!");
            datePicker.requestFocus();
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}