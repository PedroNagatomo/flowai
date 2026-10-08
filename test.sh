#!/bin/bash
set -e

BASE_URL=${BASE_URL:-http://localhost}
EMAIL=${EMAIL:-prod@flowai.dev}
PASSWORD=${PASSWORD:-senha123}
WEBHOOK_URL=${WEBHOOK_URL:?Defina WEBHOOK_URL antes de rodar}

echo "🔑 Login..."
TOKEN=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Falha no login"
  exit 1
fi
echo "✅ Token: ${TOKEN:0:30}..."

echo ""
echo "🧪 Teste 1: Workflow linear (compatibilidade)"
W1=$(curl -s -X POST $BASE_URL/api/workflows \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"[teste1] linear",
    "definition":{
      "trigger":{"type":"MANUAL","config":{}},
      "actions":[{"type":"HTTP_REQUEST","config":{
        "method":"POST","url":"'"$WEBHOOK_URL"'",
        "headers":{"Content-Type":"application/json"},
        "body":"{\"tipo\":\"linear\"}"
      }}]
    }
  }' | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

curl -s -X POST $BASE_URL/api/workflows/$W1/run -H "Authorization: Bearer $TOKEN" > /dev/null
sleep 3
echo "Workflow: $W1"
curl -s $BASE_URL/api/workflows/$W1/executions -H "Authorization: Bearer $TOKEN" | grep -o '"status":"[^"]*"' | head -1

echo ""
echo "🧪 Teste 2: AI_PROMPT"
W2=$(curl -s -X POST $BASE_URL/api/workflows \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"[teste2] ai",
    "definition":{
      "trigger":{"type":"MANUAL","config":{}},
      "actions":[
        {"type":"AI_PROMPT","id":"r","config":{"prompt":"Diga apenas: olá mundo"}},
        {"type":"HTTP_REQUEST","config":{
          "method":"POST","url":"'"$WEBHOOK_URL"'",
          "headers":{"Content-Type":"application/json"},
          "body":"{\"ia\":\"{{nodes.r.output}}\"}"
        }}
      ]
    }
  }' | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

curl -s -X POST $BASE_URL/api/workflows/$W2/run -H "Authorization: Bearer $TOKEN" > /dev/null
sleep 10
echo "Workflow: $W2"
curl -s $BASE_URL/api/workflows/$W2/executions -H "Authorization: Bearer $TOKEN" | grep -o '"status":"[^"]*"' | head -1

echo ""
echo "🧪 Teste 3: Grafo com IF"
W3=$(curl -s -X POST $BASE_URL/api/workflows \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"[teste3] graph",
    "definition":{
      "trigger":{"type":"MANUAL","config":{}},
      "graph":{"nodes":[
        {"id":"trigger","type":"TRIGGER","config":{},"next":["classify"]},
        {"id":"classify","type":"AI_PROMPT","config":{"prompt":"Classifique em POSITIVE ou NEGATIVE: Eu amei!"},"next":["check"]},
        {"id":"check","type":"IF","config":{"condition":{"field":"nodes.classify.output","operator":"CONTAINS","value":"POSITIVE"}},"next":{"then":["pos"],"else":["neg"]}},
        {"id":"pos","type":"HTTP_REQUEST","config":{"method":"POST","url":"'"$WEBHOOK_URL"'","body":"{\"branch\":\"pos\"}"}},
        {"id":"neg","type":"HTTP_REQUEST","config":{"method":"POST","url":"'"$WEBHOOK_URL"'","body":"{\"branch\":\"neg\"}"}}
      ]}
    }
  }' | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

curl -s -X POST $BASE_URL/api/workflows/$W3/run -H "Authorization: Bearer $TOKEN" > /dev/null
sleep 10
echo "Workflow: $W3"
curl -s $BASE_URL/api/workflows/$W3/executions -H "Authorization: Bearer $TOKEN" | python -m json.tool 2>/dev/null | head -40 || \
  curl -s $BASE_URL/api/workflows/$W3/executions -H "Authorization: Bearer $TOKEN"

echo ""
echo "🧪 Teste 4: IA gera grafo"
curl -s -X POST $BASE_URL/api/workflows/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Quando receber POST, classifique sentimento com IA. Se negativo, avise no Slack. Senão, mande email de feedback."}' \
  | python -m json.tool 2>/dev/null | head -60 || echo "(instale python ou veja raw)"

echo ""
echo "✅ Testes finalizados. Confira o webhook.site."
EOF