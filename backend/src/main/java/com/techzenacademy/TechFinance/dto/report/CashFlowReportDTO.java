package com.techzenacademy.TechFinance.dto.report;

import java.math.BigDecimal;
import java.util.List;

public class CashFlowReportDTO {
    private Integer year;
    private SummaryDTO summary;
    private List<MonthlyDataDTO> monthlyData;

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public SummaryDTO getSummary() {
        return summary;
    }

    public void setSummary(SummaryDTO summary) {
        this.summary = summary;
    }

    public List<MonthlyDataDTO> getMonthlyData() {
        return monthlyData;
    }

    public void setMonthlyData(List<MonthlyDataDTO> monthlyData) {
        this.monthlyData = monthlyData;
    }

    public static class SummaryDTO {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal totalProfit;
        private BigDecimal endBalance;

        public BigDecimal getTotalIncome() {
            return totalIncome;
        }

        public void setTotalIncome(BigDecimal totalIncome) {
            this.totalIncome = totalIncome;
        }

        public BigDecimal getTotalExpense() {
            return totalExpense;
        }

        public void setTotalExpense(BigDecimal totalExpense) {
            this.totalExpense = totalExpense;
        }

        public BigDecimal getTotalProfit() {
            return totalProfit;
        }

        public void setTotalProfit(BigDecimal totalProfit) {
            this.totalProfit = totalProfit;
        }

        public BigDecimal getEndBalance() {
            return endBalance;
        }

        public void setEndBalance(BigDecimal endBalance) {
            this.endBalance = endBalance;
        }
    }

    public static class MonthlyDataDTO {
        private Integer month;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal profit;
        private BigDecimal balance;

        public Integer getMonth() {
            return month;
        }

        public void setMonth(Integer month) {
            this.month = month;
        }

        public BigDecimal getIncome() {
            return income;
        }

        public void setIncome(BigDecimal income) {
            this.income = income;
        }

        public BigDecimal getExpense() {
            return expense;
        }

        public void setExpense(BigDecimal expense) {
            this.expense = expense;
        }

        public BigDecimal getProfit() {
            return profit;
        }

        public void setProfit(BigDecimal profit) {
            this.profit = profit;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }
    }
} 