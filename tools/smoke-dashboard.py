"""Validação HTTP da aplicação no perfil demo; não altera pedidos.

Defina DASHBOARD_DEMO_PASSWORD e, opcionalmente, DASHBOARD_BASE_URL.
O resultado versionável nunca inclui cookies ou credenciais.
"""
import datetime as dt
from decimal import Decimal
from http.cookiejar import CookieJar
import json
import os
from pathlib import Path
import re
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
base = os.environ.get("DASHBOARD_BASE_URL", "http://localhost:8080").rstrip("/")
password = os.environ["DASHBOARD_DEMO_PASSWORD"]
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(CookieJar()))
login = opener.open(base + "/login", timeout=20).read().decode()
csrf = re.search(r'name="_csrf"[^>]*value="([^"]+)"', login).group(1)
opener.open(base + "/login", urllib.parse.urlencode({"username":"admin","password":password,"_csrf":csrf}).encode(), timeout=20).close()
checks = []

def get(path, expected=200, authenticated=True):
    try:
        response = (opener.open if authenticated else urllib.request.urlopen)(base + path, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    assert response.code == expected, (path, response.code, expected)
    raw = response.read().decode()
    result = json.loads(raw, parse_float=Decimal)
    checks.append({"rota":path,"http":response.code,"resultado":"passou"})
    return result, raw

options, _ = get("/api/dashboard/filtros")
today = dt.date.fromisoformat(options["hoje"])
query = urllib.parse.urlencode({"inicio":today-dt.timedelta(days=29),"fim":today})
summary, raw = get("/api/dashboard?"+query)
indicators = summary["indicadores"]
assert sum(s["quantidade"] for s in summary["situacoes"]) == indicators["pedidos"]
assert sum(s["faturamento"] for s in summary["vendas"]) == indicators["faturamento"]
assert len(summary["produtos"]) <= 5
weekly, _ = get("/api/dashboard?"+query+"&agrupamento=semana")
assert weekly["indicadores"] == indicators
assert sum(s["faturamento"] for s in weekly["vendas"]) == indicators["faturamento"]
canceled, _ = get("/api/dashboard?"+query+"&status=CANCELADO")
assert canceled["indicadores"]["faturamento"] == 0 and canceled["produtos"] == []
page, _ = get("/api/dashboard/pedidos?"+query+"&pagina=0&tamanho=8")
assert page["total"] == indicators["pedidos"] and len(page["itens"]) <= 8
assert all(set(p)=={"id","status","dataHoraEntrada","valorTotal","quantidadeItens"} for p in page["itens"])
get("/api/dashboard?inicio=31-10-2026",400)
get("/api/dashboard?status=INVALIDO",400)
get("/api/dashboard/pedidos?pagina=-1",400)
get("/api/dashboard",401,authenticated=False)
evidence = ROOT / "docs/dashboard/evidencias"
evidence.mkdir(parents=True,exist_ok=True)
(evidence / "api-resumo-demo.json").write_text(json.dumps(json.loads(raw),ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
(evidence / "api-smoke.json").write_text(json.dumps({"ambiente":"demo local com MongoDB real","verificacoes":checks},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print(f"{len(checks)} verificações HTTP aprovadas; nenhuma credencial exportada.")
