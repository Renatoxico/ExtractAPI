<script>
// @ts-nocheck

    import { onMount } from 'svelte';
	import GroupedExpenseItem from '../components/GroupedExpenseItem.svelte';
    import ExpenseItem from '../components/ExpenseItem.svelte';

    import dados from '$lib/data/example.json';
	import ExpenseReport from '../components/ExpenseReport.svelte';

    let files = [];
    let errorMessage = '';
    let isLoading = false; // State to track loading status

    let sessionToken = null; 
    let smartList = null;
    let topExpenses = null;
    let expenses = null;
    let biggestExpense = null;

    function handleFileChange(event) {
        const selectedFiles = Array.from(event.target.files);
        const newFiles = [];

        for (const file of selectedFiles) {
            if (file.type !== 'application/pdf') {
                errorMessage = `Only PDF files are allowed.`;
                return;
            }

            if (file.size > 1024 * 1024) {
                errorMessage = `File "${file.name}" exceeds the 1MB size limit.`;
                return;
            }

            if (files.length + newFiles.length >= 4) {
                errorMessage = `You can only select up to 4 files.`;
                return;
            }

            newFiles.push(file);
        }

        files = [...files, ...newFiles];
        errorMessage = ''; // Clear error message if all validations pass
    }

    function removeFile(index) {
        files = files.filter((_, i) => i !== index);
    }

    async function handleSubmit(event) {
        event.preventDefault();
        if (files.length === 0) {
            errorMessage = 'Please select at least one file.';
            return;
        }

        const formData = new FormData();
        files.forEach((file) => {
            formData.append('file', file);
        });

        isLoading = true; // Show spinner
        try {
            const response = await fetch('http://localhost:9090/extract/', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error('Failed to upload files');
            } else {
                const data = await response.json();
                console.log('Files successfully uploaded:', data);
                sessionToken = data["sessionToken"]; 
                smartList = data["SmartGroupExpenselist"];
                topExpenses = data["Top10Expenses"];
                expenses = data["AllExpenses"];
                biggestExpense = data["BiggestSingularExpense"];
            }
            errorMessage = ''; // Clear error message on successful submission
            files = []; // Clear the file list after successful upload
        } catch (error) {
            errorMessage = 'An error occurred while uploading files.';
            console.error(error);
        } finally {
            isLoading = false; // Hide spinner
        }
    }

    function test() {
        const data = dados
        console.log('test data', data);
        sessionToken = data['sessionToken']; 
        smartList = data['SmartGroupExpenselist'];
        topExpenses = data['Top10Expenses'];
        expenses = data['AllExpenses'];
        biggestExpense = data['BiggestSingularExpense'];
    }
</script>

<style>
    /* Atualização da fonte */
    body {
        background-color: #121212;
        color: #e0e0e0;
        font-family: 'Poppins', Arial, sans-serif; /* Fonte mais amigável */
        margin: 0;
        padding: 0;
    }

    .container {
        max-width: 800px;
        margin: 0 auto;
        padding: 20px;
    }

    .header {
        text-align: center;
        margin-bottom: 20px;
        padding: 20px;
        /* background-color: #1e1e1e; */
        border-radius: 10px;
        /* box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5); */
    }

    .header img {
        max-width: 20%;
        height: auto;
        border-radius: 10px;
        margin-bottom: 20px;
    }

    .header-content h1 {
        font-size: 2rem;
        color: #76c893;
        margin-bottom: 10px;
    }

    .header-content p {
        font-size: 1.1rem;
        color: #e0e0e0;
        margin-bottom: 20px;
    }

    .header-content ul {
        list-style-type: disc;
        padding: 0;
        text-align: left;
        margin-left: 3%;
    }

    .header-content li {
        font-size: 1rem;
        margin-bottom: 10px;
    }

    .info-green {
        color: #76c893;
    }

    .info-red {
        color: #ff6b6b;
    }

    .file-list {
        margin-top: 20px;
        background-color: #1e1e1e;
        padding: 20px;
        border-radius: 10px;
    }

    .file-card {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px;
        border: 1px solid #76c893;
        border-radius: 5px;
        margin-bottom: 10px;
        background-color: #2a2a2a;
        transition: box-shadow 0.3s ease, transform 0.3s ease;
    }

    .file-card:hover {
        box-shadow: inset 0 0 10px #76c893;
        transform: scale(1.02);
    }

    .file-icon {
        font-size: 24px;
        color: #76c893;
    }

    .file-name {
        flex-grow: 1;
        color: #e0e0e0;
    }

    .remove-button {
        background: #76c893;
        color: #121212;
        border: none;
        border-radius: 5px;
        padding: 5px 10px;
        cursor: pointer;
    }

    .remove-button:hover {
        background: #5aa678;
    }

    .form-actions {
        display: flex;
        justify-content: flex-end; /* Alinha os botões à direita */
        gap: 10px; /* Espaçamento entre os botões */
        margin-top: 20px;
    }

    .form-actions button {
        background: #76c893;
        color: #121212;
        border: none;
        border-radius: 5px;
        padding: 10px 20px;
        cursor: pointer;
        font-size: 1rem;
    }

    .form-actions button:hover {
        background: #5aa678;
    }

    /* Esconde o input padrão */
    input[type="file"] {
        display: none;
    }

    /* Estilo do botão customizado */
    .custom-file-picker {
        display: inline-block;
        background: #76c893;
        color: #121212;
        border: none;
        border-radius: 5px;
        padding: 10px 20px;
        cursor: pointer;
        font-size: 1rem;
        text-align: center;
    }

    .custom-file-picker:hover {
        background: #5aa678;
    }

    .custom-file-picker span {
        pointer-events: none;
    }

    .error-message {
        color: #ff6b6b;
        margin-top: 10px;
        font-size: 0.9rem;
    }

    .spinner {
        border: 4px solid #e0e0e0;
        border-top: 4px solid #76c893;
        border-radius: 50%;
        width: 30px;
        height: 30px;
        animation: spin 1s linear infinite;
        margin: 0 auto;
    }

    @keyframes spin {
        0% {
            transform: rotate(0deg);
        }
        100% {
            transform: rotate(360deg);
        }
    }

    .loading-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
    }
</style>

<div class="container">
    <div class="header">
        <img src="/money.png" alt="Header" />
        <div class="header-content">
            <h1>Bem-vindo</h1>
            <p>Faça o upload dos seus extratos bancários em PDF e receba automaticamente um resumo claro dos seus gastos. Simples, rápido e seguro.</p>
            <ul>
                <li class="info-green">Os arquivos devem ser do tipo: PDF</li>
                <li class="info-red">Os arquivos não podem passar de 1Mb cada</li>
                <li class="info-red">Não pode ter mais de 4 arquivos por vez</li>
                <li class="info-red">Nenhum tipo de dado pessoal fornecido será salvo</li>
            </ul>
        </div>
    </div>

    <div class="file-list">
        {#each files as file, index}
            <div class="file-card">
                <span class="file-icon">📄</span>
                <span class="file-name">{file.name}</span>
                <button class="remove-button" on:click={() => removeFile(index)}>Remove</button>
            </div>
        {/each}
    </div>

    {#if errorMessage}
        <p class="error-message">{errorMessage}</p>
    {/if}

    <form on:submit={test}>
        <div class="form-actions">
            <label class="custom-file-picker">
                <input type="file" accept="application/pdf" multiple on:change={handleFileChange} />
                <span>Choose Files</span>
            </label>
            <button type="submit">Processar</button>
        </div>
    </form>

    {#if isLoading}
        <div class="loading-overlay">
            <div class="spinner"></div>
        </div>
    {/if}
</div>
<br>
{#if sessionToken}
    <ExpenseReport
        sessionToken={sessionToken}
        smartList={smartList}
        topExpenses={topExpenses}
        expenses={expenses}
        biggestExpense={biggestExpense}
    />
{/if}
