# Dashboard Administrativo — análise de requisitos

## Contexto e atores

O responsável pela lanchonete/pizzaria precisa acompanhar receita, volume de pedidos, cancelamentos e preferência dos clientes usando os pedidos já registrados no Pediu Patrão.

**ADMIN e GERENTE** consultam o dashboard. **ATENDENTE** continua operando os pedidos, sem acesso ao painel financeiro. Os dados vêm da coleção `pedidos` do MongoDB, por uma API REST autenticada por sessão.

## Requisitos funcionais e critérios de aceite

| ID | Requisito implementado | Critério de aceite |
|---|---|---|
| RF01 | Indicadores gerais de vendas | Exibir faturamento líquido realizado, quantidade total de pedidos, ticket médio, cancelados, taxa de cancelamento, pedidos em andamento e finalizados. Comparar faturamento com período anterior de igual duração. |
| RF02 | Análise de vendas | Gráfico de receita ou quantidade por dia, semana ou mês. Intervalos sem pedidos aparecem com zero. Tabela alternativa permite consultar os valores exatos. |
| RF03 | Situação dos pedidos | Gráfico de rosca e legenda com contagem de cada status. Tabela dos pedidos mais recentes, ordenada e paginada. |
| RF04 | Produtos mais vendidos | Top 5 por quantidade de unidades em pedidos finalizados, com nome histórico e receita bruta dos itens. |
| RF05 | Filtros de consulta | Datas inicial/final, situação e agrupamento; atalhos de 7 dias, 30 dias e mês atual. Os filtros aplicados afetam indicadores, gráficos, ranking e pedidos. A URL preserva a consulta. |
| RF06 | Integração com API | Consumir `GET /api/dashboard`, `/api/dashboard/pedidos` e `/api/dashboard/filtros`, com dados agregados a partir do MongoDB. |
| RF07 | Atualização e tratamento | Atualização manual e automática a cada 60 segundos na aba visível; estados de carregamento, consulta vazia, sessão expirada, acesso negado, indisponibilidade e tempo limite. Respostas de consultas anteriores não sobrescrevem uma consulta nova. |

## Requisitos não funcionais

| ID | Requisito | Implementação/verificação |
|---|---|---|
| RNF01 | Controle de acesso | Spring Security nas rotas e autorização no serviço; apenas ADMIN/GERENTE. APIs retornam 401/403 JSON. CSRF continua habilitado para operações de escrita existentes. |
| RNF02 | Precisão monetária | Agregação com decimal no MongoDB; `BigDecimal` e arredondamento HALF_UP para duas casas na API. Desconto aplicado pelo sistema de pedidos. |
| RNF03 | Consultas delimitadas | Máximo de 366 dias, índice por data e ID, agregação no banco, top 5 e páginas de até 50 pedidos. Sem `findAll` no dashboard. |
| RNF04 | Privacidade | Resumos de pedidos não expõem cliente, endereço, usuário responsável, senha ou justificativas. Respostas financeiras usam `Cache-Control: no-store`. |
| RNF05 | Usabilidade e acessibilidade | Layout responsivo, navegação por teclado, foco visível, rótulos nos campos, mensagens anunciadas, tabela para o gráfico e legendas textuais de status. |
| RNF06 | Consistência temporal | Datas inclusivas em `America/Sao_Paulo`, convertidas para instantes UTC; limite superior exclusivo no dia seguinte. |
| RNF07 | Manutenibilidade | Pacote `dashboard` com controller, filtro, serviço, repositório e tratamento de erros. Frontend sem dependências de gráficos externas. Contrato OpenAPI e testes automatizados. |
| RNF08 | Recuperação | Cliente cancela consultas substituídas e limita espera a 15 segundos. Falha mantém indicação explícita de dados anteriores ou oculta resultados quando o filtro mudou. |
| RNF09 | Reprodutibilidade | Java 21, Maven Wrapper, Docker Compose opcional para MongoDB, perfil demo separado e CI com MongoDB real. |

RNF03 reduz custo, mas não constitui SLA de latência. Carga concorrente e grandes volumes precisam de ensaios no ambiente de produção antes de definir metas de desempenho.

## Regras de negócio

1. **Data de referência:** recebimento (`dataHoraEntrada`). Um pedido recebido em outubro e finalizado depois continua associado a outubro. O painel é uma análise operacional por recebimento, não um relatório contábil por data de liquidação.
2. **Faturamento realizado:** soma de `valorTotal` somente de `FINALIZADO` (e legado `ENTREGUE`), já com desconto. Cancelados e pedidos em andamento não geram faturamento realizado.
3. **Ticket médio:** faturamento / quantidade de finalizados. Sem finalizados, retorna `0.00`.
4. **Pedidos:** todos os pedidos no intervalo e no status selecionado. Em andamento = total − finalizados − cancelados; inclui situações legadas desconhecidas, discriminadas na legenda.
5. **Taxa de cancelamento:** cancelados / total filtrado × 100; base zero retorna zero. Se o filtro é CANCELADO, a taxa é 100% quando há pedidos.
6. **Comparação:** intervalo imediatamente anterior com a mesma quantidade de dias e mesmo status. Variação = (receita atual − anterior) / anterior × 100. Base anterior zero retorna `null`, exibido como “sem base”.
7. **Ranking:** somente itens de finalizados; soma das quantidades por `produtoId`; desempate por ID crescente. A receita por produto é bruta: soma de `itens.total`, antes do desconto global. Não deve ser somada como se fosse faturamento líquido. Usa o nome do item do pedido mais recente, preservando histórico mesmo se o catálogo mudar.
8. **Agrupamento:** semana começa na segunda-feira; mês no primeiro dia. Primeira/última faixa podem ser parciais e não incluem pedidos fora do filtro.
9. **Legado:** `EM_PREPARO` → `EM_PREPARACAO`; `ENTREGUE` → `FINALIZADO`. Valores monetários ausentes/inconvertíveis viram zero e totais negativos são limitados a zero. Status ausente aparece como `DESCONHECIDO`. Pedidos sem data de entrada não entram em consultas por período. Não há estimativa de preço a partir do catálogo atual.
10. **Dados demo:** inserção explícita, apenas com perfil `demo`, banco com sufixo `_demo` e coleção de pedidos vazia. Nenhum pedido real é alterado pelo dashboard.

## Rastreabilidade

RF01/RF02/RF04 → `DashboardRepository.analisar`, `DashboardService.consultar`; RF03 → consultas agregadas e endpoint paginado; RF05 → `DashboardFiltro` e formulário; RF06 → `DashboardController` e `dashboard.js`; RF07 → `DashboardExceptionHandler` e controle de atualização do cliente. A matriz de evidências está em [testes.md](testes.md).
