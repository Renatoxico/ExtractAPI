<script>
// @ts-nocheck

    export let expenses = null;
    let searchTerm = '';
    $: filteredExpenses = expenses.filter(expense => {
        return expense.expenseName.toLowerCase().includes(searchTerm.toLowerCase());
    });
</script>

<style>
    .expense-list {
        background-color: #1e1e1e;
        padding: 20px;
        border-radius: 10px;
        border: 1px solid #76c893;
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
    }

    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
    }

    h2 {
        color: #76c893;
        margin: 0;
    }

    .search-input {
        background-color: #2a2a2a;
        border: 1px solid #76c893;
        border-radius: 5px;
        padding: 10px;
        color: #e0e0e0;
        font-size: 1rem;
        width: 50%;
        transition: border-color 0.3s ease;
    }

    .search-input::placeholder {
        color: #a0a0a0;
    }

    .search-input:focus {
        outline: none;
        border-color: #5aa678;
    }

    table {
        width: 100%;
        border-collapse: collapse;
        margin-top: 10px;
    }

    thead {
        background-color: #2a2a2a;
    }

    th {
        color: #76c893;
        text-align: left;
        padding: 10px;
        border-bottom: 2px solid #76c893;
    }

    tbody tr {
        background-color: #1e1e1e;
        transition: background-color 0.3s ease;
    }

    tbody tr:nth-child(even) {
        background-color: #2a2a2a;
    }

    tbody tr:hover {
        background-color: #333333;
    }

    td {
        color: #e0e0e0;
        padding: 10px;
        border-bottom: 1px solid #2a2a2a;
    }

    td:last-child {
        color: #76c893;
        font-weight: bold;
    }
</style>

<div class="expense-list">
    <div class="header">
        <h2>Todas Despesas</h2>
        <input
            type="text"
            class="search-input"
            bind:value={searchTerm}
            placeholder="Pesquisar Despesas"
        />
    </div>
    <table>
        <thead>
            <tr>
                <th>Data</th>
                <th>Nome</th>
                <th>Valor</th>
            </tr>
        </thead>
        <tbody>
            {#each filteredExpenses as expense}
                <tr>
                    <td>{expense.date}</td>
                    <td>{expense.expenseName}</td>
                    <td>R${expense.value.toFixed(2)}</td>
                </tr>
            {/each}
        </tbody>
    </table>
</div>