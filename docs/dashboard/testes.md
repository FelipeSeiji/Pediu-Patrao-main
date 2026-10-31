# Evidências de testes

## Execução automatizada

Executado em **31/10/2026**, Java **21.0.12.1**, Spring Boot **3.4.4**, Maven e MongoDB Community **9.0**, em instância local isolada na porta 27018. Nenhum teste foi executado contra dados do estabelecimento.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-21'
$env:MONGODB_TEST_URI = 'mongodb://127.0.0.1:27018/pediupatrao_dashboard_test'
.\mvnw.cmd verify -B
```

Resultado: **57 testes, 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS**. O pacote JAR foi gerado. Evidências exportadas diretamente dos relatórios Surefire: [test-results.json](evidencias/test-results.json). O exportador [export-test-evidence.py](../../tools/export-test-evidence.py) remove propriedades de ambiente e credenciais dos relatórios antes de versionar.

Também foram aprovadas **9 verificações HTTP** na aplicação executada: autenticação por formulário e sessão, três endpoints, totais conciliados entre série e indicadores, agrupamento semanal, cancelamentos sem receita, paginação sem campos pessoais, erros 400 e acesso anônimo 401. Resultado: [api-smoke.json](evidencias/api-smoke.json); resposta real com dados fictícios: [api-resumo-demo.json](evidencias/api-resumo-demo.json). Para repetir, defina `DASHBOARD_DEMO_PASSWORD` e execute `python tools/smoke-dashboard.py` no ambiente demo. Cookies e senha não são exportados.

O contrato OpenAPI foi validado com `openapi-spec-validator`. O vídeo foi decodificado integralmente com FFmpeg, sem erro: H.264/AAC, 1600×900, duração **2min54s**, abaixo do limite de três minutos. [Metadados](evidencias/video-metadata.json).

O CI fixa `ubuntu-22.04`, cujo kernel 6.8 permite iniciar o MongoDB 8.0. A primeira execução em `ubuntu-latest` encontrou a incompatibilidade de inicialização do banco com kernels 6.19–7.0.13, antes de executar o código da aplicação. Referências: [restrição documentada pelo MongoDB](https://www.mongodb.com/docs/manual/administration/production-notes/), [imagem do runner 22.04](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2204-Readme.md). Ao atualizar o runner, verifique a compatibilidade do kernel com a imagem do banco.

| Suíte | Testes | Cobertura relevante |
|---|---:|---|
| DashboardFiltroTest | 8 | Padrões, mesmo período anterior, um dia, ano bissexto, período invertido/excessivo, status e agrupamento inválidos. |
| DashboardControllerTest | 16 | 401 JSON nas três rotas, atendente bloqueado nas APIs e página, ADMIN/GERENTE, renderização real Thymeleaf, parâmetros inválidos, 503 sem detalhes internos e no-store. |
| DashboardMongoIntegrationTest | 9 | Mongo real: decimal, receita com desconto, ticket, cancelamentos, estados legados, fronteiras de meia-noite, filtros, comparação, faixas vazias, paginação estável e top 5 com múltiplos itens. |
| DashboardHttpIntegrationTest | 3 | Spring Boot + segurança + controller + serviço + banco sem mocks; resposta monetária, projeção sem cliente e bloqueio direto no serviço. |
| Suítes anteriores do sistema | 21 | Regressão em pedidos, produtos, clientes, entregadores, horários, configurações e permissões. |

Os testes de banco usam apenas `pediupatrao_dashboard_test` e `pediupatrao_dashboard_http_test`, explicitamente definidos nas classes. Eles limpam a coleção de pedidos nesses bancos de teste antes de cada caso. Não aponte ambientes reais para esse fluxo de testes. Sem `MONGODB_TEST_URI`, os 12 casos que usam banco são marcados como ignorados; o workflow de CI define a variável e executa todos.

## Verificação da interface em navegador

Aplicação executada no perfil `demo`, banco `pediupatrao_demo`. Capturas reais de 31/10/2026; os valores abaixo são demonstrativos.

| Caso | Resultado observado | Evidência |
|---|---|---|
| Carga inicial, últimos 30 dias | 731 pedidos, R$ 68.438,09 de receita e 664 finalizados. | [Visão geral](evidencias/01-visao-geral.png) |
| Atalho de sete dias | Todos os blocos recalculados: 168 pedidos, receita R$ 15.838,94. | [Sete dias](evidencias/02-sete-dias.png) |
| Ranking de produtos | Top 5 com quantidades e receita bruta; indicador de escopo finalizado. | [Ranking](evidencias/03-ranking-pedidos.png) |
| Próxima página | Tabela mudou do primeiro grupo para pedidos seguintes; paginação mostrou 9–16 de 168. | [Paginação](evidencias/04-paginacao.png) |
| Situação CANCELADO | 14 pedidos, taxa 100%, receita e ticket R$ 0,00. | [Cancelados](evidencias/05-cancelados.png) |
| Período sem movimento | Mensagem explícita e indicadores zerados, sem erro de divisão. | [Sem dados](evidencias/06-sem-dados.png) |
| Agrupamento semanal | Gráfico com início de semana na segunda e mesma receita total filtrada. | [Vendas semanais](evidencias/07-vendas-semanais.png) |
| Alternância de métrica | Gráfico mudou de faturamento para quantidade de pedidos. | [Volume](evidencias/08-volume-pedidos.png) |
| Layout móvel | Cartões e gráficos reorganizados, sem overflow horizontal (largura útil e de conteúdo: 375 px). | [Gráficos móveis](evidencias/09-mobile.png), [filtros móveis](evidencias/10-mobile-filtros.png) |

As capturas de navegação mostram o conteúdo que cabia na janela; a página completa permite rolar até as seções restantes. O vídeo usa as mesmas capturas reais.

## Reproduzir e verificar

1. Inicie MongoDB e configure Java 21.
2. Defina `MONGODB_TEST_URI` e execute `mvnw verify`.
3. Consulte `target/surefire-reports/`; gere o resumo com `python tools/export-test-evidence.py`.
4. Inicie o perfil demo descrito no README e repita os casos da tabela.
5. Para regerar o vídeo no Windows: `pip install pillow imageio-ffmpeg` e `powershell -File tools/generate-demo.ps1`. O gerador exige duração inferior a 180 segundos.

## Limites da validação

Não foram realizados ensaios de carga, auditoria formal de acessibilidade ou testes em todos os navegadores/dispositivos. A indisponibilidade 503 é testada automaticamente no controller; a desconexão de rede do navegador e o intervalo de 60 segundos não possuem teste automatizado de ponta a ponta. A documentação distingue essas limitações dos testes efetivamente executados.
