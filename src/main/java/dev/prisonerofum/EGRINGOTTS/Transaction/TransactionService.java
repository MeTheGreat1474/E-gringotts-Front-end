package dev.prisonerofum.EGRINGOTTS.Transaction;


import dev.prisonerofum.EGRINGOTTS.Account.Account;
import dev.prisonerofum.EGRINGOTTS.Account.AccountRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Date;
import java.util.Optional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired                                  // initialized the EGringottsRepository
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;

    // create new transactions
    public String makeNewTransaction(String senderId, String receiverId, double amount, TransactionCategory category, String transactionType, String remarks) {
        Optional<Account> senderOpt = accountRepository.findById(senderId);
        Optional<Account> receiverOpt = accountRepository.findById(receiverId);

        if (!senderOpt.isPresent() || !receiverOpt.isPresent()) {
            return "Sender or receiver account not found.";
        }

        Account sender = senderOpt.get();
        Account receiver = receiverOpt.get();

        if (sender.getBalance() < amount) {
            return "Insufficient balance. " + sender;
        }

        sender.setBalance(sender.getBalance() - amount);
        receiver.setBalance(receiver.getBalance() + amount);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction transaction = new Transaction();
        transaction.setUserID(senderId);
        transaction.setReceiverID(receiverId);
        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setTransactionType(transactionType);
        transaction.setRemarks(remarks);
        transaction.setDate(new Date());

        transactionRepository.save(transaction);

        // Generate receipt
        String receipt = generateReceipt(transaction);

        return "Transaction successful. Receipt: " + receipt;
    }

    // getTransactionHistory method
    public List<Transaction> getTransactionsHistory(String userID) {
        return transactionRepository.findByUserID(userID);
    }


    // filter method for date in specific range
    public List<Transaction> getTransactionsByDateRange(Date startDate, Date endDate) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate);
    }

    // filter method according to amount threshold
    public List<Transaction> getTransactionsByAmountRange(double minAmount, double maxAmount) {
        return transactionRepository.findTransactionsByAmountRange(minAmount, maxAmount);
    }

    // filter  method according to category
    public List<Transaction> getTransactionsByCategory(TransactionCategory category) {
        return transactionRepository.findByCategory(category);
    }

    // generate receipt method
    public String generateReceipt(Transaction transaction) {
        // Get transaction details
        String transactionId = transaction.getTransactionID();
        String transactionDate = transaction.getTransactionDate();
        String senderUserId = transaction.getUserID();
        String recipientUserId = transaction.getReceiverID();
        double amount = transaction.getAmount();

        // Get sender and recipient information
        Account sender = accountRepository.findById(senderUserId).orElse(null);
        Account recipient = accountRepository.findById(recipientUserId).orElse(null);

//        // Format transaction date
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String formattedDate = dateFormat.format(transactionDate);

        // Generate receipt
        StringBuilder receiptBuilder = new StringBuilder();
        receiptBuilder.append("Transaction ID: ").append(transactionId).append("\n");
        receiptBuilder.append("Date: ").append(transactionDate).append("\n");
        receiptBuilder.append("Sender: ").append(sender != null ? sender.getUsername() : "Unknown").append("\n");
        receiptBuilder.append("Recipient: ").append(recipient != null ? recipient.getUsername() : "Unknown").append("\n");
        receiptBuilder.append("Amount: ").append(amount).append("\n");
        receiptBuilder.append("Thank you for using E-Gringotts! Your magical transfer has been successfully completed.\n\n" +
                "For any inquiries or further assistance, owl us at support@egringotts.com\n\n" +
                "May your galleons multiply like Fizzing Whizbees!");

        return receiptBuilder.toString();
    }

    public Transaction getTransactionById(String transactionId) {
        return transactionRepository.findById(new ObjectId(transactionId)).orElse(null);
    }

    public Map<TransactionCategory, Map<String, Double>> calculateCategoryPercentages(List<Transaction> transactions) {
        double totalExpenditure = transactions.stream().mapToDouble(Transaction::getAmount).sum();
        Map<TransactionCategory, Double> categorySums = new HashMap<>();
        for (Transaction transaction : transactions) {
            categorySums.merge(transaction.getCategory(), (double) transaction.getAmount(), Double::sum);
        }
        Map<TransactionCategory, Map<String, Double>> categoryData = new HashMap<>();

        for (Map.Entry<TransactionCategory, Double> entry : categorySums.entrySet()) {
            Map<String, Double> data = new HashMap<>();
            data.put("percentage", (entry.getValue() / totalExpenditure) * 100);
            data.put("totalExpenditure", entry.getValue());
            categoryData.put(entry.getKey(), data);
        }

        return categoryData;
    }

    public Map<String, Map<TransactionCategory, Map<String, Double>>> calculateCategoryPercentagesByFrequency(List<Transaction> transactions, String frequency) {
        Map<String, List<Transaction>> groupedTransactions = new HashMap<>();
        SimpleDateFormat sdf = frequency.equals("Daily") ? new SimpleDateFormat("yyyy-MM-dd") : new SimpleDateFormat("yyyy-MM");

        for (Transaction transaction : transactions) {
            String key = sdf.format(transaction.getDate());
            groupedTransactions.computeIfAbsent(key, k -> new ArrayList<>()).add(transaction);
        }

        Map<String, Map<TransactionCategory, Map<String, Double>>> categoryPercentagesByFrequency = new HashMap<>();

        for (Map.Entry<String, List<Transaction>> entry : groupedTransactions.entrySet()) {
            String key = entry.getKey();
            List<Transaction> groupedTransactionList = entry.getValue();
            categoryPercentagesByFrequency.put(key, calculateCategoryPercentages(groupedTransactionList));
        }

        return categoryPercentagesByFrequency;
    }

    public List<Transaction> filterTransactions(List<Transaction> transactions, Date startDate, Date endDate, Set<String> paymentMethods) {
        return transactions.stream()
                .filter(t -> !t.getDate().before(startDate) && !t.getDate().after(endDate))
                .filter(t -> paymentMethods.contains(t.getTransactionType()))
                .collect(Collectors.toList());
    }

}