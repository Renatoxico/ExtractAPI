# Política de Privacidade do Somai

**Última atualização:** 31/10/2026
**Vigência:** a partir de 31/10/2026

O Somai (https://somai.renatoxico.net e app para iOS) transforma extratos bancários e faturas de cartão em PDF em um relatório de gastos por categoria. Esta Política explica, conforme a Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018, "LGPD"), quais dados pessoais tratamos, por que tratamos, com quem compartilhamos, por quanto tempo guardamos e como você exerce seus direitos.

## 1. Quem é o controlador

O controlador dos seus dados pessoais é:

- **Nome:** Renato Siqueira Dias Junior
- **CPF:** 135.4xx.xxx-x9
- **Endereço:** São Carlos/SP
- **E-mail para assuntos de privacidade:** diasrenato2@outlook.com
- **Encarregado (DPO):** o Somai é mantido por pessoa física de pequeno porte e, conforme a Resolução CD/ANPD nº 2/2022, não indica encarregado. O canal para assuntos de privacidade é diasrenato2@outlook.com.

## 2. Quais dados tratamos

### 2.1 Dados da conta (login)

O login é feito pelo **Firebase Authentication**, do Google: na web, com conta Google ou Microsoft; no app iOS, com conta Google, conta Microsoft ou e-mail e senha. Recebemos do Firebase e guardamos no nosso banco de dados:

- identificador do usuário no Firebase (UID);
- e-mail;
- nome de exibição e URL da foto de perfil, quando a conta Google os informa.

Quem guarda a senha (login por e-mail no iOS) é o Firebase. Nós não temos acesso a ela.

### 2.2 Arquivos que você envia (extratos e faturas em PDF)

- São aceitos até 6 arquivos PDF por envio, cada um com no máximo 512 KB.
- O texto do PDF é lido **em memória** no nosso servidor. **O arquivo original não é gravado** em disco nem no banco de dados depois do processamento.
- **O nome do arquivo** pode aparecer nos registros técnicos (logs) do servidor e em mensagens de erro. Por isso, recomendamos não colocar dados pessoais no nome do arquivo. Os logs são guardados pelo prazo do item 6.

### 2.3 Transações extraídas e relatórios

Do texto do PDF, extraímos e guardamos, associados à sua conta:

- data da transação;
- valor;
- descrição da transação, como aparece no extrato (por exemplo, nome do estabelecimento ou do destinatário de um Pix). Sequências de 4 ou mais dígitos são removidas da descrição;
- categoria atribuída;
- identificador e data de criação do relatório.

Não extraímos nem guardamos número de conta, agência, número do cartão ou saldo como campos próprios. Mas as descrições podem conter **nomes de terceiros**, como pessoas para quem você fez um Pix.

### 2.4 Catálogo de categorização (compartilhado e sem vínculo com a conta)

Para categorizar os gastos, mantemos um catálogo de **descrições de transações** e suas categorias. Ele é usado para todos os usuários e **não fica ligado à sua conta**. Isso evita classificar de novo a mesma descrição, como "UBER TRIP". Esse catálogo e o histórico das tarefas de classificação (descrição enviada e resposta recebida da IA) ficam no nosso banco de dados.

### 2.5 Dados técnicos

- Registros de acesso e de erro do servidor (data e hora, endereço IP, rota acessada, mensagens de erro, nome de arquivo).
- Não usamos cookies de publicidade nem ferramentas de analytics de terceiros.
- O Firebase Authentication guarda o estado de login no navegador ou no aparelho (armazenamento local).

Não tratamos dados de menores de 18 anos. O serviço é destinado apenas a maiores de idade.

## 3. Para que usamos os dados e com qual base legal

| Finalidade | Dados | Base legal (LGPD, art. 7º) |
|---|---|---|
| Criar e manter sua conta, autenticar e controlar o acesso aos seus relatórios | Dados da conta | Execução de contrato (inciso V) |
| Gerar, exibir, guardar e exportar (CSV) seus relatórios | PDF (temporário), transações, relatórios | Execução de contrato (inciso V) |
| Categorizar gastos com IA e manter o catálogo de categorias | Descrições das transações | Execução de contrato (inciso V) e legítimo interesse em melhorar a precisão do serviço (inciso IX) |
| Monitorar falhas, segurança e funcionamento | Logs, descrições envolvidas em falhas | Legítimo interesse (inciso IX) |
| Cumprir obrigações legais e regulatórias e exercer direitos em processos | Dados necessários ao caso | Obrigação legal (inciso II) e exercício regular de direitos (inciso VI) |
| Comunicações sobre o serviço (por exemplo, convite de teste ou pesquisa de feedback) | E-mail | Legítimo interesse (inciso IX), com opção de sair a qualquer momento; consentimento (inciso I) para envios de marketing |

Dados financeiros não são "dados pessoais sensíveis" na definição do art. 5º, II, da LGPD. Mesmo assim, tratamos esses dados com cuidado redobrado e **não vendemos nem alugamos dados pessoais ou financeiros**.

## 4. Com quem compartilhamos (operadores)

- **Google Firebase Authentication (Google LLC):** login e gestão de contas.
- **Google Gemini API (Google LLC):** sugere a categoria de cada gasto. **Enviamos apenas as descrições das transações** (por exemplo, "PADARIA SAO JOSE") e um número interno de tarefa. **Não enviamos** o arquivo PDF, valores, datas, seu e-mail nem identificadores da sua conta. O modelo usado atualmente é da família Gemini Flash. A chave da API usada pelo Somai está no nível pago da Gemini API, em que o Google não usa esses dados para melhorar ou treinar seus modelos.
- **Provedor de e-mail (Gmail/Google Workspace, via SMTP):** envia alertas operacionais **somente para os administradores**. Esses alertas podem conter descrições de transações cuja categorização falhou. Usuários não recebem esses e-mails.
- **Provedor de hospedagem (servidor/VPS):** Hostinger, com servidor no Brasil, hospeda a aplicação e o banco de dados PostgreSQL.
- **GitHub (GitHub, Inc.):** guarda o código e as imagens da aplicação (GHCR). **Não recebe dados de usuários.** Mencionado aqui apenas para transparência; pode ser removido.
- **Autoridades públicas:** quando exigido por lei ou ordem judicial.

## 5. Transferência internacional de dados

O Google (Firebase Authentication e Gemini API) pode tratar dados em servidores fora do Brasil, inclusive nos Estados Unidos. Isso inclui seus dados de conta e as descrições das transações enviadas para categorização. Essa transferência é necessária para prestar o serviço e é feita conforme o art. 33 da LGPD, com base nas cláusulas contratuais de proteção de dados que o Google oferece em seus termos de tratamento de dados do Google Cloud e do Firebase. A aplicação e o banco de dados ficam no Brasil.

## 6. Por quanto tempo guardamos

| Dado | Prazo |
|---|---|
| Arquivo PDF | Não é armazenado. Fica em memória só durante o processamento |
| Dados da conta, transações e relatórios | Enquanto sua conta existir. Excluídos quando você exclui a conta ou o relatório (ver item 8) |
| Catálogo de descrições e categorias (sem vínculo com a conta) | Por tempo indeterminado, como parte do funcionamento do serviço. |
| Histórico das tarefas de classificação por IA | Por tempo indeterminado, sem vínculo com a conta, junto com o catálogo de categorias |
| Logs do servidor | Até 6 meses |
| Cópias de segurança (backup) | Cópias (snapshots) do servidor feitas pelo provedor de hospedagem, substituídas periodicamente pelas mais novas. Dados excluídos somem das cópias quando as anteriores à exclusão são substituídas |

Depois desses prazos, os dados são excluídos ou anonimizados, salvo as hipóteses do art. 16 da LGPD.

## 7. Como protegemos os dados

Medidas que usamos hoje:

- conexão criptografada (HTTPS/TLS) entre o seu navegador ou app e o servidor;
- autenticação pelo Firebase, com verificação do token de acesso em cada requisição ao servidor;
- cada relatório só pode ser consultado ou exportado pelo dono da conta (verificação de propriedade no servidor);
- o PDF original não é gravado depois do processamento;
- envio mínimo à IA: só descrições, sem valores, datas ou identificadores da conta;
- remoção de sequências de 4 ou mais dígitos das descrições;
- limite de tamanho e quantidade de arquivos, e aceitação apenas de PDF;
- credenciais do servidor guardadas como segredos fora do código;
- cabeçalhos de segurança no servidor web.

Também mantemos cópias de segurança (snapshots) do servidor pelo provedor de hospedagem, e o acesso administrativo ao servidor é restrito ao responsável pelo Somai.

Nenhum sistema é 100% seguro. Se houver um incidente de segurança com risco ou dano relevante, comunicaremos a você e a ANPD, conforme o art. 48 da LGPD.

## 8. Seus direitos (art. 18 da LGPD)

Você pode, a qualquer momento e sem custo:

1. confirmar se tratamos seus dados;
2. acessar seus dados. Seus relatórios ficam no histórico do app e podem ser exportados em CSV;
3. corrigir dados incompletos, inexatos ou desatualizados;
4. pedir anonimização, bloqueio ou eliminação de dados desnecessários ou tratados em desconformidade com a LGPD;
5. pedir a portabilidade dos dados (exportação CSV dos relatórios);
6. pedir a eliminação dos dados tratados com base no consentimento;
7. saber com quem compartilhamos seus dados (item 4);
8. saber que pode não dar consentimento e quais são as consequências;
9. revogar o consentimento;
10. se opor a tratamento baseado em legítimo interesse, quando não cumprir a LGPD;
11. pedir revisão de decisões tomadas só com base em tratamento automatizado (art. 20). A categorização por IA só organiza seu relatório e não gera decisões sobre você.

**Exclusão da conta e dos dados:** Você pode excluir sua conta em *Conta → Excluir conta*, no site ou no app, ou pedir a exclusão pelo e-mail diasrenato2@outlook.com. A exclusão apaga, de forma definitiva, sua conta no Firebase, seus dados de cadastro, todos os seus relatórios e transações. Descrições já incorporadas ao catálogo de categorização (item 2.4) continuam lá, sem qualquer vínculo com você. Logs e backups são apagados quando os prazos do item 6 acabarem.

Para exercer seus direitos, escreva para diasrenato2@outlook.com. Respondemos em até 15 dias (art. 19, II). Se precisarmos confirmar sua identidade, pediremos que escreva a partir do e-mail da conta. Você também pode reclamar à **Agência Nacional de Proteção de Dados (ANPD)** (https://www.gov.br/anpd).

## 9. Alterações desta Política

Podemos atualizar esta Política. A data no topo mostra a versão em vigor. Mudanças relevantes serão avisadas no app ou por e-mail antes de valerem.

## 10. Contato

Renato Siqueira Dias Junior, diasrenato2@outlook.com
