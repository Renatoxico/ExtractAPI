package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.FileValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import java.util.List;

@Service
public class ValidationService {
    private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT);
    private final long maxFileBytes;
    private final Clock clock;

    public ValidationService() {
        this(Clock.systemDefaultZone(), 1024 * 1024);
    }

    @Autowired
    public ValidationService(MultipartProperties multipart) {
        this(Clock.systemDefaultZone(), multipart.getMaxFileSize().toBytes());
    }

    ValidationService(Clock clock, long maxFileBytes) {
        this.clock = clock;
        this.maxFileBytes = maxFileBytes;
    }

    public FileValidationResult validateFiles (MultipartFile[] files) {
        if (files.length < 1) {
            LOG.warn("File validation failed: No files provided");
            return new FileValidationResult(false, "No files found", "NO_FILES_PROVIDED", HttpStatus.BAD_REQUEST);
        }
        if (files.length > 6) {
            LOG.warn("File validation failed: Too many files ({})", files.length);
            return new FileValidationResult(false, "Too many files (maximum 6 allowed)", "TOO_MANY_FILES", HttpStatus.BAD_REQUEST);
        }

        for (MultipartFile file : files) {
            long fileSizeKB = file.getSize() / 1024;
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();

            LOG.info("Validating file: {} ({}KB, type: {})", fileName, fileSizeKB, contentType);

            boolean isValidType = "application/pdf".equals(contentType);
            boolean hasValidExtension = fileName != null && (fileName.toLowerCase().endsWith(".pdf"));

            if (file.getSize() > maxFileBytes) {
                LOG.warn("File {} is too large: {}KB (max: {}KB)", fileName, fileSizeKB, maxFileBytes / 1024);
                return new FileValidationResult(false,
                    "File '" + fileName + "' is too large: " + fileSizeKB + "KB (maximum " + maxFileBytes / 1024 + "KB)",
                    "FILE_TOO_BIG",
                    HttpStatus.BAD_REQUEST);
            }

            if (!isValidType || !hasValidExtension) {
                LOG.warn("File {} has invalid type or extension", fileName);
                return new FileValidationResult(false,
                    "File '" + fileName + "' is not a valid PDF file",
                    "INVALID_FILE_TYPE",
                    HttpStatus.BAD_REQUEST);
            }
        }
        LOG.info("All {} files passed validation", files.length);
        return new FileValidationResult(true, "All files validated successfully", "OK", HttpStatus.OK);
    }

    public List<Expense> validateExpenses (List<Expense> expenses) {
        expenses.removeIf(expense -> expense.getAmount() == null || expense.getAmount().signum() == 0
            || expense.getExpenseName() == null || expense.getExpenseName().isBlank());
        expenses.forEach(expense -> {
            expense.setExpenseName(expense.getExpenseName().trim());
            preClassifyExpenses(expense);
        });
        setLongDate(expenses);
        return expenses;
    }

    private Expense preClassifyExpenses (Expense expense) {
            String expName = expense.getExpenseName().toUpperCase();
            if(expName.contains("UBER") || expName.contains("CLICKBUS")){
                expense.setCategory("Transporte / Auto");
            }
            else if (expName.contains("IFOOD") || expName.contains("IFD")) {
                expense.setCategory("Restaurante / Lanches");
            }
            else if (expName.contains("SERV BEM") || expName.contains("CARREFOUR")
                    || expName.contains("JAU SERVE")){
                expense.setCategory("Supermercado");
            }
            else if (expName.contains("NETFLIX") || expName.contains("HBOMAX")
                    || expName.contains("AMAZONPRIME") || expName.contains("PATREON")
                    || expName.contains("PAG*STEAM") || expName.contains("PET SHOP") || expName.contains("PETSHOP")
                    || expName.contains("KICKSTREAMING") || expName.contains("YOUTUBE")
                    || expName.contains("MELIMAIS") || expName.contains("DISNEY")) {
                expense.setCategory("Lazer / Entretenimento / Pets");
            }
            else if (expName.contains("AMAZON") || expName.contains("MERCADOLIVRE")
                    || expName.contains("MERCADOPAGO") || expName.contains("KABUM")
                    || expName.contains("HAVAN")){
                expense.setCategory("E-commerce / Compras online");
            }
            else if (expName.contains("PAGAMENTO DE BOLETO") || expName.contains("TARIFA MENSALIDADE")
                    || expName.contains("PGTO CONTA") || expName.contains("MENSALIDADE DE SEGURO")
                    || expName.contains("SEGURO VIDA") || expName.contains("SEGUROVIDA")
                    || expName.contains("APPLECOM/BILL")){
                expense.setCategory("Moradia / Contas");
            }
            else if (expName.contains("HOSTGATOR")) {
                expense.setCategory("Investimentos / Assinaturas profissionais");
            }
            else if (expName.contains("RENNER") || expName.contains("HERING")
                    || expName.contains("NIKEECOMMER")){
                expense.setCategory("Roupas / Acessórios");
            }
            else if (expName.contains("SAUDE") || expName.contains("FARMACIA")){
                expense.setCategory("Saúde / Farmácia / Bem-estar");
            }
            else if (//expName.contains("PIX ") || expName.contains("PIX*") || expName.contains("PIX-")|| 
                    expName.contains("TRANSFERENCIA") || expName.contains("TRANSFERÊNCIA")
                    || expName.contains("TED ") || expName.contains("TED*") || expName.contains("TED-")
                    || expName.contains("DOC ") || expName.contains("DOC*") || expName.contains("DOC-")){
                expense.setCategory("Outros / Transferências");
            }
            return expense;
    }

    private void setLongDate(List<Expense> expenses) {
        LocalDate today = LocalDate.now(clock);
        // Explicit transaction dates anchor historical uploads. No statement-period inference yet.
        LocalDate explicitAnchor = expenses.stream().map(Expense::getDate).map(this::parseFullDate)
            .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        LocalDate anchor = explicitAnchor == null ? today : explicitAnchor;
        expenses.removeIf(expense -> {
            String value = expense.getDate();
            LocalDate parsed = parseFullDate(value);
            if (value != null && value.matches("\\d{2}/\\d{2}")) {
                try {
                    int month = Integer.parseInt(value.substring(3));
                    int year = anchor.getYear();
                    if (explicitAnchor == null) {
                        java.time.MonthDay day = java.time.MonthDay.of(month, Integer.parseInt(value.substring(0, 2)));
                        if (day.isAfter(java.time.MonthDay.from(today))) year--;
                    } else {
                        if (month - anchor.getMonthValue() > 6) year--;
                        if (anchor.getMonthValue() - month > 6) year++;
                    }
                    parsed = LocalDate.parse(value + "/" + year, DATE);
                } catch (DateTimeException ex) {
                    return true;
                }
            }
            if (parsed == null) return true;
            expense.setDate(DATE.format(parsed));
            return false;
        });
    }

    private LocalDate parseFullDate(String value) {
        if (value == null || !value.matches("\\d{2}/\\d{2}/\\d{4}")) return null;
        try {
            return LocalDate.parse(value, DATE);
        } catch (DateTimeException ex) {
            return null;
        }
    }
}
