"""Exporta resultados reais do Surefire, sem propriedades de ambiente/credenciais."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
suites = []
for path in sorted((ROOT / "target/surefire-reports").glob("TEST-*.xml")):
    suite = ET.parse(path).getroot()
    suites.append({
        "classe": suite.attrib["name"],
        "testes": int(suite.attrib["tests"]),
        "falhas": int(suite.attrib["failures"]),
        "erros": int(suite.attrib["errors"]),
        "ignorados": int(suite.attrib["skipped"]),
        "segundos": float(suite.attrib["time"]),
        "casos": [{"nome": case.attrib["name"], "resultado": "ignorado" if case.find("skipped") is not None
                   else "falhou" if case.find("failure") is not None or case.find("error") is not None else "passou"}
                  for case in suite.findall("testcase")],
    })
if not suites:
    raise SystemExit("Execute mvnw verify antes de exportar as evidências.")
result = {"origem": "target/surefire-reports/TEST-*.xml", "suites": suites,
          "totais": {key: sum(s[key] for s in suites) for key in ("testes", "falhas", "erros", "ignorados")}}
output = ROOT / "docs/dashboard/evidencias/test-results.json"
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result["totais"], ensure_ascii=False))
