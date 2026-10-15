# Pediu Patrão

Sistema de pedidos para pizzaria com Spring Boot, Thymeleaf, Spring Security e MongoDB.

## Execução

Requisitos: Java 21 e MongoDB.

Configure a conexão e a senha inicial do administrador no ambiente. A conta `admin` só é criada se ainda não existir e `APP_BOOTSTRAP_ADMIN_PASSWORD` estiver definido; a senha não é alterada em inicializações seguintes.

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/pizzaria"
$env:MONGODB_DATABASE = "pizzaria"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = "defina-uma-senha-forte"
.\mvnw.cmd spring-boot:run
```

Para executar os testes:

```powershell
.\mvnw.cmd test
```

## Perfis

- `ADMIN`: administra usuários e consulta pedidos e auditoria.
- `GERENTE`: gerencia pedidos, aplica descontos, cancela pedidos e consulta clientes e auditoria.
- `ATENDENTE`: cadastra e atualiza clientes, registra pedidos e acompanha o andamento.

As permissões são aplicadas no Spring Security, nos controllers e nos serviços. O cadastro de usuários não é público; somente `ADMIN` pode criar usuários ou atribuir perfis. A própria conta autenticada não pode trocar de perfil nem ser excluída. Senhas não são retornadas pelas APIs.

## Pedidos e auditoria

Pedidos são registrados no estado `RECEBIDO`. O fluxo permitido é `RECEBIDO` → `EM_PREPARACAO` → `PRONTO` → `SAIU_PARA_ENTREGA` ou `RETIRADO` → `FINALIZADO`. O servidor obtém os preços do catálogo e calcula subtotal, desconto e total. O gerente pode aplicar até 20% de desconto antes da saída ou cancelar antes da saída com justificativa.

Inclusões e alterações de pedidos e clientes são persistidas na coleção MongoDB `auditorias`, com usuário, perfil, horário, entidade, campos anteriores/novos e justificativa aplicável. A aplicação expõe somente consulta de auditoria para `ADMIN` e `GERENTE`; não há operações comuns para editar ou excluir esses registros.

## APIs principais

- `GET /api/pedidos`: lista pedidos para usuários autenticados.
- `POST /api/pedidos`: registra pedido com `clienteId` e `itens` (`produtoId`, `quantidade`); valores enviados pelo cliente são ignorados.
- `PUT /api/pedidos/{id}/status?status=...`: avança o pedido conforme o fluxo válido.
- `PUT /api/pedidos/{id}/desconto?percentual=10`: aplica desconto, exclusivo do gerente.
- `PUT /api/pedidos/{id}/cancelar?justificativa=...`: cancela, exclusivo do gerente.
- `GET /api/auditoria`: consulta registros, exclusivo de `ADMIN` e `GERENTE`.
- `GET /api/clientes`: consulta para gerente e atendente; `POST` e `PUT` são exclusivos do atendente.
- `/api/usuarios/**`: administração exclusiva de `ADMIN`.

CSRF permanece habilitado. Formulários Thymeleaf incluem o token automaticamente; clientes das APIs autenticadas por sessão devem enviar o token CSRF nos métodos que alteram dados.

## Demonstração

Para a entrega individual, grave na tela uma demonstração dos três perfis, das tentativas não autorizadas, do fluxo completo do pedido, do desconto/cancelamento, de uma alteração de cliente e dos registros correspondentes na tela de auditoria.