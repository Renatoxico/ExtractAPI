<script>
// @ts-nocheck

    export let sessionToken = null; 
    export let smartList = null;
    export let topExpenses = null;
    export let expenses = null;
    export let biggestExpense = null;

    import GroupedExpenseItem from '../components/GroupedExpenseItem.svelte';
    import ExpenseItem from '../components/ExpenseItem.svelte';
	import ExpenseList from './ExpenseList.svelte';

</script>

<style>
    .row {
	display: flex;
	flex-direction: row;
	justify-content: space-between;
	flex-wrap: wrap;
  	margin-bottom: 1rem;
    }

    .col {
        display: flex;
        flex-direction: column;
        flex: 1;
        padding: 0 1rem;
        /* max-width: 400px; */
    }

    .grid-container {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 1rem;
    }

    .expense-report {
        background-color: #1e1e1e;
        border-radius: 10px;
        padding: 20px;
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
        max-width: 90%;
        margin-left: auto;
        margin-right: auto;
    }
</style>

<div class="expense-report container">
    <div class="expense-header row">
        <h2>Token de Sessão: {sessionToken}</h2>        
    </div>

    <div class="expense-body">
        <div class="row">
            <div class="col">                
                <h2>Maior Despesa Única</h2>
                <ExpenseItem
                    expenseName={biggestExpense[0].expenseName}
                    expenseAmount={biggestExpense[0].value}
                    expenseDate={biggestExpense[0].date}
                />
                <h2>Lista de Despesas Agrupadas</h2>
                {#each smartList as expense}
                    <GroupedExpenseItem
                    expenseName={expense["expenseName"]}
                    expenseAmount={expense["total"]}
                    instances={expense["instances"]}
                    />
                {/each}
            </div>
            <div class="col">
                <h2>Top 10 Despesas</h2>
                {#each topExpenses as ex}
                <GroupedExpenseItem
                    expenseName={ex["expenseName"]}
                    expenseAmount={ex["total"]}
                    instances={ex["instances"]}
                />
                {/each}
            </div>
        </div>
        <div >
            <ExpenseList expenses={expenses} />
        </div>
    </div>
</div>