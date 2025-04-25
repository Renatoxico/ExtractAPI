<script>
    export let expenseName;
    export let expenseAmount;
    export let expenseDate;
</script>

<style>
    .expense-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        background-color: #1e1e1e;
        padding: 15px;
        border-radius: 8px;
        margin-bottom: 10px;
        border: 1px solid #76c893;
        transition: transform 0.2s ease, box-shadow 0.2s ease;
    }

    .expense-item:hover {
        transform: scale(1.02);
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
    }

    .expense-icon {
        font-size: 24px;
        color: #76c893;
        margin-right: 15px;
    }

    .expense-details {
        flex-grow: 1;
    }

    .expense-title {
        font-size: 1.1rem;
        color: #e0e0e0;
        margin: 0;
    }

    .expense-date {
        font-size: 0.9rem;
        color: #a0a0a0;
        margin: 0;
    }

    .expense-amount {
        font-size: 1.1rem;
        color: #76c893;
        font-weight: bold;
    }
</style>

<div class="expense-item">
    <!-- <div class="expense-icon">$</div> -->
    <div class="expense-details">
        <p class="expense-title">{expenseName}</p>
        <p class="expense-date">{expenseDate}</p>
    </div>
    <div class="expense-amount">R${expenseAmount}</div>
</div>