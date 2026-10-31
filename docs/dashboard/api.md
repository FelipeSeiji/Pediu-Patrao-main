# API do Dashboard

Base: `http://localhost:8080`. JSON UTF-8. Contrato completo: [openapi.yaml](openapi.yaml).

## Autenticação e autorização

Os três endpoints exigem sessão de um usuário `ADMIN` ou `GERENTE`. Faça login em `/login`; o navegador envia `JSESSIONID` automaticamente. Não há cadastro público nem token fixo. As consultas GET não exigem token CSRF. Escritas do sistema existente continuam exigindo CSRF.

Fluxo por terminal, usando uma conta de demonstração criada com sua senha de ambiente:

```powershell
$base = 'http://localhost:8080'
$login = Invoke-WebRequest "$base/login" -SessionVariable sessao
$csrf = [regex]::Match($login.Content, 'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
$credencial = Get-Credential -UserName admin -Message 'Conta local do Pediu Patrão'
Invoke-WebRequest "$base/login" -Method Post -WebSession $sessao -Body @{
    username = $credencial.UserName
    password = $credencial.GetNetworkCredential().Password
    _csrf = $csrf
} | Out-Null
Invoke-RestMethod "$base/api/dashboard?inicio=2026-10-01&fim=2026-10-31&agrupamento=dia" -WebSession $sessao
```

Não versione cookies/senhas. Para o dashboard web, entre no sistema e abra `/dashboard` pelo menu lateral.

## Parâmetros de consulta

| Parâmetro | Rotas | Tipo / padrão | Validação e significado |
|---|---|---|---|
| `inicio` | resumo e pedidos | `date`, padrão `fim − 29 dias` | `AAAA-MM-DD`, inclusivo. |
| `fim` | resumo e pedidos | `date`, padrão hoje em São Paulo | Inclusivo. Período de 1 a 366 dias; início ≤ fim. Datas futuras válidas retornam vazio se não houver dados. |
| `status` | resumo e pedidos | string opcional | Vazio/ausente = todos; aceita os sete status canônicos abaixo, sem distinção de maiúsculas. |
| `agrupamento` | resumo | `dia` (padrão), `semana`, `mes` | Define a série temporal. |
| `pagina` | pedidos | inteiro, padrão `0` | Base zero, entre 0 e 100000. |
| `tamanho` | pedidos | inteiro, padrão `8` | Entre 1 e 50. |

Status: `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO`, `CANCELADO`. Use o valor canônico nos filtros; a API normaliza os nomes legados nos registros do banco.

## GET /api/dashboard

Consulta integrada de indicadores, comparação, série temporal, situações e top 5. A mesma seleção é aplicada a todos os blocos. Valores monetários são números em reais, com duas casas decimais no servidor. Sem pedidos: indicadores zerados, ranking vazio e série preenchida com zero.

Requisição:

```http
GET /api/dashboard?inicio=2026-10-31&fim=2026-10-31&agrupamento=dia
Accept: application/json
Cookie: JSESSIONID=<sessao-autenticada>
```

Resposta ilustrativa `200 OK`, `Cache-Control: no-store`:

```json
{
  "filtros": {"inicio":"2026-10-31","fim":"2026-10-31","status":null,"agrupamento":"dia"},
  "fusoHorario":"America/Sao_Paulo",
  "atualizadoEm":"2026-10-31T23:00:00Z",
  "indicadores": {
    "pedidos":2,"finalizados":1,"emAndamento":0,"cancelados":1,
    "faturamento":90.00,"ticketMedio":90.00,"descontos":10.00,"taxaCancelamento":50.00
  },
  "comparacao": {"inicio":"2026-10-30","fim":"2026-10-30","faturamento":80.00,"variacaoFaturamento":12.50},
  "vendas":[{"data":"2026-10-31","pedidos":2,"faturamento":90.00}],
  "situacoes":[
    {"status":"RECEBIDO","quantidade":0},{"status":"EM_PREPARACAO","quantidade":0},
    {"status":"PRONTO","quantidade":0},{"status":"SAIU_PARA_ENTREGA","quantidade":0},
    {"status":"RETIRADO","quantidade":0},{"status":"FINALIZADO","quantidade":1},
    {"status":"CANCELADO","quantidade":1}
  ],
  "produtos":[{"produtoId":"p1","nome":"Pizza de calabresa","quantidade":2,"receitaBruta":100.00}]
}
```

`variacaoFaturamento` será `null` quando não houver receita anterior. `atualizadoEm` é um instante UTC. `data` é o início da faixa no fuso informado; pode anteceder `inicio` numa semana/mês parcial, mas os pedidos agregados sempre respeitam o intervalo. `situacoes` sempre inclui os sete status, inclusive zerados; também pode incluir situações desconhecidas do legado.

## GET /api/dashboard/pedidos

Consulta paginada, sem dados pessoais. Ordenação: entrada decrescente, depois ID decrescente para desempate estável.

```http
GET /api/dashboard/pedidos?inicio=2026-10-31&fim=2026-10-31&status=FINALIZADO&pagina=0&tamanho=8
```

Resposta ilustrativa `200 OK`, `Cache-Control: no-store`:

```json
{
  "itens":[{"id":"abc123","status":"FINALIZADO","dataHoraEntrada":"2026-10-31T22:00:00Z","valorTotal":90.00,"quantidadeItens":2}],
  "total":1,"pagina":0,"tamanho":8,"totalPaginas":1
}
```

Uma página além do total retorna `200` com `itens: []` e os metadados preservados. Nenhum resultado retorna `total: 0` e `totalPaginas: 0`. `quantidadeItens` representa unidades, não quantidade de linhas do pedido.

## GET /api/dashboard/filtros

Informa opções aceitas e a data atual do estabelecimento. Sem parâmetros.

```http
GET /api/dashboard/filtros
```

Resposta ilustrativa `200 OK`:

```json
{
  "status":["RECEBIDO","EM_PREPARACAO","PRONTO","SAIU_PARA_ENTREGA","RETIRADO","FINALIZADO","CANCELADO"],
  "agrupamentos":["dia","semana","mes"],
  "fusoHorario":"America/Sao_Paulo","limiteDias":366,"hoje":"2026-10-31"
}
```

## Erros

| HTTP | Causa | Exemplo / ação |
|---|---|---|
| 400 | Formato ou domínio inválido | `inicio=31-10-2026`, início após fim, 367 dias, status desconhecido, `agrupamento=hora`, `pagina=-1`, `tamanho=51`. Corrigir parâmetros. |
| 401 | Sessão ausente/expirada | Fazer login novamente. APIs não redirecionam para HTML de login. |
| 403 | Perfil sem permissão | ATENDENTE não pode consultar nenhum endpoint do painel. |
| 405 | Método não suportado | Estes recursos são somente GET. Uma escrita sem CSRF pode ser rejeitada antes com 403 pelo Spring Security. |
| 503 | MongoDB indisponível/falha de consulta | Tentar novamente; detalhes internos só aparecem no log do servidor. |

Erros de validação e banco usam `application/problem+json`, por exemplo:

```json
{
  "type":"about:blank","title":"Consulta inválida","status":400,
  "detail":"Status inválido.","instance":"/api/dashboard","timestamp":"2026-10-31T23:00:00Z"
}
```

401 e 403 também usam `application/problem+json`, com `status`, `title` e `detail`. Os clientes devem usar `status` e `detail`, sem depender de campos adicionais.

## Consistência e limites

Uma agregação `$facet` produz os blocos do resumo a partir da mesma leitura. Comparação anterior e página de pedidos são consultas separadas: alterações concorrentes podem gerar diferenças momentâneas entre elas. A atualização seguinte reconcilia a visão; não há transação de snapshot envolvendo as três consultas. O dashboard não mantém cache financeiro no cliente ou no servidor.
