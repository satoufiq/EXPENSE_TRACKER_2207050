package org.example.service;

import org.example.model.Expense;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service
 * Provides personal-mode analytics aggregations and AI-powered suggestions.
 */
public class AnalyticsService {

    public static class PersonalAnalyticsSummary {
        public double monthlyTotal;
        public double weeklyTotal;
        public LocalDate highestSpendingDay;
        public double highestSpendingAmount;
        public Map<String, Double> categoryTotals;
        public List<String> suggestions;
    }

    /**
     * Build analytics summary for a user's personal expenses.
     */
    public static PersonalAnalyticsSummary buildPersonalSummary(String userId) {
        List<Expense> expenses = ExpenseService.getPersonalExpensesObservable(userId);
        LocalDate now = LocalDate.now();
        WeekFields wf = WeekFields.of(Locale.getDefault());

        double monthly = 0.0;
        double weekly = 0.0;
        Map<LocalDate, Double> byDay = new HashMap<>();
        Map<String, Double> categoryTotals = new HashMap<>();
        Map<DayOfWeek, Double> byDayOfWeek = new HashMap<>();
        List<Expense> last30Days = new ArrayList<>();

        for (Expense e : expenses) {
            LocalDate d;
            try {
                d = LocalDate.parse(e.getDate());
            } catch (Exception ex) {
                continue;
            }

            // Monthly total
            if (d.getYear() == now.getYear() && d.getMonth() == now.getMonth()) {
                monthly += e.getAmount();
            }

            // Weekly total
            if (d.get(wf.weekOfYear()) == now.get(wf.weekOfYear()) && d.getYear() == now.getYear()) {
                weekly += e.getAmount();
            }

            // Daily totals
            byDay.merge(d, e.getAmount(), Double::sum);

            // Category totals
            categoryTotals.merge(Optional.ofNullable(e.getCategory()).orElse("Uncategorized"),
                e.getAmount(), Double::sum);

            // Day of week patterns
            byDayOfWeek.merge(d.getDayOfWeek(), e.getAmount(), Double::sum);

            // Last 30 days
            if (!d.isBefore(now.minusDays(30))) {
                last30Days.add(e);
            }
        }

        // Find highest spending day
        LocalDate maxDay = null;
        double maxAmt = 0.0;
        for (Map.Entry<LocalDate, Double> en : byDay.entrySet()) {
            if (en.getValue() > maxAmt) {
                maxAmt = en.getValue();
                maxDay = en.getKey();
            }
        }

        PersonalAnalyticsSummary s = new PersonalAnalyticsSummary();
        s.monthlyTotal = monthly;
        s.weeklyTotal = weekly;
        s.highestSpendingDay = maxDay;
        s.highestSpendingAmount = maxAmt;
        s.categoryTotals = categoryTotals.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, LinkedHashMap::new));
        s.suggestions = buildSmartSuggestions(s, userId, byDayOfWeek, last30Days, now);
        return s;
    }

    private static List<String> buildSmartSuggestions(PersonalAnalyticsSummary s, String userId,
                                                      Map<DayOfWeek, Double> byDayOfWeek,
                                                      List<Expense> last30Days, LocalDate now) {
        List<String> tips = new ArrayList<>();

        // Budget-related suggestions
        double budget = BudgetService.getMonthlyBudget(userId);
        if (budget > 0) {
            double percentage = (s.monthlyTotal / budget) * 100;
            int daysInMonth = now.lengthOfMonth();
            int dayOfMonth = now.getDayOfMonth();
            double expectedPercentage = ((double) dayOfMonth / daysInMonth) * 100;

            if (percentage > expectedPercentage + 15) {
                tips.add("⚠️ Spending Alert: You're spending faster than expected. " +
                        String.format("%.0f%% of budget used in %.0f%% of the month.", percentage, expectedPercentage));
            } else if (percentage < expectedPercentage - 10) {
                tips.add("✅ Great job! You're spending less than expected and staying within budget.");
            }

            double remaining = budget - s.monthlyTotal;
            if (remaining > 0) {
                int daysLeft = daysInMonth - dayOfMonth;
                double dailyAvailable = remaining / Math.max(daysLeft, 1);
                tips.add(String.format("💰 Budget remaining: ৳%.2f. You can spend ৳%.2f per day.",
                    remaining, dailyAvailable));
            } else {
                tips.add(String.format("🚨 Budget exceeded by ৳%.2f. Consider reducing spending.",
                    Math.abs(remaining)));
            }
        } else {
            if (s.monthlyTotal > 0) {
                tips.add("💡 Tip: Set a monthly budget to track your spending better.");
            }
        }

        // Category analysis
        if (!s.categoryTotals.isEmpty()) {
            Map.Entry<String, Double> top = s.categoryTotals.entrySet().iterator().next();
            double topPercentage = (top.getValue() / s.monthlyTotal) * 100;

            tips.add(String.format("📊 Top category: %s (৳%.2f, %.0f%% of spending). " +
                "Review if this seems high.", top.getKey(), top.getValue(), topPercentage));

            // Check if one category dominates
            if (topPercentage > 50) {
                tips.add("⚡ One category dominates your spending. Consider diversifying or reducing costs here.");
            }
        }

        // Spending trend prediction
        if (last30Days.size() > 0) {
            double avg30Days = last30Days.stream()
                .mapToDouble(Expense::getAmount)
                .average()
                .orElse(0);

            double currentMonthAvg = s.monthlyTotal / now.getDayOfMonth();

            if (currentMonthAvg > avg30Days * 1.2) {
                tips.add(String.format("📈 Spending trend increasing! Current daily avg: ৳%.2f vs 30-day avg: ৳%.2f",
                    currentMonthAvg, avg30Days));
            } else if (currentMonthAvg < avg30Days * 0.8) {
                tips.add(String.format("📉 Good news! Spending trend decreasing. Current daily avg: ৳%.2f vs 30-day avg: ৳%.2f",
                    currentMonthAvg, avg30Days));
            }

            // Predict month-end spending
            double predictedMonthEnd = currentMonthAvg * now.lengthOfMonth();
            tips.add(String.format("🔮 Prediction: At current rate, you'll spend ৳%.2f by month-end.",
                predictedMonthEnd));
        }

        // Day of week patterns
        if (!byDayOfWeek.isEmpty()) {
            DayOfWeek mostExpensiveDay = byDayOfWeek.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

            if (mostExpensiveDay != null) {
                tips.add(String.format("📅 Pattern detected: You spend most on %ss. Plan ahead!",
                    mostExpensiveDay.toString()));
            }
        }

        // Peak spending alert
        if (s.highestSpendingDay != null && s.highestSpendingAmount > 0) {
            if (s.highestSpendingAmount > s.monthlyTotal * 0.2) {
                tips.add(String.format("🔥 Single-day spike: ৳%.2f on %s. This was %.0f%% of your monthly spending!",
                    s.highestSpendingAmount, s.highestSpendingDay,
                    (s.highestSpendingAmount / s.monthlyTotal) * 100));
            }
        }

        // Weekly comparison
        if (s.weeklyTotal > 0 && s.monthlyTotal > 0) {
            double weeklyProjection = s.weeklyTotal * 4.3; // ~4.3 weeks per month
            if (weeklyProjection > s.monthlyTotal * 1.2) {
                tips.add("⚠️ This week's spending is higher than usual. Consider slowing down.");
            }
        }

        // General tips
        if (tips.size() < 3) {
            tips.add("💪 Financial Tip: Track daily expenses to identify unnecessary spending.");
            tips.add("🎯 Strategy: Use the 50/30/20 rule - 50% needs, 30% wants, 20% savings.");
            tips.add("📱 Smart Move: Review your subscriptions and cancel unused services.");
        }

        return tips;
    }
}
