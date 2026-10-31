# Demonstração da solução

Vídeo narrado de **2min54s**: [demonstracao.mp4](demonstracao.mp4). As telas são capturas reais da aplicação executada com MongoDB no perfil demo. Os valores são exclusivamente demonstrativos. A narração é sintética e explica as decisões de projeto.

## Roteiro para apresentar ao vivo

| Tempo sugerido | Demonstração | Justificativa |
|---|---|---|
| 0:00–0:25 | Abrir `/dashboard` como ADMIN/GERENTE. Mostrar faturamento, pedidos, ticket e cancelamentos. | Indicadores usam os mesmos filtros; receita considera somente finalizados com desconto. |
| 0:25–0:50 | Alternar 30 para 7 dias; selecionar agrupamento e trocar Receita/Pedidos. | Compara períodos e revela a evolução da operação. Faixas vazias recebem zero. |
| 0:50–1:15 | Mostrar rosca de status e selecionar CANCELADO. | Pedidos cancelados aparecem no volume e taxa, mas não no faturamento. |
| 1:15–1:40 | Rolar para o top 5 e avançar pedidos. | Quantidade por produto orienta o cardápio; paginação limita dados transferidos. |
| 1:40–2:05 | Consultar período sem dados e mostrar layout móvel. | Estados vazios claros e acesso em telas pequenas. |
| 2:05–2:35 | Explicar os três endpoints, MongoDB e segurança. | Mesma autenticação, agregação no banco, decimal e histórico de itens. |
| 2:35–2:55 | Mostrar testes e documentação no GitHub. | Evidências reproduzíveis, CI e implementação em branch de feature para develop. |

## Para reproduzir

Siga a seção de demonstração do [README](../../README.md). O perfil demo exige um banco terminado em `_demo`; ele gera 60 dias de pedidos somente quando a coleção está vazia. Uma nova execução usa os registros já existentes. Para recriar em outra data, escolha um novo banco vazio com esse sufixo.

## Materiais

- [Requisitos e regras](requisitos.md)
- [API e exemplos](api.md)
- [Arquitetura/modelagem](arquitetura.md)
- [Testes e evidências](testes.md)
- [Capturas](evidencias/)

O vídeo versionado pode ser baixado pelo GitHub caso o navegador não ofereça reprodução direta. Envie no Teams somente o link do repositório/branch da entrega, conforme o enunciado.
